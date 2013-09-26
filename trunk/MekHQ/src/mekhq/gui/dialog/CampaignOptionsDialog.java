/*
 * CampaignOptionsDialog.java
 *
 * Created on August 19, 2009, 11:22 AM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.Era;
import mekhq.campaign.universe.Faction;
import mekhq.gui.model.DataTableModel;

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


    private javax.swing.JButton btnCamo;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDate;
    private javax.swing.JButton btnOkay;
    private JSpinner spnClanPriceModifier;
    private JSpinner spnUsedPartsValue[];
    private JSpinner spnDamagedPartsValue;
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
    private javax.swing.JPanel panMercenary;
    private javax.swing.JPanel panNameGen;
    private javax.swing.JPanel panXP;
    private javax.swing.JPanel panRank;
    private javax.swing.JPanel panSkill;
    private javax.swing.JPanel panRandomSkill;
    private javax.swing.JTabbedPane tabOptions;
    private javax.swing.JTextField txtName;
    private javax.swing.JCheckBox useEraModsCheckBox;
    private javax.swing.JCheckBox useDragoonRatingCheckBox;
    private javax.swing.JComboBox dragoonsRatingMethodCombo;
    private javax.swing.JCheckBox useFactionForNamesBox;
    private javax.swing.JCheckBox useTacticsBox;
    private javax.swing.JCheckBox useInitBonusBox;
    private javax.swing.JCheckBox useToughnessBox;
    private javax.swing.JCheckBox useArtilleryBox;
    private javax.swing.JCheckBox useAbilitiesBox;
    private javax.swing.JCheckBox useQuirksBox;
    private javax.swing.JCheckBox useEdgeBox;
    private javax.swing.JCheckBox useImplantsBox;
    private javax.swing.JCheckBox useAdvancedMedicalBox;
    private javax.swing.JCheckBox useDylansRandomXpBox;
    private javax.swing.JCheckBox payForPartsBox;
    private javax.swing.JCheckBox payForUnitsBox;
    private javax.swing.JCheckBox payForSalariesBox;
    private javax.swing.JCheckBox payForOverheadBox;
    private javax.swing.JCheckBox payForMaintainBox;
    private javax.swing.JCheckBox payForTransportBox;
    private javax.swing.JCheckBox payForRecruitmentBox;
    private javax.swing.JCheckBox useLoanLimitsBox;
    private javax.swing.JCheckBox sellUnitsBox;
    private javax.swing.JCheckBox sellPartsBox;

    private JTextField[] txtSalaryBase;
    private JSpinner[] spnSalaryXp;
    private JSpinner spnSalaryCommision;
    private JSpinner spnSalaryEnlisted;
    private JSpinner spnSalaryAntiMek;
    
    private JSpinner spnHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JCheckBox useRandomHitsForVees;
    private JSpinner spnMinimumHitsForVees;
    private JCheckBox useTougherHealing;
    
    // Start Personnel Market
    private JPanel panPersonnelMarket;
    private JComboBox personnelMarketType;
    private JTextField personnelMarketRandomEliteRemoval;
    private JTextField personnelMarketRandomVeteranRemoval;
    private JTextField personnelMarketRandomRegularRemoval;
    private JTextField personnelMarketRandomGreenRemoval;
    private JTextField personnelMarketRandomUltraGreenRemoval;
    private JCheckBox personnelMarketReportRefresh;
    private JSpinner personnelMarketDylansWeight;
    private JLabel personnelMarketTypeLabel;
    private JLabel personnelMarketRandomEliteRemovalLabel;
    private JLabel personnelMarketRandomVeteranRemovalLabel;
    private JLabel personnelMarketRandomRegularRemovalLabel;
    private JLabel personnelMarketRandomGreenRemovalLabel;
    private JLabel personnelMarketRandomUltraGreenRemovalLabel;
    private JLabel personnelMarketDylansWeightLabel;
    // End Personnel Market

    private javax.swing.JCheckBox useDamageMargin;
    private JSpinner spnDamageMargin;

    private javax.swing.JCheckBox checkMaintenance;
    private javax.swing.JCheckBox logMaintenance;
    private JSpinner spnMaintenanceDays;
    private JSpinner spnMaintenanceBonus;
    private javax.swing.JCheckBox useQualityMaintenance;


    private JRadioButton btnContractEquipment;
    private JRadioButton btnContractPersonnel;
    private JSpinner spnEquipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JCheckBox chkBLCSaleValue;
    private JSpinner spnOrderRefund;
    private javax.swing.JCheckBox usePercentageMaintBox;

    private javax.swing.JTable tableRanks;
    private RankTableModel ranksModel;
    private javax.swing.JScrollPane scrRanks;
    private JButton btnAddRank;
    private JButton btnDeleteRank;
    String[] rankColNames = {"Rank", "Officer", "Pay Multiplier"};

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
    private JLabel lblTaskXP;
    private JLabel lblTasks;
    private JSpinner spnNTasksXP;
    private JSpinner spnTaskXP;
    private JSpinner spnSuccessXP;
    private JLabel lblSuccessXp;
    private JSpinner spnMistakeXP;
    private JLabel lblMistakeXP;
    private JSpinner spnIdleXP;
    private JSpinner spnTargetIdleXP;
    private JSpinner spnMonthsIdleXP;

    private JCheckBox chkSupportStaffOnly;
    private JSpinner spnAcquireWaitingPeriod;
    private JComboBox choiceAcquireSkill;
    private JSpinner spnAcquireClanPenalty;
    private JSpinner spnAcquireIsPenalty;


    private JSpinner spnNDiceTransitTime;
    private JSpinner spnConstantTransitTime;
    private JComboBox choiceTransitTimeUnits;
    private JSpinner spnAcquireMosBonus;
    private JComboBox choiceAcquireMosUnits;
    private JSpinner spnAcquireMinimum;
    private JComboBox choiceAcquireMinimumUnit;

    private javax.swing.JCheckBox limitByYearBox;
    private javax.swing.JCheckBox allowClanPurchasesBox;
    private javax.swing.JCheckBox allowISPurchasesBox;
    private javax.swing.JCheckBox allowCanonOnlyBox;
    private javax.swing.JCheckBox useAmmoByTypeBox;
    private javax.swing.JCheckBox disallowSLUnitsBox;
    private javax.swing.JLabel lblTechLevel;
    private javax.swing.JComboBox choiceTechLevel;

    private JLabel lblOverallRecruitBonus;
    private JSpinner spnOverallRecruitBonus;
    private JSpinner[] spnTypeRecruitBonus;
    private JCheckBox chkExtraRandom;
    private JCheckBox chkClanBonus;
    private JLabel lblProbAntiMek;
    private JSpinner spnProbAntiMek;
    private JSpinner spnArtyProb;
    private JSpinner spnArtyBonus;
    private JSpinner spnSecondProb;
    private JSpinner spnSecondBonus;
    private JSpinner spnTacticsGreen;
    private JSpinner spnTacticsReg;
    private JSpinner spnTacticsVet;
    private JSpinner spnTacticsElite;
    private JSpinner spnCombatSA;
    private JSpinner spnSupportSA;
    private JSpinner spnAbilGreen;
    private JSpinner spnAbilReg;
    private JSpinner spnAbilVet;
    private JSpinner spnAbilElite;
    private JSpinner spnProbPhenoMW;
    private JSpinner spnProbPhenoAero;
    private JSpinner spnProbPhenoBA;
    private JSpinner spnProbPhenoVee;

    private javax.swing.JPanel panRandomPortrait;
    private JCheckBox[] chkUsePortrait;

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
        useEraModsCheckBox.setSelected(options.useEraMods());
        useDragoonRatingCheckBox.setSelected(options.useDragoonRating());
        dragoonsRatingMethodCombo.setSelectedItem(options.getDragoonsRatingMethod().getDescription());
        useFactionForNamesBox.setSelected(options.useFactionForNames());
        useTacticsBox.setSelected(options.useTactics());
        useInitBonusBox.setSelected(options.useInitBonus());
        useToughnessBox.setSelected(options.useToughness());
        useArtilleryBox.setSelected(options.useArtillery());
        useAbilitiesBox.setSelected(options.useAbilities());
        useEdgeBox.setSelected(options.useEdge());
        useImplantsBox.setSelected(options.useImplants());
        useAdvancedMedicalBox.setSelected(options.useAdvancedMedical());
        useDylansRandomXpBox.setSelected(options.useDylansRandomXp());
        payForPartsBox.setSelected(options.payForParts());
        payForUnitsBox.setSelected(options.payForUnits());
        payForSalariesBox.setSelected(options.payForSalaries());
        payForOverheadBox.setSelected(options.payForOverhead());
        payForMaintainBox.setSelected(options.payForMaintain());
        payForTransportBox.setSelected(options.payForTransport());
        payForRecruitmentBox.setSelected(options.payForRecruitment());
        useLoanLimitsBox.setSelected(options.useLoanLimits());
        usePercentageMaintBox.setSelected(options.usePercentageMaint());

        useDamageMargin.setSelected(options.isDestroyByMargin());
        useQualityMaintenance.setSelected(options.useQualityMaintenance());
        checkMaintenance.setSelected(options.checkMaintenance());

        sellUnitsBox.setSelected(options.canSellUnits());
        sellPartsBox.setSelected(options.canSellParts());

        limitByYearBox.setSelected(options.limitByYear());
        allowClanPurchasesBox.setSelected(options.allowClanPurchases());
        allowISPurchasesBox.setSelected(options.allowISPurchases());
        allowCanonOnlyBox.setSelected(options.allowCanonOnly());
        useAmmoByTypeBox.setSelected(options.useAmmoByType());

        useQuirksBox.setSelected(options.useQuirks());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());

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
        panMercenary = new javax.swing.JPanel();
        panNameGen = new javax.swing.JPanel();
        panRank = new javax.swing.JPanel();
        panXP = new javax.swing.JPanel();
        panSkill = new javax.swing.JPanel();
        panAbilityXP = new javax.swing.JPanel();
        panTech = new javax.swing.JPanel();
        panRandomSkill = new javax.swing.JPanel();
        panRandomPortrait = new javax.swing.JPanel();
        useEraModsCheckBox = new javax.swing.JCheckBox();
        useDragoonRatingCheckBox = new javax.swing.JCheckBox();
        dragoonsRatingMethodCombo = new javax.swing.JComboBox(CampaignOptions.DragoonsRatingMethod.getDragoonsRatingMethodNames());
        javax.swing.JLabel clanPriceModifierLabel = new javax.swing.JLabel();
        javax.swing.JLabel usedPartsValueLabel = new javax.swing.JLabel();
        javax.swing.JLabel damagedPartsValueLabel = new javax.swing.JLabel();
        DecimalFormat numberFormat = (DecimalFormat) DecimalFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setGroupingSeparator(' ');
        decimalFormatSymbols.setDecimalSeparator('.');
        numberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        useFactionForNamesBox = new javax.swing.JCheckBox();
        useTacticsBox = new javax.swing.JCheckBox();
        useInitBonusBox = new javax.swing.JCheckBox();
        useToughnessBox = new javax.swing.JCheckBox();
        useArtilleryBox = new javax.swing.JCheckBox();
        useAbilitiesBox = new javax.swing.JCheckBox();
        useEdgeBox = new javax.swing.JCheckBox();
        useImplantsBox = new javax.swing.JCheckBox();
        useAdvancedMedicalBox = new javax.swing.JCheckBox();
        useDylansRandomXpBox = new javax.swing.JCheckBox();
        payForPartsBox = new javax.swing.JCheckBox();
        payForUnitsBox = new javax.swing.JCheckBox();
        payForSalariesBox = new javax.swing.JCheckBox();
        payForRecruitmentBox = new javax.swing.JCheckBox();
        useLoanLimitsBox = new javax.swing.JCheckBox();
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
        useAmmoByTypeBox = new javax.swing.JCheckBox();
        choiceTechLevel = new javax.swing.JComboBox();
        btnOkay = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        scrRanks = new javax.swing.JScrollPane();

        useDamageMargin = new JCheckBox();
        useQualityMaintenance = new JCheckBox();
        checkMaintenance = new JCheckBox();
        logMaintenance = new JCheckBox();

        chkSupportStaffOnly = new JCheckBox();

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
            factionModel.addElement(Faction.getFaction(sname).getFullName(Era.E_JIHAD));
        }
        factionModel.setSelectedItem(campaign.getFaction().getFullName(Era.E_JIHAD));
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

        JPanel dragoonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));

        useDragoonRatingCheckBox.setText(resourceMap.getString("useDragoonRatingCheckBox.text")); // NOI18N
        useDragoonRatingCheckBox.setName("useDragoonRatingCheckBox"); // NOI18N
        dragoonsPanel.add(useDragoonRatingCheckBox);

        dragoonsPanel.add(Box.createHorizontalStrut(10));

        JLabel dragoonsMethodLabel = new JLabel("Dragoons Rating Method:");
        dragoonsMethodLabel.setName("dragoonsMethodLabel");
        dragoonsPanel.add(dragoonsMethodLabel);

        dragoonsRatingMethodCombo.setName("dragoonsRatingMethodCombo");
        dragoonsPanel.add(dragoonsRatingMethodCombo);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(dragoonsPanel, gridBagConstraints);

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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(btnCamo, gridBagConstraints);

        lblCamo.setText(resourceMap.getString("lblCamo.text")); // NOI18N
        lblCamo.setName("lblCamo"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblCamo, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panGeneral.TabConstraints.tabTitle"), panGeneral); // NOI18N

        panRepair.setName("panRules"); // NOI18N
        panRepair.setLayout(new java.awt.GridLayout(2,2));

        JPanel panSubRepair = new JPanel(new GridBagLayout());
        JPanel panSubMaintenance = new JPanel(new GridBagLayout());
        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());

        panSubRepair.setBorder(BorderFactory.createTitledBorder("Repair"));
        panSubMaintenance.setBorder(BorderFactory.createTitledBorder("Maintenance"));
        panSubAcquire.setBorder(BorderFactory.createTitledBorder("Acquisition"));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder("Delivery"));

        panRepair.add(panSubRepair);
        panRepair.add(panSubAcquire);
        panRepair.add(panSubMaintenance);
        panRepair.add(panSubDelivery);

        useEraModsCheckBox.setText(resourceMap.getString("useEraModsCheckBox.text")); // NOI18N
        useEraModsCheckBox.setToolTipText(resourceMap.getString("useEraModsCheckBox.toolTipText")); // NOI18N
        useEraModsCheckBox.setName("useEraModsCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useEraModsCheckBox, gridBagConstraints);

        useQuirksBox.setText(resourceMap.getString("useQuirksBox.text")); // NOI18N
        useQuirksBox.setToolTipText(resourceMap.getString("useQuirksBox.toolTipText")); // NOI18N
        useQuirksBox.setName("useQuirksBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useQuirksBox, gridBagConstraints);

        useDamageMargin.setText(resourceMap.getString("useDamageMargin.text")); // NOI18N
        useDamageMargin.setToolTipText(resourceMap.getString("useDamageMargin.toolTipText")); // NOI18N   
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useDamageMargin, gridBagConstraints);

        useDamageMargin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(useDamageMargin.isSelected()) {
                    spnDamageMargin.setEnabled(true);
                } else {
                    spnDamageMargin.setEnabled(false);
                }
            }
        });

        spnDamageMargin = new JSpinner(new SpinnerNumberModel(options.getDestroyMargin(), 1, 20, 1));
        ((JSpinner.DefaultEditor)spnDamageMargin.getEditor()).getTextField().setEditable(false);
        spnDamageMargin.setEnabled(options.isDestroyByMargin());

        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel("Margin:"));      
        pnlDamageMargin.add(spnDamageMargin);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDamageMargin, gridBagConstraints);

        checkMaintenance.setText(resourceMap.getString("checkMaintenance.text")); // NOI18N
        checkMaintenance.setToolTipText(resourceMap.getString("checkMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(checkMaintenance, gridBagConstraints);

        checkMaintenance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(checkMaintenance.isSelected()) {
                    spnMaintenanceDays.setEnabled(true);
                    useQualityMaintenance.setEnabled(true);
                    spnMaintenanceBonus.setEnabled(true);
                    logMaintenance.setEnabled(true);
                } else {
                    spnMaintenanceDays.setEnabled(false);
                    useQualityMaintenance.setEnabled(false);
                    spnMaintenanceBonus.setEnabled(false);
                    logMaintenance.setEnabled(false);
                }
            }
        });

        spnMaintenanceDays = new JSpinner(new SpinnerNumberModel(options.getMaintenanceCycleDays(), 1, 365, 1));
        ((JSpinner.DefaultEditor)spnMaintenanceDays.getEditor()).getTextField().setEditable(false);
        spnMaintenanceDays.setEnabled(options.checkMaintenance());

        JPanel pnlMaintenanceDays = new JPanel();
        pnlMaintenanceDays.add(spnMaintenanceDays);
        pnlMaintenanceDays.add(new JLabel("Maintenance cycle length in days"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceDays, gridBagConstraints);

        spnMaintenanceBonus = new JSpinner(new SpinnerNumberModel(options.getMaintenanceBonus(), -13, 13, 1));
        ((JSpinner.DefaultEditor)spnMaintenanceBonus.getEditor()).getTextField().setEditable(false);
        spnMaintenanceBonus.setEnabled(options.checkMaintenance());
        spnMaintenanceBonus.setToolTipText(resourceMap.getString("spnMaintenanceBonus.toolTipText")); // NOI18N   


        JPanel pnlMaintenanceBonus = new JPanel();
        pnlMaintenanceBonus.add(spnMaintenanceBonus);
        pnlMaintenanceBonus.add(new JLabel("Maintenance modifier"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceBonus, gridBagConstraints);

        useQualityMaintenance.setText(resourceMap.getString("useQualityMaintenance.text")); // NOI18N
        useQualityMaintenance.setToolTipText(resourceMap.getString("useQualityMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useQualityMaintenance, gridBagConstraints);

        logMaintenance.setText(resourceMap.getString("logMaintenance.text")); // NOI18N
        logMaintenance.setToolTipText(resourceMap.getString("logMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        spnAcquireWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getWaitingPeriod(), 1, 365, 1));
        ((JSpinner.DefaultEditor)spnAcquireWaitingPeriod.getEditor()).getTextField().setEditable(false);

        JPanel pnlWaitingPeriod = new JPanel();
        pnlWaitingPeriod.add(spnAcquireWaitingPeriod);
        pnlWaitingPeriod.add(new JLabel("Waiting period (in days) between acquisition rolls"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlWaitingPeriod, gridBagConstraints);

        DefaultComboBoxModel acquireSkillModel = new DefaultComboBoxModel();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        acquireSkillModel.setSelectedItem(options.getAcquisitionSkill());
        choiceAcquireSkill = new JComboBox(acquireSkillModel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panSubAcquire.add(new JLabel("Acquisition Skill:"), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panSubAcquire.add(choiceAcquireSkill, gridBagConstraints);      

        chkSupportStaffOnly.setText("Only support personnel can make acquisition checks"); // NOI18N
        //chkSupportStaffOnly.setToolTipText(resourceMap.getString("useQuirksBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(chkSupportStaffOnly, gridBagConstraints);

        spnAcquireClanPenalty = new JSpinner(new SpinnerNumberModel(options.getClanAcquisitionPenalty(), 0, 13, 1));
        ((JSpinner.DefaultEditor)spnAcquireClanPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlClanPenalty = new JPanel();
        pnlClanPenalty.add(spnAcquireClanPenalty);
        pnlClanPenalty.add(new JLabel("Penalty for Clan equipment"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlClanPenalty, gridBagConstraints);

        spnAcquireIsPenalty = new JSpinner(new SpinnerNumberModel(options.getIsAcquisitionPenalty(), 0, 13, 1));
        ((JSpinner.DefaultEditor)spnAcquireIsPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlIsPenalty = new JPanel();
        pnlIsPenalty.add(spnAcquireIsPenalty);
        pnlIsPenalty.add(new JLabel("Penalty for Inner Sphere equipment"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlIsPenalty, gridBagConstraints);


        spnNDiceTransitTime = new JSpinner(new SpinnerNumberModel(options.getNDiceTransitTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor)spnNDiceTransitTime.getEditor()).getTextField().setEditable(false);

        spnConstantTransitTime = new JSpinner(new SpinnerNumberModel(options.getConstantTransitTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor)spnConstantTransitTime.getEditor()).getTextField().setEditable(false);

        spnAcquireMosBonus = new JSpinner(new SpinnerNumberModel(options.getAcquireMosBonus(), 0, 365, 1));
        ((JSpinner.DefaultEditor)spnAcquireMosBonus.getEditor()).getTextField().setEditable(false);

        spnAcquireMinimum = new JSpinner(new SpinnerNumberModel(options.getAcquireMinimumTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor)spnAcquireMinimum.getEditor()).getTextField().setEditable(false);

        DefaultComboBoxModel transitUnitModel = new DefaultComboBoxModel();
        for(int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        choiceTransitTimeUnits = new JComboBox(transitUnitModel);  

        DefaultComboBoxModel transitMosUnitModel = new DefaultComboBoxModel();
        for(int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMosUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));
        choiceAcquireMosUnits = new JComboBox(transitMosUnitModel);  

        DefaultComboBoxModel transitMinUnitModel = new DefaultComboBoxModel();
        for(int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMinUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        choiceAcquireMinimumUnit = new JComboBox(transitMinUnitModel);  


        JPanel pnlTransitTime = new JPanel();
        pnlTransitTime.add(new JLabel("Delivery Time:"));      
        pnlTransitTime.add(spnNDiceTransitTime);
        pnlTransitTime.add(new JLabel("d6 + "));      
        pnlTransitTime.add(spnConstantTransitTime);
        pnlTransitTime.add(choiceTransitTimeUnits);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlTransitTime, gridBagConstraints);

        JPanel pnlMinTransit = new JPanel();
        pnlMinTransit.add(new JLabel("Minimum Transit Time:"));      
        pnlMinTransit.add(spnAcquireMinimum);
        pnlMinTransit.add(choiceAcquireMinimumUnit);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMinTransit, gridBagConstraints);

        JPanel pnlMosBonus = new JPanel();
        pnlMosBonus.add(new JLabel("Reduce delivery time by"));      
        pnlMosBonus.add(spnAcquireMosBonus);
        pnlMosBonus.add(choiceAcquireMosUnits);
        pnlMosBonus.add(new JLabel("per MoS"));      

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMosBonus, gridBagConstraints);

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

        lblTechLevel = new JLabel(resourceMap.getString("lblTechLevel.text")); // NOI18N
        lblTechLevel.setName("lblTechLevel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        DefaultComboBoxModel techLevelComboBoxModel = new DefaultComboBoxModel();
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));
        choiceTechLevel.setModel(techLevelComboBoxModel);
        //choiceTechLevel.setToolTipText(resourceMap.getString("choiceTechLevel.toolTipText")); // NOI18N
        choiceTechLevel.setName("choiceTechLevel"); // NOI18N
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(choiceTechLevel, gridBagConstraints);

        useAmmoByTypeBox.setText(resourceMap.getString("useAmmoByTypeBox.text")); // NOI18N
        useAmmoByTypeBox.setToolTipText(resourceMap.getString("useAmmoByTypeBox.toolTipText")); // NOI18N
        useAmmoByTypeBox.setName("useAmmoByTypeBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(useAmmoByTypeBox, gridBagConstraints);

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

        useAdvancedMedicalBox.setText(resourceMap.getString("useAdvancedMedicalBox.text")); // NOI18N
        useAdvancedMedicalBox.setToolTipText(resourceMap.getString("useAdvancedMedicalBox.toolTipText")); // NOI18N
        useAdvancedMedicalBox.setName("useAdvancedMedicalBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useAdvancedMedicalBox, gridBagConstraints);

        useDylansRandomXpBox.setText(resourceMap.getString("useDylansRandomXpBox.text")); // NOI18N
        useDylansRandomXpBox.setToolTipText(resourceMap.getString("useDylansRandomXpBox.toolTipText")); // NOI18N
        useDylansRandomXpBox.setName("useDylansRandomXpBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useDylansRandomXpBox, gridBagConstraints);

        spnHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getHealingWaitingPeriod(), 1, 30, 1));
        ((JSpinner.DefaultEditor)spnHealWaitingPeriod.getEditor()).getTextField().setEditable(false);      
        JPanel pnlHealWaitingPeriod = new JPanel();
        pnlHealWaitingPeriod.add(spnHealWaitingPeriod);
        pnlHealWaitingPeriod.add(new JLabel("Days to wait between healing checks by doctors"));      
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(pnlHealWaitingPeriod, gridBagConstraints);

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getNaturalHealingWaitingPeriod(), 1, 365, 1));
        ((JSpinner.DefaultEditor)spnNaturalHealWaitingPeriod.getEditor()).getTextField().setEditable(false);    
        JPanel pnlNaturalHealWaitingPeriod = new JPanel();
        pnlNaturalHealWaitingPeriod.add(spnNaturalHealWaitingPeriod);
        pnlNaturalHealWaitingPeriod.add(new JLabel("Days to wait for natural healing"));      
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(pnlNaturalHealWaitingPeriod, gridBagConstraints);

        spnMinimumHitsForVees = new JSpinner(new SpinnerNumberModel(options.getMinimumHitsForVees(), 1, 5, 1));
        ((JSpinner.DefaultEditor)spnMinimumHitsForVees.getEditor()).getTextField().setEditable(false);    
        JPanel panMinimumHitsForVees = new JPanel();
        panMinimumHitsForVees.add(spnMinimumHitsForVees);
        panMinimumHitsForVees.add(new JLabel("Minimum number of hits for wounded crews and infantry"));      
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(panMinimumHitsForVees, gridBagConstraints);

        useRandomHitsForVees = new JCheckBox();
        useRandomHitsForVees.setSelected(options.useRandomHitsForVees());
        useRandomHitsForVees.setText(resourceMap.getString("useRandomHitsForVees.text")); // NOI18N
        useRandomHitsForVees.setToolTipText(resourceMap.getString("useRandomHitsForVees.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useRandomHitsForVees, gridBagConstraints);

        useTougherHealing = new JCheckBox(resourceMap.getString("useTougherHealing.text"));
        useTougherHealing.setSelected(options.useTougherHealing());
        useTougherHealing.setToolTipText(resourceMap.getString("useTougherHealing.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useTougherHealing, gridBagConstraints);
        
        JPanel panSalary = new JPanel(new GridBagLayout());
        panSalary.setBorder(BorderFactory.createTitledBorder("Salary"));
        
        JPanel panMultiplier = new JPanel(new GridLayout(1,3));
        panMultiplier.setBorder(BorderFactory.createTitledBorder("Multipliers"));
        spnSalaryCommision = new JSpinner(new SpinnerNumberModel(options.getSalaryCommissionMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor)spnSalaryCommision.getEditor()).getTextField().setEditable(false);      
        JPanel panSalaryCommission = new JPanel();
        panSalaryCommission.add(spnSalaryCommision);
        panSalaryCommission.add(new JLabel("Commissioned")); 
        panMultiplier.add(panSalaryCommission);
        
        spnSalaryEnlisted = new JSpinner(new SpinnerNumberModel(options.getSalaryEnlistedMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor)spnSalaryEnlisted.getEditor()).getTextField().setEditable(false);      
        JPanel panSalaryEnlisted = new JPanel();
        panSalaryEnlisted.add(spnSalaryEnlisted);
        panSalaryEnlisted.add(new JLabel("Enlisted")); 
        panMultiplier.add(panSalaryEnlisted);
        
        spnSalaryAntiMek = new JSpinner(new SpinnerNumberModel(options.getSalaryAntiMekMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor)spnSalaryAntiMek.getEditor()).getTextField().setEditable(false);      
        JPanel panSalaryAntiMek = new JPanel();
        panSalaryAntiMek.add(spnSalaryAntiMek);
        panSalaryAntiMek.add(new JLabel("Anti-Mek")); 
        panMultiplier.add(panSalaryAntiMek);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSalary.add(panMultiplier, gridBagConstraints);
        
        JPanel panXpMultiplier = new JPanel(new GridLayout(2,3));
        panXpMultiplier.setBorder(BorderFactory.createTitledBorder("Experience Multipliers"));
        spnSalaryXp = new JSpinner[5];
        JSpinner spnXpSalary;
        JPanel panXpSalary;
        for(int i = 0; i < 5; i++) {
            spnXpSalary = new JSpinner(new SpinnerNumberModel(options.getSalaryXpMultiplier(i), 0, 10, 0.05));
            ((JSpinner.DefaultEditor)spnXpSalary.getEditor()).getTextField().setEditable(false);      
            panXpSalary = new JPanel();
            panXpSalary.add(spnXpSalary);
            panXpSalary.add(new JLabel(SkillType.getExperienceLevelName(i))); 
            panXpMultiplier.add(panXpSalary);
            spnSalaryXp[i] = spnXpSalary;          
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSalary.add(panXpMultiplier, gridBagConstraints);
        
        JPanel panAllTypes = new JPanel(new GridLayout(Person.T_NUM/2,2));
        JTextField txtType;
        JPanel panType;
        txtSalaryBase = new JTextField[Person.T_NUM];
        for(int i = 1; i < Person.T_NUM; i++) {
            txtType = new JTextField();
            txtType.setText(Integer.toString(options.getBaseSalary(i)));
            txtType.setPreferredSize(new Dimension(75,20));
            panType = new JPanel(new GridBagLayout());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panType.add(new JLabel(Person.getRoleDesc(i,false)), gridBagConstraints);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 0.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panType.add(txtType, gridBagConstraints);
            txtSalaryBase[i] = txtType;
            panAllTypes.add(panType);
        }
        JScrollPane scrSalaryBase = new JScrollPane(panAllTypes);
        scrSalaryBase.setBorder(BorderFactory.createTitledBorder("Base Salaries"));
        scrSalaryBase.setOpaque(false);
        scrSalaryBase.setPreferredSize(new Dimension(200,200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSalary.add(scrSalaryBase, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 14;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(panSalary, gridBagConstraints);
        

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

        payForRecruitmentBox.setText(resourceMap.getString("payForRecruitmentBox.text")); // NOI18N
        payForRecruitmentBox.setToolTipText(resourceMap.getString("payForRecruitmentBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForRecruitmentBox, gridBagConstraints);

        useLoanLimitsBox.setText(resourceMap.getString("useLoanLimitsBox.text")); // NOI18N
        useLoanLimitsBox.setToolTipText(resourceMap.getString("useLoanLimitsBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useLoanLimitsBox, gridBagConstraints);

        // Unofficial maintenance costs
        usePercentageMaintBox = new JCheckBox(resourceMap.getString("usePercentageMaintBox.text")); // NOI18N
        usePercentageMaintBox.setToolTipText(resourceMap.getString("usePercentageMaintBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePercentageMaintBox, gridBagConstraints);


        clanPriceModifierLabel.setText(resourceMap.getString("clanPriceModifierLabel.text")); // NOI18N
        clanPriceModifierLabel.setName("clanPriceModifierLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(clanPriceModifierLabel, gridBagConstraints);

        spnClanPriceModifier = new JSpinner(new SpinnerNumberModel(options.getClanPriceModifier(), 1.0, null, 0.1));
        ((JSpinner.DefaultEditor)spnClanPriceModifier.getEditor()).getTextField().setEditable(false);
        ((JSpinner.DefaultEditor)spnClanPriceModifier.getEditor()).getTextField().setColumns(3);
        spnClanPriceModifier.setToolTipText(resourceMap.getString("clanPriceModifierJFormattedTextField.toolTipText")); // NOI18N      
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(spnClanPriceModifier, gridBagConstraints);

        //JPanel panUsedParts = new JPanel(new GridBagLayout());

        usedPartsValueLabel.setText(resourceMap.getString("usedPartsValueLabel.text")); // NOI18N
        usedPartsValueLabel.setName("usedPartsValueLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(usedPartsValueLabel, gridBagConstraints);

        spnUsedPartsValue = new JSpinner[6];
        gridBagConstraints.gridwidth = 1;
        for(int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            gridBagConstraints.gridy++;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.insets = new Insets(0,20,0,0);
            panFinances.add(new JLabel(Part.getQualityName(i) + " Quality"), gridBagConstraints);
            gridBagConstraints.gridx = 2;
            gridBagConstraints.insets = new Insets(0,10,0,0);
            spnUsedPartsValue[i] = new JSpinner(new SpinnerNumberModel(options.getUsedPartsValue(i), 0.00, 1.00, 0.05));
            ((JSpinner.DefaultEditor)spnUsedPartsValue[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor)spnUsedPartsValue[i].getEditor()).getTextField().setColumns(3);
            spnUsedPartsValue[i].setToolTipText(resourceMap.getString("usedPartsValueJFormattedTextField.toolTipText")); // NOI18N
            panFinances.add(spnUsedPartsValue[i], gridBagConstraints);
        }

        damagedPartsValueLabel.setText(resourceMap.getString("damagedPartsValueLabel.text")); // NOI18N
        damagedPartsValueLabel.setName("damagedPartsValueLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(damagedPartsValueLabel, gridBagConstraints);

        spnDamagedPartsValue = new JSpinner(new SpinnerNumberModel(options.getDamagedPartsValue(), 0.00, 1.00, 0.05));
        ((JSpinner.DefaultEditor)spnDamagedPartsValue.getEditor()).getTextField().setEditable(false);
        ((JSpinner.DefaultEditor)spnDamagedPartsValue.getEditor()).getTextField().setColumns(3);
        spnDamagedPartsValue.setToolTipText(resourceMap.getString("damagedPartsValueJFormattedTextField.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(spnDamagedPartsValue, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(new JLabel("Reimbursement % (as decimal) for cancelled orders"), gridBagConstraints);

        spnOrderRefund = new JSpinner(new SpinnerNumberModel(options.GetCanceledOrderReimbursement(), 0.00, 1.00, 0.05));
        ((JSpinner.DefaultEditor)spnOrderRefund.getEditor()).getTextField().setEditable(false);
        ((JSpinner.DefaultEditor)spnOrderRefund.getEditor()).getTextField().setColumns(3);
        //spnDamagedPartsValue.setToolTipText(resourceMap.getString("damagedPartsValueJFormattedTextField.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(spnOrderRefund, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panFinances.TabConstraints.tabTitle"), panFinances); // NOI18N

        panMercenary.setName("panMercenary"); // NOI18N
        panMercenary.setLayout(new java.awt.GridBagLayout());

        btnContractEquipment = new JRadioButton("Base contract payment on percentage of TO&E unit value (StellarOps Beta)");
        btnContractPersonnel = new JRadioButton("Base contract payment on personnel payroll (FMMr)");

        if(options.useEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }

        spnEquipPercent = new JSpinner(new SpinnerNumberModel(options.getEquipmentContractPercent(), 0.1, 20, 0.1));
        ((JSpinner.DefaultEditor)spnEquipPercent.getEditor()).getTextField().setEditable(false);

        ButtonGroup groupContract = new ButtonGroup();
        groupContract.add(btnContractEquipment);
        groupContract.add(btnContractPersonnel);

        chkEquipContractSaleValue = new JCheckBox("Base on equipment sale value");
        chkEquipContractSaleValue.setSelected(options.useEquipmentContractSaleValue());
        chkBLCSaleValue = new JCheckBox("Base battle loss compensation on equipment sale value");
        chkBLCSaleValue.setSelected(options.useBLCSaleValue());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractEquipment, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5,30,5,5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Percent:"), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(spnEquipPercent, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(chkEquipContractSaleValue, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractPersonnel, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(chkBLCSaleValue, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panMercenary.TabConstraints.tabTitle"), panMercenary); // NOI18N

        panXP.setName("panXP"); // NOI18N
        panXP.setLayout(new java.awt.GridBagLayout());

        lblScenarioXP = new JLabel(resourceMap.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(options.getScenarioXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnScenarioXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnScenarioXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblScenarioXP, gridBagConstraints);      

        lblKillXP = new JLabel(resourceMap.getString("lblKillXP.text"));
        spnKillXP = new JSpinner(new SpinnerNumberModel(options.getKillXPAward(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnKillXP.getEditor()).getTextField().setEditable(false);

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
        ((JSpinner.DefaultEditor)spnKills.getEditor()).getTextField().setEditable(false);

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

        lblTaskXP = new JLabel(resourceMap.getString("lblKillXP.text"));
        spnTaskXP = new JSpinner(new SpinnerNumberModel(options.getTaskXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnTaskXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnTaskXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblTaskXP, gridBagConstraints);

        lblTasks = new JLabel(resourceMap.getString("lblTasks.text"));
        spnNTasksXP = new JSpinner(new SpinnerNumberModel(options.getNTasksXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnNTasksXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnNTasksXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblTasks, gridBagConstraints);

        lblSuccessXp = new JLabel(resourceMap.getString("lblSuccessXP.text"));
        spnSuccessXP = new JSpinner(new SpinnerNumberModel(options.getSuccessXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnSuccessXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnSuccessXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblSuccessXp, gridBagConstraints);  

        lblMistakeXP = new JLabel(resourceMap.getString("lblMistakeXP.text"));
        spnMistakeXP = new JSpinner(new SpinnerNumberModel(options.getMistakeXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnMistakeXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnMistakeXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblMistakeXP, gridBagConstraints);  

        spnIdleXP = new JSpinner(new SpinnerNumberModel(options.getIdleXP(), 0, 50, 1));
        ((JSpinner.DefaultEditor)spnIdleXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP for every"), gridBagConstraints);

        spnMonthsIdleXP = new JSpinner(new SpinnerNumberModel(options.getMonthsIdleXP(), 0, 36, 1));
        ((JSpinner.DefaultEditor)spnMonthsIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnMonthsIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("active month(s) on a 2d6 roll of greater than or equal to"), gridBagConstraints);

        spnTargetIdleXP = new JSpinner(new SpinnerNumberModel(options.getTargetIdleXP(), 2, 13, 1));
        ((JSpinner.DefaultEditor)spnTargetIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnTargetIdleXP, gridBagConstraints);

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
        txtInstructionsXP.setOpaque(false);
        txtInstructionsXP.setMinimumSize(new Dimension(550,120));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
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
        gridBagConstraints.gridy = 7;
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
        gridBagConstraints.gridy = 7;
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
            ((JSpinner.DefaultEditor)spnTarget.getEditor()).getTextField().setEditable(false);
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor)spnGreen.getEditor()).getTextField().setEditable(false);
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor)spnReg.getEditor()).getTextField().setEditable(false);
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor)spnVet.getEditor()).getTextField().setEditable(false);
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor)spnElite.getEditor()).getTextField().setEditable(false);
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

        JPanel panRollTable = new JPanel(new GridLayout(6,3,5,0));
        panRollTable.add(new JLabel("<html><b>Value</b></html>"));
        panRollTable.add(new JLabel("<html><b>Level</b></html>"));
        panRollTable.add(new JLabel("<html><b># Abils</b></html>"));
        panRollTable.add(new JLabel("less than 2"));
        JLabel lblUltraGreen = new JLabel("Ultra-Green/None");
        lblUltraGreen.setToolTipText(resourceMap.getString("lblUltraGreen.toolTipText"));
        panRollTable.add(lblUltraGreen);
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("2-5"));
        panRollTable.add(new JLabel("Green"));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("6-9"));
        panRollTable.add(new JLabel("Regular"));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("10-11"));
        panRollTable.add(new JLabel("Veteran"));
        panRollTable.add(new JLabel("1"));
        panRollTable.add(new JLabel("12 or more"));
        panRollTable.add(new JLabel("Elite"));
        panRollTable.add(new JLabel("2"));
        panRollTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2d6 + Bonus"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        lblOverallRecruitBonus = new JLabel(resourceMap.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resourceMap.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resourceMap.getString("chkExtraRandom.toolTipText"));
        chkExtraRandom.setSelected(rskillPrefs.randomizeSkill());
        chkClanBonus = new JCheckBox(resourceMap.getString("chkClanBonus.text"));
        chkClanBonus.setToolTipText(resourceMap.getString("chkClanBonus.toolTipText"));
        chkClanBonus.setSelected(rskillPrefs.useClanBonuses());
        lblProbAntiMek = new JLabel(resourceMap.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(rskillPrefs.getAntiMekProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor)spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getOverallRecruitBonus(), -12, 12, 1));
        ((JSpinner.DefaultEditor)spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus.setToolTipText(resourceMap.getString("spnOverallRecruitBonus.toolTipText"));
        spnTypeRecruitBonus = new JSpinner[Person.T_NUM];
        int nrow = (int)Math.ceil(Person.T_NUM / 4.0);
        JPanel panTypeRecruitBonus = new JPanel(new GridLayout(nrow,4));
        JSpinner spin;
        JPanel panRecruit;
        for(int i = 0; i < Person.T_NUM; i++) {
            panRecruit = new JPanel(new GridBagLayout());
            gridBagConstraints = new java.awt.GridBagConstraints();
            spin = new JSpinner(new SpinnerNumberModel(rskillPrefs.getRecruitBonus(i), -12, 12, 1));
            ((JSpinner.DefaultEditor)spin.getEditor()).getTextField().setEditable(false);
            spnTypeRecruitBonus[i] = spin;
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(2,5,0,0);
            panRecruit.add(spin, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            panRecruit.add(new JLabel(Person.getRoleDesc(i,false)), gridBagConstraints);
            panTypeRecruitBonus.add(panRecruit);
        }

        panTypeRecruitBonus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panTypeRecruitBonus.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(chkExtraRandom, gridBagConstraints);

        JPanel panClanPheno = new JPanel(new GridLayout(2,2));
        spnProbPhenoMW = new JSpinner(new SpinnerNumberModel(options.getProbPhenoMW(),0,100,5));
        ((JSpinner.DefaultEditor)spnProbPhenoMW.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoMW = new JPanel();
        panPhenoMW.add(spnProbPhenoMW);
        panPhenoMW.add(new JLabel("Mechwarrior"));
        panPhenoMW.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoMW);
        spnProbPhenoAero = new JSpinner(new SpinnerNumberModel(options.getProbPhenoAero(),0,100,5));
        ((JSpinner.DefaultEditor)spnProbPhenoAero.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoAero = new JPanel();
        panPhenoAero.add(spnProbPhenoAero);
        panPhenoAero.add(new JLabel("Aero Pilot"));
        panPhenoAero.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoAero);
        spnProbPhenoBA = new JSpinner(new SpinnerNumberModel(options.getProbPhenoBA(),0,100,5));
        ((JSpinner.DefaultEditor)spnProbPhenoBA.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoBA = new JPanel();
        panPhenoBA.add(spnProbPhenoBA);
        panPhenoBA.add(new JLabel("Elemental"));
        panPhenoBA.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoBA);        
        spnProbPhenoVee = new JSpinner(new SpinnerNumberModel(options.getProbPhenoVee(),0,100,5));
        ((JSpinner.DefaultEditor)spnProbPhenoVee.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoVee = new JPanel();
        panPhenoVee.add(spnProbPhenoVee);
        panPhenoVee.add(new JLabel("Vehicle"));
        panPhenoVee.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoVee);  
        
        panClanPheno.setBorder(BorderFactory.createTitledBorder("Trueborn Phenotype Probabilites"));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panClanPheno, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panRollTable, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(spnProbAntiMek, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(lblProbAntiMek, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(spnOverallRecruitBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(lblOverallRecruitBonus, gridBagConstraints);    

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panTypeRecruitBonus, gridBagConstraints);

        JPanel panArtillery = new JPanel();
        panArtillery.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Artillery Skill"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        spnArtyProb = new JSpinner(new SpinnerNumberModel(rskillPrefs.getArtilleryProb(),0,100,5));
        ((JSpinner.DefaultEditor)spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resourceMap.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getArtilleryBonus(),-10,10,1));
        ((JSpinner.DefaultEditor)spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSecondSkillProb(),0,100,5));
        ((JSpinner.DefaultEditor)spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resourceMap.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSecondSkillBonus(),-10,10,1));
        ((JSpinner.DefaultEditor)spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Skills"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        JPanel panTactics   = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_GREEN),-10,10,1));
        ((JSpinner.DefaultEditor)spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resourceMap.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_REGULAR),-10,10,1));
        ((JSpinner.DefaultEditor)spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resourceMap.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_VETERAN),-10,10,1));
        ((JSpinner.DefaultEditor)spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resourceMap.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_ELITE),-10,10,1));
        ((JSpinner.DefaultEditor)spnTacticsElite.getEditor()).getTextField().setEditable(false);
        spnTacticsElite.setToolTipText(resourceMap.getString("spnTacticsElite.toolTipText"));
        panTactics.add(spnTacticsGreen);
        panTactics.add(new JLabel("Green")); 
        panTactics.add(spnTacticsReg);
        panTactics.add(new JLabel("Reg")); 
        panTactics.add(spnTacticsVet);
        panTactics.add(new JLabel("Vet"));
        panTactics.add(spnTacticsElite);
        panTactics.add(new JLabel("Elite"));
        panTactics.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Tactics Skill"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        JPanel panSmallArms = new JPanel();
        spnCombatSA = new JSpinner(new SpinnerNumberModel(rskillPrefs.getCombatSmallArmsBonus(),-10,10,1));
        ((JSpinner.DefaultEditor)spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resourceMap.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSupportSmallArmsBonus(),-10,10,1));
        ((JSpinner.DefaultEditor)spnSupportSA.getEditor()).getTextField().setEditable(false);
        spnSupportSA.setToolTipText(resourceMap.getString("spnSupportSA.toolTipText"));
        panSmallArms.add(spnCombatSA);
        panSmallArms.add(new JLabel("Combat Personnel")); 
        panSmallArms.add(spnSupportSA);
        panSmallArms.add(new JLabel("Support Personnel")); 
        panSmallArms.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Small Arms Skill"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        JPanel panAbilities = new JPanel();
        spnAbilGreen = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_GREEN),-10,10,1));
        ((JSpinner.DefaultEditor)spnAbilGreen.getEditor()).getTextField().setEditable(false);
        spnAbilGreen.setToolTipText(resourceMap.getString("spnAbilGreen.toolTipText"));
        spnAbilReg = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_REGULAR),-10,10,1));
        ((JSpinner.DefaultEditor)spnAbilReg.getEditor()).getTextField().setEditable(false);
        spnAbilReg.setToolTipText(resourceMap.getString("spnAbilReg.toolTipText"));
        spnAbilVet = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_VETERAN),-10,10,1));
        ((JSpinner.DefaultEditor)spnAbilVet.getEditor()).getTextField().setEditable(false);
        spnAbilVet.setToolTipText(resourceMap.getString("spnAbilVet.toolTipText"));
        spnAbilElite = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_ELITE),-10,10,1));
        ((JSpinner.DefaultEditor)spnAbilElite.getEditor()).getTextField().setEditable(false);
        spnAbilElite.setToolTipText(resourceMap.getString("spnAbilElite.toolTipText"));
        panAbilities.add(spnAbilGreen);
        panAbilities.add(new JLabel("Green")); 
        panAbilities.add(spnAbilReg);
        panAbilities.add(new JLabel("Reg")); 
        panAbilities.add(spnAbilVet);
        panAbilities.add(new JLabel("Vet"));
        panAbilities.add(spnAbilElite);
        panAbilities.add(new JLabel("Elite"));
        panAbilities.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Special Abilities"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        JPanel panOtherBonuses = new JPanel(new GridLayout(3,2));
        panOtherBonuses.add(panArtillery);
        panOtherBonuses.add(panSecondary);
        panOtherBonuses.add(panTactics);
        panOtherBonuses.add(panAbilities);
        panOtherBonuses.add(panSmallArms);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panOtherBonuses, gridBagConstraints);

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
        comboRanks.setModel(rankModel);
        comboRanks.setSelectedIndex(campaign.getRanks().getRankSystem());
        comboRanks.setName("comboRanks"); // NOI18N
        comboRanks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillRankInfo();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(comboRanks, gridBagConstraints);

        btnAddRank = new JButton("Add Rank");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(btnAddRank, gridBagConstraints);
        btnAddRank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRank();
            }
        });


        btnDeleteRank = new JButton("Remove Rank");
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(btnDeleteRank, gridBagConstraints);
        btnDeleteRank.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeRank();
            }
        });
        btnDeleteRank.setEnabled(false);

        ranksModel = new RankTableModel(campaign.getRanks().getRanksArray(), rankColNames);
        tableRanks = new JTable(ranksModel);
        tableRanks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRanks.setRowSelectionAllowed(false);
        tableRanks.setColumnSelectionAllowed(false);
        tableRanks.setCellSelectionEnabled(true);
        tableRanks.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableRanks.setIntercellSpacing(new Dimension(0, 0));
        tableRanks.setShowGrid(false);
        TableColumnModel tcm = tableRanks.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(300);
        tcm.getColumn(1).setPreferredWidth(100);
        tcm.getColumn(2).setPreferredWidth(100);
        tcm.getColumn(2).setCellEditor(new SpinnerEditor());
        tableRanks.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        tableRanksValueChanged(evt);
                    }
                });
        scrRanks.setViewportView(tableRanks);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRank.add(scrRanks, gridBagConstraints);

        scrRanks.setMinimumSize(new Dimension(500, 500));
        scrRanks.setPreferredSize(new Dimension(500, 500));
        scrRanks.setMaximumSize(new Dimension(500, 500));
        
        JTextArea txtInstructionsRanks = new javax.swing.JTextArea();
        txtInstructionsRanks.setText(resourceMap.getString("txtInstructionsRanks.text"));
        txtInstructionsRanks.setEditable(false);
        txtInstructionsRanks.setLineWrap(true);
        txtInstructionsRanks.setWrapStyleWord(true);
        txtInstructionsRanks.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructionsRanks.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructionsRanks.setOpaque(false);
        txtInstructionsRanks.setMinimumSize(new Dimension(550,120));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRank.add(txtInstructionsRanks, gridBagConstraints);
        
        tabOptions.addTab(resourceMap.getString("panRank.TabConstraints.tabTitle"), panRank); // NOI18N

        panNameGen.setName("panNameGen"); // NOI18N
        panNameGen.setLayout(new java.awt.GridBagLayout());

        useFactionForNamesBox.setText(resourceMap.getString("useFactionForNamesBox.text")); // NOI18N
        useFactionForNamesBox.setToolTipText(resourceMap.getString("useFactionForNamesBox.toolTipText")); // NOI18N
        useFactionForNamesBox.setName("useFactionForNamesBox"); // NOI18N
        useFactionForNamesBox.setSelected(options.useFactionForNames());
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
        gridBagConstraints.insets = new Insets(10,0,0,0);
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
        gridBagConstraints.insets = new Insets(10,0,0,0);
        panNameGen.add(sldGender, gridBagConstraints);

        panRandomPortrait.setName("panRandomPortait"); // NOI18N
        panRandomPortrait.setLayout(new BorderLayout());

        JPanel panUsePortrait = new JPanel(new GridLayout(nrow, 4));

        chkUsePortrait = new JCheckBox[Person.T_NUM];
        JCheckBox box;
        for(int i = 0; i < Person.T_NUM; i++) {
            box = new JCheckBox(Person.getRoleDesc(i, false));
            box.setSelected(options.usePortraitForType(i));
            panUsePortrait.add(box);
            chkUsePortrait[i] = box;
        }

        panRandomPortrait.add(panUsePortrait, BorderLayout.CENTER);
        JTextArea txtPortraitInst = new JTextArea(resourceMap.getString("txtPortraitInst.text"));
        txtPortraitInst.setPreferredSize(new Dimension(728, 50));
        txtPortraitInst.setEditable(false);
        txtPortraitInst.setLineWrap(true);
        txtPortraitInst.setWrapStyleWord(true);
        txtPortraitInst.setOpaque(false);
        panRandomPortrait.add(txtPortraitInst, BorderLayout.PAGE_START);

        panRandomPortrait.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panRandomPortait.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(panRandomPortrait, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panNameGen.TabConstraints.tabTitle"), panNameGen); // NOI18N

        // Start Personnel Market
        panPersonnelMarket = new JPanel();
        personnelMarketType = new JComboBox();
        personnelMarketReportRefresh = new JCheckBox("Display a report when market refreshes");
        personnelMarketRandomEliteRemoval = new JTextField();
        personnelMarketRandomVeteranRemoval = new JTextField();
        personnelMarketRandomRegularRemoval = new JTextField();
        personnelMarketRandomGreenRemoval = new JTextField();
        personnelMarketRandomUltraGreenRemoval = new JTextField();
        personnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(options.getPersonnelMarketDylansWeight(), 0.1, 0.8, 0.1));
        personnelMarketTypeLabel = new JLabel("Market Type:");
        personnelMarketRandomEliteRemovalLabel = new JLabel("Random & Dylan's Elite Removal");
        personnelMarketRandomVeteranRemovalLabel = new JLabel("Random & Dylan's Veteran Removal");
        personnelMarketRandomRegularRemovalLabel = new JLabel("Random & Dylan's Regular Removal");
        personnelMarketRandomGreenRemovalLabel = new JLabel("Random & Dylan's Green Removal");
        personnelMarketRandomUltraGreenRemovalLabel = new JLabel("Random & Dylan's Ultra-Green Removal");
        personnelMarketDylansWeightLabel = new JLabel("<html>Weight for Dylan's Method to choose most"
                + "<br />common unit type based on your forces</html>");
        personnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        for (int i = PersonnelMarket.TYPE_RANDOM; i < PersonnelMarket.TYPE_NUM; i++) {
            personnelMarketType.addItem(PersonnelMarket.getTypeName(i));
        }
        personnelMarketType.setSelectedIndex(options.getPersonnelMarketType());
        personnelMarketRandomEliteRemoval.setText(Integer.toString(options.getPersonnelMarketRandomEliteRemoval()));
        personnelMarketRandomVeteranRemoval.setText(Integer.toString(options.getPersonnelMarketRandomVeteranRemoval()));
        personnelMarketRandomRegularRemoval.setText(Integer.toString(options.getPersonnelMarketRandomRegularRemoval()));
        personnelMarketRandomGreenRemoval.setText(Integer.toString(options.getPersonnelMarketRandomGreenRemoval()));
        personnelMarketRandomUltraGreenRemoval.setText(Integer.toString(options.getPersonnelMarketRandomUltraGreenRemoval()));
        personnelMarketRandomEliteRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
        personnelMarketRandomVeteranRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
        personnelMarketRandomRegularRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
        personnelMarketRandomGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
        personnelMarketRandomUltraGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);

        panPersonnelMarket.setName("panPersonnelMarket");
        panPersonnelMarket.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketTypeLabel, gridBagConstraints);

        personnelMarketType.setSelectedIndex(options.getPersonnelMarketType());
        personnelMarketType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                personnelMarketRandomEliteRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                        || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomVeteranRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                        || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomRegularRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                        || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                        || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomUltraGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_RANDOM
                        || personnelMarketType.getSelectedIndex() == PersonnelMarket.TYPE_DYLANS);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketType, gridBagConstraints);

        personnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketReportRefresh, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomEliteRemovalLabel, gridBagConstraints);

        personnelMarketRandomEliteRemoval.setText(Integer.toString(options.getPersonnelMarketRandomEliteRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomEliteRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomVeteranRemovalLabel, gridBagConstraints);

        personnelMarketRandomVeteranRemoval.setText(Integer.toString(options.getPersonnelMarketRandomVeteranRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomVeteranRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomRegularRemovalLabel, gridBagConstraints);

        personnelMarketRandomRegularRemoval.setText(Integer.toString(options.getPersonnelMarketRandomRegularRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomRegularRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomGreenRemovalLabel, gridBagConstraints);

        personnelMarketRandomGreenRemoval.setText(Integer.toString(options.getPersonnelMarketRandomGreenRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomGreenRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomUltraGreenRemovalLabel, gridBagConstraints);

        personnelMarketRandomUltraGreenRemoval.setText(Integer.toString(options.getPersonnelMarketRandomUltraGreenRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomUltraGreenRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketDylansWeightLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10,0,0,0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketDylansWeight, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panPersonnelMarket.TabConstraints.tabTitle"), panPersonnelMarket); // NOI18N
        // End Personnel Market

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
        Ranks ranks = new Ranks(comboRanks.getSelectedIndex());
        ranksModel.setDataVector(ranks.getRanksArray(), rankColNames);
    }

    private void tableRanksValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = tableRanks.getSelectedRow();
        btnDeleteRank.setEnabled(row != -1);
    }

    private void addRank() {
        Object[] rank = {"Unknown", false, 1.0};
        int row = tableRanks.getSelectedRow();
        if(row == -1) {
            if(ranksModel.getRowCount() > 0) {
                rank[1] = ranksModel.getValueAt(ranksModel.getRowCount()-1, 1);
            }
            ranksModel.addRow(rank);
            tableRanks.setRowSelectionInterval(tableRanks.getRowCount()-1, tableRanks.getRowCount()-1);
        } else {
            rank[1] = ranksModel.getValueAt(row, 1);
            ranksModel.insertRow(row+1, rank);
            tableRanks.setRowSelectionInterval(row+1, row+1);
        }
    }

    private void removeRank() {
        int row = tableRanks.getSelectedRow();
        if(row >-1) {
            ranksModel.removeRow(row);
        }
        if(tableRanks.getRowCount() > row) {
            tableRanks.setRowSelectionInterval(row, row);
        } else {
            tableRanks.setRowSelectionInterval(row-1, row-1);
        }
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
        campaign.getRanks().setRanksFromModel(ranksModel);
        campaign.setCamoCategory(camoCategory);
        campaign.setCamoFileName(camoFileName);
        campaign.setColorIndex(colorIndex);

        for(int i = 0; i < chkUsePortrait.length; i++) {
            options.setUsePortraitForType(i, chkUsePortrait[i].isSelected());
        }

        updateSkillTypes();
        updateXPCosts();

        // Rules panel
        options.setEraMods(useEraModsCheckBox.isSelected());
        options.setClanPriceModifier((Double)spnClanPriceModifier.getModel().getValue());
        for(int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            options.setUsedPartsValue((Double)spnUsedPartsValue[i].getModel().getValue(), i);
        }
        options.setDamagedPartsValue((Double)spnDamagedPartsValue.getModel().getValue());
        options.setCanceledOrderReimbursement((Double)spnOrderRefund.getModel().getValue());
        options.setDragoonRating(useDragoonRatingCheckBox.isSelected());
        options.setDragoonsRatingMethod(CampaignOptions.DragoonsRatingMethod.getDragoonsRatingMethod((String)dragoonsRatingMethodCombo.getSelectedItem()));
        options.setFactionForNames(useFactionForNamesBox.isSelected());
        options.setTactics(useTacticsBox.isSelected());
        options.setDestroyByMargin(useDamageMargin.isSelected());
        options.setDestroyMargin((Integer)spnDamageMargin.getModel().getValue());
        if(useTacticsBox.isSelected()) {
            campaign.getGameOptions().getOption("command_init").setValue(true);
        } else {
            campaign.getGameOptions().getOption("command_init").setValue(false);
        }
        options.setCheckMaintenance(checkMaintenance.isSelected());
        options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
        options.setMaintenanceBonus((Integer)spnMaintenanceBonus.getModel().getValue());
        options.setMaintenanceCycleDays((Integer)spnMaintenanceDays.getModel().getValue());	    
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
        options.setAdvancedMedical(useAdvancedMedicalBox.isSelected());
        options.setDylansRandomXp(useDylansRandomXpBox.isSelected());
        options.setPayForParts(payForPartsBox.isSelected());
        options.setPayForUnits(payForUnitsBox.isSelected());
        options.setPayForSalaries(payForSalariesBox.isSelected());
        options.setPayForOverhead(payForOverheadBox.isSelected());
        options.setPayForMaintain(payForMaintainBox.isSelected());
        options.setPayForTransport(payForTransportBox.isSelected());
        options.setPayForRecruitment(payForRecruitmentBox.isSelected());
        options.setLoanLimits(useLoanLimitsBox.isSelected());
        options.setUsePercentageMaint(usePercentageMaintBox.isSelected());
        options.setSellUnits(sellUnitsBox.isSelected());
        options.setSellParts(sellPartsBox.isSelected());

        options.setEquipmentContractBase(btnContractEquipment.isSelected());
        options.setEquipmentContractPercent((Double)spnEquipPercent.getModel().getValue());
        options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
        options.setBLCSaleValue(chkBLCSaleValue.isSelected());

        options.setQuirks(useQuirksBox.isSelected());
        campaign.getGameOptions().getOption("stratops_quirks").setValue(useQuirksBox.isSelected());

        options.setWaitingPeriod((Integer)spnAcquireWaitingPeriod.getModel().getValue());
        options.setAcquisitionSkill((String)choiceAcquireSkill.getSelectedItem());
        options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
        options.setClanAcquisitionPenalty((Integer)spnAcquireClanPenalty.getModel().getValue());
        options.setIsAcquisitionPenalty((Integer)spnAcquireIsPenalty.getModel().getValue());


        options.setNDiceTransitTime((Integer)spnNDiceTransitTime.getModel().getValue());
        options.setConstantTransitTime((Integer)spnConstantTransitTime.getModel().getValue());
        options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());
        options.setAcquireMosBonus((Integer)spnAcquireMosBonus.getModel().getValue());
        options.setAcquireMinimumTime((Integer)spnAcquireMinimum.getModel().getValue());
        options.setAcquireMinimumTimeUnit(choiceAcquireMinimumUnit.getSelectedIndex());
        options.setAcquireMosUnit(choiceAcquireMosUnits.getSelectedIndex());


        options.setScenarioXP((Integer)spnScenarioXP.getModel().getValue());
        options.setKillsForXP((Integer)spnKills.getModel().getValue());
        options.setKillXPAward((Integer)spnKillXP.getModel().getValue());

        options.setTaskXP((Integer)spnTaskXP.getModel().getValue());
        options.setNTasksXP((Integer)spnNTasksXP.getModel().getValue());
        options.setSuccessXP((Integer)spnSuccessXP.getModel().getValue());
        options.setMistakeXP((Integer)spnMistakeXP.getModel().getValue());
        options.setIdleXP((Integer)spnIdleXP.getModel().getValue());
        options.setMonthsIdleXP((Integer)spnMonthsIdleXP.getModel().getValue());
        options.setTargetIdleXP((Integer)spnTargetIdleXP.getModel().getValue());

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
        options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
        options.setTechLevel(choiceTechLevel.getSelectedIndex());
        if(choiceTechLevel.getSelectedIndex() > 0) {
            campaign.getGameOptions().getOption("allow_advanced_units").setValue(true);
            campaign.getGameOptions().getOption("allow_advanced_ammo").setValue(true);
        } 

        //we need to reset healing time options through the campaign because we may need to 
        //loop through personnel to make adjustments
        campaign.setHealingTimeOptions((Integer)spnHealWaitingPeriod.getModel().getValue(), (Integer)spnNaturalHealWaitingPeriod.getModel().getValue());

        options.setMinimumHitsForVees((Integer)spnMinimumHitsForVees.getModel().getValue());
        options.setUseRandomHitsForVees(useRandomHitsForVees.isSelected());
        options.setTougherHealing(useTougherHealing.isSelected());

        rskillPrefs.setOverallRecruitBonus((Integer)spnOverallRecruitBonus.getModel().getValue());	 
        for(int i = 0; i < Person.T_NUM; i++) {
            rskillPrefs.setRecruitBonus(i, (Integer)spnTypeRecruitBonus[i].getModel().getValue());	 
        }
        rskillPrefs.setRandomizeSkill(chkExtraRandom.isSelected());
        rskillPrefs.setAntiMekProb((Integer)spnProbAntiMek.getModel().getValue());
        rskillPrefs.setArtilleryProb((Integer)spnArtyProb.getModel().getValue());
        rskillPrefs.setArtilleryBonus((Integer)spnArtyBonus.getModel().getValue());
        rskillPrefs.setSecondSkillProb((Integer)spnSecondProb.getModel().getValue());
        rskillPrefs.setSecondSkillBonus((Integer)spnSecondBonus.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_GREEN, (Integer)spnTacticsGreen.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_REGULAR, (Integer)spnTacticsReg.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_VETERAN, (Integer)spnTacticsVet.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_ELITE, (Integer)spnTacticsElite.getModel().getValue());
        rskillPrefs.setCombatSmallArmsBonus((Integer)spnCombatSA.getModel().getValue());
        rskillPrefs.setSupportSmallArmsBonus((Integer)spnSupportSA.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_GREEN, (Integer)spnAbilGreen.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_REGULAR, (Integer)spnAbilReg.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_VETERAN, (Integer)spnAbilVet.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_ELITE, (Integer)spnAbilElite.getModel().getValue());

        options.setProbPhenoMW((Integer)spnProbPhenoMW.getModel().getValue());
        options.setProbPhenoAero((Integer)spnProbPhenoAero.getModel().getValue());
        options.setProbPhenoBA((Integer)spnProbPhenoBA.getModel().getValue());
        options.setProbPhenoVee((Integer)spnProbPhenoVee.getModel().getValue());
        
        //start salary
        for(int i = 1; i < Person.T_NUM; i++) {
            try {
                int salary = Integer.parseInt(txtSalaryBase[i].getText());
                options.setBaseSalary(salary, i);
            } catch(NumberFormatException ex) {
                //dont change it
            }
        }
        for(int i = 0; i < 5; i++) {
            options.setSalaryXpMultiplier((Double)spnSalaryXp[i].getModel().getValue(), i);
        }
        options.setSalaryCommissionMultiplier((Double)spnSalaryCommision.getModel().getValue());
        options.setSalaryEnlistedMultiplier((Double)spnSalaryEnlisted.getModel().getValue());
        options.setSalaryAntiMekMultiplier((Double)spnSalaryAntiMek.getModel().getValue());
        //end salary
        
        // Start Personnel Market
        options.setPersonnelMarketDylansWeight((Double)personnelMarketDylansWeight.getValue());
        options.setPersonnelMarketRandomEliteRemoval(Integer.parseInt(personnelMarketRandomEliteRemoval.getText()));
        options.setPersonnelMarketRandomVeteranRemoval(Integer.parseInt(personnelMarketRandomVeteranRemoval.getText()));
        options.setPersonnelMarketRandomRegularRemoval(Integer.parseInt(personnelMarketRandomRegularRemoval.getText()));
        options.setPersonnelMarketRandomGreenRemoval(Integer.parseInt(personnelMarketRandomGreenRemoval.getText()));
        options.setPersonnelMarketRandomUltraGreenRemoval(Integer.parseInt(personnelMarketRandomUltraGreenRemoval.getText()));
        options.setPersonnelMarketReportRefresh(personnelMarketReportRefresh.isSelected());
        options.setPersonnelMarketType(personnelMarketType.getSelectedIndex());
        // End Personnel Market
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

    public class RankTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 534443424190075264L;

        public final static int COL_NAME      =    0;
        public final static int COL_OFFICER       =   1;
        public final static int COL_PAYMULT  =   2;

        public RankTableModel(Object[][] ranksArray, String[] rankColNames) {
            super(ranksArray, rankColNames);
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }
    
    public static class SpinnerEditor extends DefaultCellEditor
    {
        JSpinner spinner;
        JSpinner.NumberEditor editor;
        JTextField textField;
        boolean valueSet;

        // Initializes the spinner.
        public SpinnerEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(1.0, 0, 10, 0.05));
            editor = ((JSpinner.NumberEditor)spinner.getEditor());
            textField = editor.getTextField();
            textField.addFocusListener( new FocusListener() {
                public void focusGained( FocusEvent fe ) {
                    System.err.println("Got focus");
                    //textField.setSelectionStart(0);
                    //textField.setSelectionEnd(1);
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( valueSet ) {
                                textField.setCaretPosition(1);
                            }
                        }
                    });
                }
                public void focusLost( FocusEvent fe ) {
                }
            });
            textField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent ae ) {
                    stopCellEditing();
                }
            });
        }

        // Prepares the spinner component and returns it.
        public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column
        ) {
            if ( !valueSet ) {
                spinner.setValue(value);
            }
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    textField.requestFocus();
                }
            });
            return spinner;
        }

        public boolean isCellEditable( EventObject eo ) {
            System.err.println("isCellEditable");
            if ( eo instanceof KeyEvent ) {
                KeyEvent ke = (KeyEvent)eo;
                System.err.println("key event: "+ke.getKeyChar());
                textField.setText(String.valueOf(ke.getKeyChar()));
                //textField.select(1,1);
                //textField.setCaretPosition(1);
                //textField.moveCaretPosition(1);
                valueSet = true;
            } else {
                valueSet = false;
            }
            return true;
        }

        // Returns the spinners current value.
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        public boolean stopCellEditing() {
            System.err.println("Stopping edit");
            try {
                editor.commitEdit();
                spinner.commitEdit();
            } catch ( java.text.ParseException e ) {
                JOptionPane.showMessageDialog(null,
                    "Invalid value, discarding.");
            }
            return super.stopCellEditing();
        }
    }

}
