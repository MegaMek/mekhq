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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;
import megamek.common.logging.LogLevel;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.util.DirectoryItems;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.GamePreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.model.RankTableModel;
import mekhq.gui.model.SortedComboBoxModel;
import mekhq.gui.utilities.TableCellListener;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
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
    private Hashtable<String, JSpinner> hashSkillTargets;
    private Hashtable<String, JSpinner> hashGreenSkill;
    private Hashtable<String, JSpinner> hashRegSkill;
    private Hashtable<String, JSpinner> hashVetSkill;
    private Hashtable<String, JSpinner> hashEliteSkill;
    private boolean cancelled;

    private JButton btnCamo;
    private JButton btnCancel;
    private JButton btnDate;
    private JButton btnOkay;
    private JButton btnSave;
    private JButton btnLoad;
    private JSpinner spnClanPriceModifier;
    private JSpinner spnUsedPartsValue[];
    private JSpinner spnDamagedPartsValue;
    private JComboBox<String> comboFaction;
    SortedComboBoxModel<String> factionModel;
    private JComboBox<String> comboFactionNames;
    private JComboBox<String> comboRanks;
    private JSlider sldGender;
    private JLabel lblCamo;
    private JLabel lblDate;
    private JLabel lblFaction;
    private JLabel lblFactionNames;
    private JLabel lblRank;
    private JLabel lblGender;
    private JLabel lblName;
    private JPanel panGeneral;
    private JPanel panRepair;
    private JPanel panTech;
    private JPanel panPersonnel;
    private JPanel panFinances;
    private JPanel panMercenary;
    private JPanel panNameGen;
    private JPanel panXP;
    private JPanel panRank;
    private JPanel panSkill;
    private JPanel panRandomSkill;
    private JTabbedPane tabOptions;
    private JTextField txtName;
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
	private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useUnitRatingCheckBox;
    private JComboBox<String> unitRatingMethodCombo;
    private JCheckBox useFactionForNamesBox;
    private JCheckBox useTacticsBox;
    private JCheckBox useInitBonusBox;
    private JCheckBox useToughnessBox;
    private JCheckBox useArtilleryBox;
    private JCheckBox useAbilitiesBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useEdgeBox;
    private JCheckBox useSupportEdgeBox;
    private JCheckBox useImplantsBox;
	private JCheckBox altQualityAveragingCheckBox;
    private JCheckBox useAdvancedMedicalBox;
    private JCheckBox useDylansRandomXpBox;
    private JCheckBox payForPartsBox;
    private JCheckBox payForUnitsBox;
    private JCheckBox payForSalariesBox;
    private JCheckBox payForOverheadBox;
    private JCheckBox payForMaintainBox;
    private JCheckBox payForTransportBox;
    private JCheckBox payForRecruitmentBox;
    private JCheckBox useLoanLimitsBox;
    private JCheckBox sellUnitsBox;
    private JCheckBox sellPartsBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox showPeacetimeCostBox;
    private JCheckBox chkAssignPortraitOnRoleChange;

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
    private JComboBox<String> comboPrisonerStatus;


    // Start Personnel Market
    private JPanel panPersonnelMarket;
    private JComboBox<String> personnelMarketType;
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

    private JCheckBox useDamageMargin;
    private JSpinner spnDamageMargin;
    private JSpinner spnDestroyPartTarget;


    private JCheckBox checkMaintenance;
    private JCheckBox logMaintenance;
    private JSpinner spnMaintenanceDays;
    private JSpinner spnMaintenanceBonus;
    private JCheckBox useQualityMaintenance;
    private JCheckBox useUnofficalMaintenance;
    private JCheckBox reverseQualityNames;

    private JRadioButton btnContractEquipment;
    private JRadioButton btnContractPersonnel;
    private JSpinner spnEquipPercent;
    private JSpinner spnDropshipPercent;
    private JSpinner spnJumpshipPercent;
    private JSpinner spnWarshipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JCheckBox chkBLCSaleValue;
    private JSpinner spnOrderRefund;
    private JCheckBox usePercentageMaintBox;
    private JCheckBox useInfantryDontCountBox;

    private JTable tableRanks;
    private RankTableModel ranksModel;
    private JScrollPane scrRanks;
    @SuppressWarnings("unused")
	private JButton btnAddRank; // FIXME: Unused?
    @SuppressWarnings("unused")
	private JButton btnDeleteRank; // FIXME: Unused?
    String[] rankColNames = { "Rate", "MW Rank", "ASF Rank", "Vee Crew Rank", "Naval Rank", "Infantry Rank", "Tech Rank", "Officer", "Pay Multiplier"};

    private JTextArea txtInstructionsXP;
    private JScrollPane scrXP;
    private JTable tableXP;
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
    private JSpinner spnContractNegotiationXP;
    private JSpinner spnAdminWeeklyXP;
    private JSpinner spnAdminWeeklyXPPeriod;
    private JSpinner spnEdgeCost;

    private JCheckBox chkSupportStaffOnly;
    private JSpinner spnAcquireWaitingPeriod;
    private JComboBox<String> choiceAcquireSkill;
    private JSpinner spnAcquireClanPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JCheckBox chkCapturePrisoners;
    private JTextField txtMaxAcquisitions;
    private JCheckBox chkUseUnofficialProcreation;
    private JCheckBox chkUseUnofficialProcreationNoRelationship;
    private JCheckBox chkUseParentage;
    private JCheckBox chkLogConception;
    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseTimeInService;


    private JSpinner spnNDiceTransitTime;
    private JSpinner spnConstantTransitTime;
    private JComboBox<String> choiceTransitTimeUnits;
    private JSpinner spnAcquireMosBonus;
    private JComboBox<String> choiceAcquireMosUnits;
    private JSpinner spnAcquireMinimum;
    private JComboBox<String> choiceAcquireMinimumUnit;

    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JCheckBox variableTechLevelBox;
    private JCheckBox factionIntroDateBox;
    private JCheckBox useAmmoByTypeBox;
    //private JCheckBox disallowSLUnitsBox;
    private JLabel lblTechLevel;
    private JComboBox<String> choiceTechLevel;

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

    private JPanel panRandomPortrait;
    private JCheckBox[] chkUsePortrait;

    private JPanel panSpecialAbilities;

    Hashtable<String, SpecialAbility> tempSPA;
    private JButton btnAddSPA;

    /* Against the Bot */
    private JPanel panAtB;
    private JCheckBox chkUseAtB;

    private JComboBox<String> cbSkillLevel;
    private JCheckBox chkUseShareSystem;
    private JCheckBox chkSharesExcludeLargeCraft;
    private JCheckBox chkSharesForAll;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkRetirementRolls;
    private JCheckBox chkTrackUnitFatigue;
    private JCheckBox chkCustomRetirementMods;
    private JCheckBox chkFoundersNeverRetire;
    private JCheckBox chkLimitLanceWeight;
    private JCheckBox chkLimitLanceNumUnits;
    private JCheckBox chkUseLeadership;
    private JCheckBox chkUseStrategy;
    private JSpinner spnBaseStrategyDeployment;
    private JSpinner spnAdditionalStrategyDeployment;
    private JCheckBox chkAdjustPaymentForStrategy;

    private JCheckBox chkUseAero;
    private JCheckBox chkUseVehicles;
    private JCheckBox chkClanVehicles;
    private JCheckBox chkDoubleVehicles;
    private JCheckBox chkAdjustPlayerVehicles;
    private JSpinner spnOpforLanceTypeMechs;
    private JSpinner spnOpforLanceTypeMixed;
    private JSpinner spnOpforLanceTypeVehicles;
    private JCheckBox chkOpforUsesVTOLs;
    private JCheckBox chkOpforUsesAero;
    private JCheckBox chkOpforUsesLocalForces;
    private JCheckBox chkUseDropShips;
    private JLabel lblOpforAeroChance;
    private JLabel lblOpforLocalForceChance;
    private JSpinner spnOpforAeroChance;
    private JSpinner spnOpforLocalForceChance;

    private JRadioButton btnDynamicRATs;
    private JRadioButton btnStaticRATs;
    private JCheckBox chkIgnoreRatEra;
    private DefaultListModel<String> chosenRatModel;
    private DefaultListModel<String> availableRatModel;
    private JList<String> availableRats;
    private JList<String> chosenRats;
    private JButton btnAddRat;
    private JButton btnRemoveRat;
    private JButton btnMoveRatUp;
    private JButton btnMoveRatDown;

    private JSpinner spnSearchRadius;
    private JSpinner spnIntensity;
    private JLabel lblFightPct;
    private JLabel lblDefendPct;
    private JLabel lblScoutPct;
    private JLabel lblTrainingPct;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkRestrictPartsByMission;
    private JCheckBox chkRegionalMechVariations;
    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    private JCheckBox chkUseAtBCapture;

    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkContractMarketReportRefresh;
    private JCheckBox chkUnitMarketReportRefresh;

    private JSpinner spnStartGameDelay;

    /**
     * Creates new form CampaignOptionsDialog
     */
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
        assignedTechFirstCheckBox.setSelected(options.useAssignedTechFirst());
		resetToFirstTechCheckBox.setSelected(options.useResetToFirstTech());
        useUnitRatingCheckBox.setSelected(options.useDragoonRating());
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod().getDescription());
        useFactionForNamesBox.setSelected(options.useFactionForNames());
        useTacticsBox.setSelected(options.useTactics());
        useInitBonusBox.setSelected(options.useInitBonus());
        useToughnessBox.setSelected(options.useToughness());
        useArtilleryBox.setSelected(options.useArtillery());
        useAbilitiesBox.setSelected(options.useAbilities());
        useEdgeBox.setSelected(options.useEdge());
        useSupportEdgeBox.setSelected(options.useSupportEdge());
        useImplantsBox.setSelected(options.useImplants());
        chkCapturePrisoners.setSelected(options.capturePrisoners());
		altQualityAveragingCheckBox.setSelected(options.useAltQualityAveraging());
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
        useInfantryDontCountBox.setSelected(options.useInfantryDontCount());
        usePeacetimeCostBox.setSelected(options.usePeacetimeCost());
        useExtendedPartsModifierBox.setSelected(options.useExtendedPartsModifier());
        showPeacetimeCostBox.setSelected(options.showPeacetimeCost());
        chkAssignPortraitOnRoleChange.setSelected(options.getAssignPortraitOnRoleChange());

        useDamageMargin.setSelected(options.isDestroyByMargin());
        useAeroSystemHitsBox.setSelected(options.useAeroSystemHits());
        useQualityMaintenance.setSelected(options.useQualityMaintenance());
        useUnofficalMaintenance.setSelected(options.useUnofficalMaintenance());
        checkMaintenance.setSelected(options.checkMaintenance());
        reverseQualityNames.setSelected(options.reverseQualityNames());

        sellUnitsBox.setSelected(options.canSellUnits());
        sellPartsBox.setSelected(options.canSellParts());

        limitByYearBox.setSelected(options.limitByYear());
        disallowExtinctStuffBox.setSelected(options.disallowExtinctStuff());
        allowClanPurchasesBox.setSelected(options.allowClanPurchases());
        allowISPurchasesBox.setSelected(options.allowISPurchases());
        allowCanonOnlyBox.setSelected(options.allowCanonOnly());
        allowCanonRefitOnlyBox.setSelected(options.allowCanonRefitOnly());
        variableTechLevelBox.setSelected(options.useVariableTechLevel()
                && options.limitByYear());
        variableTechLevelBox.setEnabled(options.limitByYear());
        factionIntroDateBox.setSelected(options.useFactionIntroDate());
        useAmmoByTypeBox.setSelected(options.useAmmoByType());

        useQuirksBox.setSelected(options.useQuirks());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabOptions = new JTabbedPane();
        panGeneral = new JPanel();
        txtName = new JTextField();
        lblName = new JLabel();
        lblFaction = new JLabel();
        lblFactionNames = new JLabel();
        lblRank = new JLabel();
        lblGender = new JLabel();
        lblDate = new JLabel();
        btnDate = new JButton();
        comboFaction = new JComboBox<String>();
        comboFactionNames = new JComboBox<String>();
        comboRanks = new JComboBox<String>();
        sldGender = new JSlider(SwingConstants.HORIZONTAL);
        btnCamo = new JButton();
        lblCamo = new JLabel();
        panRepair = new JPanel();
        panPersonnel = new JPanel();
        panFinances = new JPanel();
        panMercenary = new JPanel();
        panNameGen = new JPanel();
        panRank = new JPanel();
        panXP = new JPanel();
        panSkill = new JPanel();
        panTech = new JPanel();
        panRandomSkill = new JPanel();
        panRandomPortrait = new JPanel();
        useEraModsCheckBox = new JCheckBox();
        assignedTechFirstCheckBox = new JCheckBox();
		resetToFirstTechCheckBox = new JCheckBox();
        useUnitRatingCheckBox = new JCheckBox();
        unitRatingMethodCombo = new JComboBox<String>(UnitRatingMethod.getUnitRatingMethodNames());
        javax.swing.JLabel clanPriceModifierLabel = new JLabel();
        javax.swing.JLabel usedPartsValueLabel = new JLabel();
        javax.swing.JLabel damagedPartsValueLabel = new JLabel();
        DecimalFormat numberFormat = (DecimalFormat) DecimalFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setGroupingSeparator(' ');
        decimalFormatSymbols.setDecimalSeparator('.');
        numberFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        useFactionForNamesBox = new JCheckBox();
        useTacticsBox = new JCheckBox();
        useInitBonusBox = new JCheckBox();
        useToughnessBox = new JCheckBox();
        useArtilleryBox = new JCheckBox();
        useAbilitiesBox = new JCheckBox();
        useEdgeBox = new JCheckBox();
        useSupportEdgeBox = new JCheckBox();
        useImplantsBox = new JCheckBox();
        chkCapturePrisoners = new JCheckBox();
		altQualityAveragingCheckBox = new JCheckBox();
        useAdvancedMedicalBox = new JCheckBox();
        useDylansRandomXpBox = new JCheckBox();
        payForPartsBox = new JCheckBox();
        payForUnitsBox = new JCheckBox();
        payForSalariesBox = new JCheckBox();
        payForRecruitmentBox = new JCheckBox();
        useLoanLimitsBox = new JCheckBox();
        payForOverheadBox = new JCheckBox();
        payForMaintainBox = new JCheckBox();
        payForTransportBox = new JCheckBox();
        usePeacetimeCostBox = new JCheckBox();
        useExtendedPartsModifierBox = new JCheckBox();
        showPeacetimeCostBox = new JCheckBox();
        chkAssignPortraitOnRoleChange = new JCheckBox();
        sellUnitsBox = new JCheckBox();
        sellPartsBox = new JCheckBox();
        useQuirksBox = new JCheckBox();
        limitByYearBox = new JCheckBox();
        disallowExtinctStuffBox = new JCheckBox();
        allowClanPurchasesBox = new JCheckBox();
        allowISPurchasesBox = new JCheckBox();
        allowCanonOnlyBox = new JCheckBox();
        allowCanonRefitOnlyBox = new JCheckBox();
        variableTechLevelBox = new JCheckBox();
        factionIntroDateBox = new JCheckBox();
        useAmmoByTypeBox = new JCheckBox();
        choiceTechLevel = new JComboBox<String>();
        btnOkay = new JButton();
        btnSave = new JButton();
        btnLoad = new JButton();
        btnCancel = new JButton();
        scrRanks = new JScrollPane();

        useDamageMargin = new JCheckBox();
        useAeroSystemHitsBox = new JCheckBox();
        useQualityMaintenance = new JCheckBox();
        useUnofficalMaintenance = new JCheckBox();
        checkMaintenance = new JCheckBox();
        logMaintenance = new JCheckBox();
        reverseQualityNames = new JCheckBox();
                
        chkSupportStaffOnly = new JCheckBox();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl()); //$NON-NLS-1$
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
            @Override
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
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDateActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(btnDate, gridBagConstraints);

        factionModel = new SortedComboBoxModel<String>();
        for (String sname : Faction.choosableFactionCodes) {
            Faction f = Faction.getFaction(sname);
            if (f.validIn(date.get(Calendar.YEAR))) {
                factionModel.addElement(f.getFullName(date.get(Calendar.YEAR)));
            }
        }
        factionModel.setSelectedItem(campaign.getFaction().getFullName(date.get(Calendar.YEAR)));
        comboFaction.setModel(factionModel);
        comboFaction.setMinimumSize(new java.awt.Dimension(400, 30));
        comboFaction.setName("comboFaction"); // NOI18N
        comboFaction.setPreferredSize(new java.awt.Dimension(400, 30));
        comboFaction.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                factionSelected();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(comboFaction, gridBagConstraints);

        JPanel unitRatingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));

        useUnitRatingCheckBox.setText(resourceMap.getString("useUnitRatingCheckBox.text")); // NOI18N
        useUnitRatingCheckBox.setName("useUnitRatingCheckBox"); // NOI18N
        unitRatingPanel.add(useUnitRatingCheckBox);

        unitRatingPanel.add(Box.createHorizontalStrut(10));

        JLabel unitRatingMethodLabel = new JLabel("Unit Rating Method:");
        unitRatingMethodLabel.setName("unitRatingMethodLabel");
        unitRatingPanel.add(unitRatingMethodLabel);

        unitRatingMethodCombo.setName("unitRatingMethodCombo");
        unitRatingPanel.add(unitRatingMethodCombo);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(unitRatingPanel, gridBagConstraints);

        btnCamo.setMaximumSize(new java.awt.Dimension(84, 72));
        btnCamo.setMinimumSize(new java.awt.Dimension(84, 72));
        btnCamo.setName("btnCamo"); // NOI18N
        btnCamo.setPreferredSize(new java.awt.Dimension(84, 72));
        btnCamo.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
        panRepair.setLayout(new java.awt.GridBagLayout());

        JPanel panSubRepair = new JPanel(new GridBagLayout());
        JPanel panSubMaintenance = new JPanel(new GridBagLayout());
        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());

        panSubRepair.setBorder(BorderFactory.createTitledBorder("Repair"));
        panSubMaintenance.setBorder(BorderFactory.createTitledBorder("Maintenance"));
        panSubAcquire.setBorder(BorderFactory.createTitledBorder("Acquisition"));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder("Delivery"));
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panRepair.add(panSubRepair, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panRepair.add(panSubAcquire, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panRepair.add(panSubMaintenance, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panRepair.add(panSubDelivery, gridBagConstraints);
        
        //We want the new mass repair panel to span two cells
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;

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

        assignedTechFirstCheckBox.setText(resourceMap.getString("assignedTechFirstCheckBox.text")); // NOI18N
        assignedTechFirstCheckBox.setToolTipText(resourceMap.getString("assignedTechFirstCheckBox.toolTipText")); // NOI18N
        assignedTechFirstCheckBox.setName("assignedTechFirstCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(assignedTechFirstCheckBox, gridBagConstraints);

		resetToFirstTechCheckBox.setText(resourceMap.getString("resetToFirstTechCheckBox.text")); // NOI18N
        resetToFirstTechCheckBox.setToolTipText(resourceMap.getString("resetToFirstTechCheckBox.toolTipText")); // NOI18N
        resetToFirstTechCheckBox.setName("resetToFirstTechCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(resetToFirstTechCheckBox, gridBagConstraints);

        useQuirksBox.setText(resourceMap.getString("useQuirksBox.text")); // NOI18N
        useQuirksBox.setToolTipText(resourceMap.getString("useQuirksBox.toolTipText")); // NOI18N
        useQuirksBox.setName("useQuirksBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useQuirksBox, gridBagConstraints);

        useDamageMargin.setText(resourceMap.getString("useDamageMargin.text")); // NOI18N
        useDamageMargin.setToolTipText(resourceMap.getString("useDamageMargin.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useDamageMargin, gridBagConstraints);

        useDamageMargin.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (useDamageMargin.isSelected()) {
                    spnDamageMargin.setEnabled(true);
                } else {
                    spnDamageMargin.setEnabled(false);
                }
            }
        });
        
        useAeroSystemHitsBox.setText(resourceMap.getString("useAeroSystemHits.text")); // NOI18N
        useAeroSystemHitsBox.setToolTipText(resourceMap.getString("useAeroSystemHits.toolTipText")); // NOI18N
        useAeroSystemHitsBox.setName("useAeroSystemHits"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useAeroSystemHitsBox, gridBagConstraints);

        spnDamageMargin = new JSpinner(new SpinnerNumberModel(options.getDestroyMargin(), 1, 20, 1));
        ((JSpinner.DefaultEditor) spnDamageMargin.getEditor()).getTextField().setEditable(false);
        spnDamageMargin.setEnabled(options.isDestroyByMargin());

        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel("Margin:"));
        pnlDamageMargin.add(spnDamageMargin);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDamageMargin, gridBagConstraints);

        spnDestroyPartTarget = new JSpinner(new SpinnerNumberModel(options.getDestroyPartTarget(), 2, 13, 1));
        ((JSpinner.DefaultEditor) spnDestroyPartTarget.getEditor()).getTextField().setEditable(false);

        JPanel pnlDestroyPartTarget = new JPanel();
        pnlDestroyPartTarget.add(new JLabel("Equipment hit in combat survives on a roll of"));
        pnlDestroyPartTarget.add(spnDestroyPartTarget);
        pnlDestroyPartTarget.add(new JLabel("or better"));


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDestroyPartTarget, gridBagConstraints);


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
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (checkMaintenance.isSelected()) {
                    spnMaintenanceDays.setEnabled(true);
                    useQualityMaintenance.setEnabled(true);
                    useUnofficalMaintenance.setEnabled(true);
                    spnMaintenanceBonus.setEnabled(true);
                    logMaintenance.setEnabled(true);
                } else {
                    spnMaintenanceDays.setEnabled(false);
                    useQualityMaintenance.setEnabled(false);
                    useUnofficalMaintenance.setEnabled(false);
                    spnMaintenanceBonus.setEnabled(false);
                    logMaintenance.setEnabled(false);
                }
            }
        });

        spnMaintenanceDays = new JSpinner(new SpinnerNumberModel(options.getMaintenanceCycleDays(), 1, 365, 1));
        ((JSpinner.DefaultEditor) spnMaintenanceDays.getEditor()).getTextField().setEditable(false);
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
        ((JSpinner.DefaultEditor) spnMaintenanceBonus.getEditor()).getTextField().setEditable(false);
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

        reverseQualityNames.setText(resourceMap.getString("reverseQualityNames.text")); // NOI18N
        reverseQualityNames.setToolTipText(resourceMap.getString("reverseQualityNames.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(reverseQualityNames, gridBagConstraints);


        useUnofficalMaintenance.setText(resourceMap.getString("useUnofficalMaintenance.text")); // NOI18N
        useUnofficalMaintenance.setToolTipText(resourceMap.getString("useUnofficalMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useUnofficalMaintenance, gridBagConstraints);

        logMaintenance.setText(resourceMap.getString("logMaintenance.text")); // NOI18N
        logMaintenance.setToolTipText(resourceMap.getString("logMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        spnAcquireWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getWaitingPeriod(), 1, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireWaitingPeriod.getEditor()).getTextField().setEditable(false);

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

        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<String>();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        acquireSkillModel.setSelectedItem(options.getAcquisitionSkill());
        choiceAcquireSkill = new JComboBox<String>(acquireSkillModel);

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
        ((JSpinner.DefaultEditor) spnAcquireClanPenalty.getEditor()).getTextField().setEditable(false);

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
        ((JSpinner.DefaultEditor) spnAcquireIsPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlIsPenalty = new JPanel();
        pnlIsPenalty.add(spnAcquireIsPenalty);
        pnlIsPenalty.add(new JLabel("Penalty for Inner Sphere equipment"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlIsPenalty, gridBagConstraints);

        txtMaxAcquisitions = new JTextField(4);
        txtMaxAcquisitions.setText(Integer.toString(options.getMaxAcquisitions()));
        txtMaxAcquisitions.setHorizontalAlignment(JTextField.RIGHT);
        txtMaxAcquisitions.setName("txtName"); // NOI18N

        JPanel pnlMaxAcquisitions = new JPanel();
        pnlMaxAcquisitions.add(txtMaxAcquisitions);
        pnlMaxAcquisitions.add(new JLabel("Maximum Acquisitions Per Day (0 for unlimited)"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlMaxAcquisitions, gridBagConstraints);

        spnNDiceTransitTime = new JSpinner(new SpinnerNumberModel(options.getNDiceTransitTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor) spnNDiceTransitTime.getEditor()).getTextField().setEditable(false);

        spnConstantTransitTime = new JSpinner(new SpinnerNumberModel(options.getConstantTransitTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor) spnConstantTransitTime.getEditor()).getTextField().setEditable(false);

        spnAcquireMosBonus = new JSpinner(new SpinnerNumberModel(options.getAcquireMosBonus(), 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMosBonus.getEditor()).getTextField().setEditable(false);

        spnAcquireMinimum = new JSpinner(new SpinnerNumberModel(options.getAcquireMinimumTime(), 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMinimum.getEditor()).getTextField().setEditable(false);

        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        choiceTransitTimeUnits = new JComboBox<String>(transitUnitModel);

        DefaultComboBoxModel<String> transitMosUnitModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMosUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));
        choiceAcquireMosUnits = new JComboBox<String>(transitMosUnitModel);

        DefaultComboBoxModel<String> transitMinUnitModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMinUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        choiceAcquireMinimumUnit = new JComboBox<String>(transitMinUnitModel);


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
        limitByYearBox.addActionListener(e -> variableTechLevelBox.setEnabled(limitByYearBox.isSelected()));

        disallowExtinctStuffBox.setText(resourceMap.getString("disallowExtinctStuffBox.text")); // NOI18N
        disallowExtinctStuffBox.setToolTipText(resourceMap.getString("disallowExtinctStuffBox.toolTipText")); // NOI18N
        disallowExtinctStuffBox.setName("disallowExtinctStuffBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(disallowExtinctStuffBox, gridBagConstraints);

        allowClanPurchasesBox.setText(resourceMap.getString("allowClanPurchasesBox.text")); // NOI18N
        allowClanPurchasesBox.setToolTipText(resourceMap.getString("allowClanPurchasesBox.toolTipText")); // NOI18N
        allowClanPurchasesBox.setName("allowClanPurchasesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowClanPurchasesBox, gridBagConstraints);

        allowISPurchasesBox.setText(resourceMap.getString("allowISPurchasesBox.text")); // NOI18N
        allowISPurchasesBox.setToolTipText(resourceMap.getString("allowISPurchasesBox.toolTipText")); // NOI18N
        allowISPurchasesBox.setName("allowISPurchasesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowISPurchasesBox, gridBagConstraints);

        allowCanonOnlyBox.setText(resourceMap.getString("allowCanonOnlyBox.text")); // NOI18N
        allowCanonOnlyBox.setToolTipText(resourceMap.getString("allowCanonOnlyBox.toolTipText")); // NOI18N
        allowCanonOnlyBox.setName("allowCanonOnlyBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonOnlyBox, gridBagConstraints);

        allowCanonRefitOnlyBox.setText(resourceMap.getString("allowCanonRefitOnlyBox.text")); // NOI18N
        allowCanonRefitOnlyBox.setToolTipText(resourceMap.getString("allowCanonRefitOnlyBox.toolTipText")); // NOI18N
        allowCanonRefitOnlyBox.setName("allowCanonRefitOnlyBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonRefitOnlyBox, gridBagConstraints);

        lblTechLevel = new JLabel(resourceMap.getString("lblTechLevel.text")); // NOI18N
        lblTechLevel.setName("lblTechLevel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        DefaultComboBoxModel<String> techLevelComboBoxModel = new DefaultComboBoxModel<String>();
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
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(choiceTechLevel, gridBagConstraints);

        variableTechLevelBox.setText(resourceMap.getString("variableTechLevelBox.text")); // NOI18N
        variableTechLevelBox.setToolTipText(resourceMap.getString("variableTechLevelBox.toolTipText")); // NOI18N
        variableTechLevelBox.setName("variableTechLevelBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(variableTechLevelBox, gridBagConstraints);

        factionIntroDateBox.setText(resourceMap.getString("factionIntroDateBox.text")); // NOI18N
        factionIntroDateBox.setToolTipText(resourceMap.getString("factionIntroDateBox.toolTipText")); // NOI18N
        factionIntroDateBox.setName("factionIntroDateBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(factionIntroDateBox, gridBagConstraints);

        useAmmoByTypeBox.setText(resourceMap.getString("useAmmoByTypeBox.text")); // NOI18N
        useAmmoByTypeBox.setToolTipText(resourceMap.getString("useAmmoByTypeBox.toolTipText")); // NOI18N
        useAmmoByTypeBox.setName("useAmmoByTypeBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
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
        
        useSupportEdgeBox.setText(resourceMap.getString("useSupportEdgeBox.text")); // NOI18N
        useSupportEdgeBox.setToolTipText(resourceMap.getString("useSupportEdgeBox.toolTipText")); // NOI18N
        useSupportEdgeBox.setName("useSupportEdgeBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useSupportEdgeBox, gridBagConstraints);

        useImplantsBox.setText(resourceMap.getString("useImplantsBox.text")); // NOI18N
        useImplantsBox.setToolTipText(resourceMap.getString("useImplantsBox.toolTipText")); // NOI18N
        useImplantsBox.setName("useImplantsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useImplantsBox, gridBagConstraints);

        chkCapturePrisoners.setText(resourceMap.getString("chkCapturePrisoners.text")); // NOI18N
        chkCapturePrisoners.setToolTipText(resourceMap.getString("chkCapturePrisoners.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(chkCapturePrisoners, gridBagConstraints);

        DefaultComboBoxModel<String> prisonerStatusModel = new DefaultComboBoxModel<String>();
        prisonerStatusModel.addElement("Prisoner");
        prisonerStatusModel.addElement("Bondsman");
        comboPrisonerStatus = new JComboBox<String>(prisonerStatusModel);
        comboPrisonerStatus.setSelectedIndex(options.getDefaultPrisonerStatus());
        JPanel pnlPrisonerStatus = new JPanel();
        pnlPrisonerStatus.add(new JLabel("Default Prisoner Status:"));
        pnlPrisonerStatus.add(comboPrisonerStatus);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(pnlPrisonerStatus, gridBagConstraints);

		altQualityAveragingCheckBox.setText(resourceMap.getString("altQualityAveragingCheckBox.text")); // NOI18N
        altQualityAveragingCheckBox.setToolTipText(resourceMap.getString("altQualityAveragingCheckBox.toolTipText")); // NOI18N
        altQualityAveragingCheckBox.setName("altQualityAveragingCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(altQualityAveragingCheckBox, gridBagConstraints);

        useAdvancedMedicalBox.setText(resourceMap.getString("useAdvancedMedicalBox.text")); // NOI18N
        useAdvancedMedicalBox.setToolTipText(resourceMap.getString("useAdvancedMedicalBox.toolTipText")); // NOI18N
        useAdvancedMedicalBox.setName("useAdvancedMedicalBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useAdvancedMedicalBox, gridBagConstraints);

        useDylansRandomXpBox.setText(resourceMap.getString("useDylansRandomXpBox.text")); // NOI18N
        useDylansRandomXpBox.setToolTipText(resourceMap.getString("useDylansRandomXpBox.toolTipText")); // NOI18N
        useDylansRandomXpBox.setName("useDylansRandomXpBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useDylansRandomXpBox, gridBagConstraints);

        spnHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getHealingWaitingPeriod(), 1, 30, 1));
        ((JSpinner.DefaultEditor) spnHealWaitingPeriod.getEditor()).getTextField().setEditable(false);
        JPanel pnlHealWaitingPeriod = new JPanel();
        pnlHealWaitingPeriod.add(spnHealWaitingPeriod);
        pnlHealWaitingPeriod.add(new JLabel("Days to wait between healing checks by doctors"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(pnlHealWaitingPeriod, gridBagConstraints);

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getNaturalHealingWaitingPeriod(),
                                                                          1, 365, 1));
        ((JSpinner.DefaultEditor) spnNaturalHealWaitingPeriod.getEditor()).getTextField().setEditable(false);
        JPanel pnlNaturalHealWaitingPeriod = new JPanel();
        pnlNaturalHealWaitingPeriod.add(spnNaturalHealWaitingPeriod);
        pnlNaturalHealWaitingPeriod.add(new JLabel("Days to wait for natural healing"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        //gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(pnlNaturalHealWaitingPeriod, gridBagConstraints);

        spnMinimumHitsForVees = new JSpinner(new SpinnerNumberModel(options.getMinimumHitsForVees(), 1, 5, 1));
        ((JSpinner.DefaultEditor) spnMinimumHitsForVees.getEditor()).getTextField().setEditable(false);
        JPanel panMinimumHitsForVees = new JPanel();
        panMinimumHitsForVees.add(spnMinimumHitsForVees);
        panMinimumHitsForVees.add(new JLabel("Minimum number of hits for wounded crews and infantry"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
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
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useRandomHitsForVees, gridBagConstraints);

        useTougherHealing = new JCheckBox(resourceMap.getString("useTougherHealing.text"));
        useTougherHealing.setSelected(options.useTougherHealing());
        useTougherHealing.setToolTipText(resourceMap.getString("useTougherHealing.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnel.add(useTougherHealing, gridBagConstraints);

        chkUseUnofficialProcreation = new JCheckBox("Use procreation (Unofficial)"); // NOI18N
        chkUseUnofficialProcreation.setSelected(options.useUnofficialProcreation());
        //chkUseUnofficialProcreation.setToolTipText(resourceMap.getString("chkUseUnofficialProcreation.toolTipText")); // NOI18N
        gridBagConstraints.gridy = 18;
        panPersonnel.add(chkUseUnofficialProcreation, gridBagConstraints);

        chkUseUnofficialProcreationNoRelationship = new JCheckBox("Use procreation without a relationship (Unofficial)"); // NOI18N
        chkUseUnofficialProcreationNoRelationship.setSelected(options.useUnofficialProcreationNoRelationship());
        //chkUseUnofficialProcreationNoRelationship.setToolTipText(resourceMap.getString("chkUseUnofficialProcreationNoRelationship.toolTipText")); // NOI18N
        gridBagConstraints.gridy = 19;
        panPersonnel.add(chkUseUnofficialProcreationNoRelationship, gridBagConstraints);

        chkUseParentage = new JCheckBox("Display children in the person panel");
        chkUseParentage.setSelected(options.useParentage());
        gridBagConstraints.gridy = 20;
        panPersonnel.add(chkUseParentage, gridBagConstraints);

        chkLogConception = new JCheckBox("Log Conception and Birth ");
        chkLogConception.setSelected(options.logConception());
        gridBagConstraints.gridy = 21;
        panPersonnel.add(chkLogConception, gridBagConstraints);

        chkUseTransfers = new JCheckBox("Log Saver - Use Reassign instead of Remove/Assign"); // NOI18N
        chkUseTransfers.setSelected(options.useTransfers());
        gridBagConstraints.gridy = 22;
        panPersonnel.add(chkUseTransfers, gridBagConstraints);

        chkUseTimeInService = new JCheckBox("Track Time In Service"); // NOI18N
        chkUseTimeInService.setSelected(options.getUseTimeInService());
        gridBagConstraints.gridy = 23;
        panPersonnel.add(chkUseTimeInService, gridBagConstraints);

        JPanel panSalary = new JPanel(new GridBagLayout());
        panSalary.setBorder(BorderFactory.createTitledBorder("Salary"));

        JPanel panMultiplier = new JPanel(new GridLayout(1, 3));
        panMultiplier.setBorder(BorderFactory.createTitledBorder("Multipliers"));
        spnSalaryCommision = new JSpinner(new SpinnerNumberModel(options.getSalaryCommissionMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor) spnSalaryCommision.getEditor()).getTextField().setEditable(false);
        JPanel panSalaryCommission = new JPanel();
        panSalaryCommission.add(spnSalaryCommision);
        panSalaryCommission.add(new JLabel("Commissioned"));
        panMultiplier.add(panSalaryCommission);

        spnSalaryEnlisted = new JSpinner(new SpinnerNumberModel(options.getSalaryEnlistedMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor) spnSalaryEnlisted.getEditor()).getTextField().setEditable(false);
        JPanel panSalaryEnlisted = new JPanel();
        panSalaryEnlisted.add(spnSalaryEnlisted);
        panSalaryEnlisted.add(new JLabel("Enlisted"));
        panMultiplier.add(panSalaryEnlisted);

        spnSalaryAntiMek = new JSpinner(new SpinnerNumberModel(options.getSalaryAntiMekMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor) spnSalaryAntiMek.getEditor()).getTextField().setEditable(false);
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

        JPanel panXpMultiplier = new JPanel(new GridLayout(2, 3));
        panXpMultiplier.setBorder(BorderFactory.createTitledBorder("Experience Multipliers"));
        spnSalaryXp = new JSpinner[5];
        JSpinner spnXpSalary;
        JPanel panXpSalary;
        for (int i = 0; i < 5; i++) {
            spnXpSalary = new JSpinner(new SpinnerNumberModel(options.getSalaryXpMultiplier(i), 0, 10, 0.05));
            ((JSpinner.DefaultEditor) spnXpSalary.getEditor()).getTextField().setEditable(false);
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

        JPanel panAllTypes = new JPanel(new GridLayout(Person.T_NUM / 2, 2));
        JTextField txtType;
        JPanel panType;
        txtSalaryBase = new JTextField[Person.T_NUM];
        for (int i = 1; i < Person.T_NUM; i++) {
            txtType = new JTextField();
            txtType.setText(Integer.toString(options.getBaseSalary(i)));
            txtType.setPreferredSize(new Dimension(75, 20));
            panType = new JPanel(new GridBagLayout());
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panType.add(new JLabel(Person.getRoleDesc(i, false)), gridBagConstraints);
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
        scrSalaryBase.setPreferredSize(new Dimension(200, 200));
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
        gridBagConstraints.gridheight = 23;
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

        // Unofficial infantry don't count for contract pay
        useInfantryDontCountBox = new JCheckBox(resourceMap.getString("infantryDontCount.text")); // NOI18N
        useInfantryDontCountBox.setToolTipText(resourceMap.getString("infantryDontCount.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useInfantryDontCountBox, gridBagConstraints);

        // Campaign Operations Peacetime operating costs
        usePeacetimeCostBox.setText(resourceMap.getString("usePeacetimeCostBox.text")); // NOI18N
        usePeacetimeCostBox.setToolTipText(resourceMap.getString("usePeacetimeCostBox.toolTipText")); // NOI18N
        usePeacetimeCostBox.setName("usePeacetimeCostBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePeacetimeCostBox, gridBagConstraints);

        useExtendedPartsModifierBox.setText(resourceMap.getString("useExtendedPartsModifierBox.text")); // NOI18N
        //useExtendedPartsModifierBox.setToolTipText(resourceMap.getString("useExtendedPartsModifierBox.toolTipText")); // NOI18N
        useExtendedPartsModifierBox.setName("useExtendedPartsModifierBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useExtendedPartsModifierBox, gridBagConstraints);

        showPeacetimeCostBox.setText(resourceMap.getString("showPeacetimeCostBox.text")); // NOI18N
        showPeacetimeCostBox.setToolTipText(resourceMap.getString("showPeacetimeCostBox.toolTipText")); // NOI18N
        showPeacetimeCostBox.setName("showPeacetimeCostBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(showPeacetimeCostBox, gridBagConstraints);


        clanPriceModifierLabel.setText(resourceMap.getString("clanPriceModifierLabel.text")); // NOI18N
        clanPriceModifierLabel.setName("clanPriceModifierLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panFinances.add(clanPriceModifierLabel, gridBagConstraints);

        spnClanPriceModifier = new JSpinner(new SpinnerNumberModel(options.getClanPriceModifier(), 1.0, null, 0.1));
        spnClanPriceModifier.setEditor(new JSpinner.NumberEditor(spnClanPriceModifier, "0.00"));
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
        for (int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            gridBagConstraints.gridy++;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            panFinances.add(new JLabel(Part.getQualityName(i, options.reverseQualityNames()) + " Quality"), gridBagConstraints);
            gridBagConstraints.gridx = 2;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            spnUsedPartsValue[i] = new JSpinner(new SpinnerNumberModel(options.getUsedPartsValue(i), 0.00, 1.00, 0.05));
            spnUsedPartsValue[i].setEditor(new JSpinner.NumberEditor(spnUsedPartsValue[i], "0.00"));
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
        spnDamagedPartsValue.setEditor(new JSpinner.NumberEditor(spnDamagedPartsValue, "0.00"));
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
        spnOrderRefund.setEditor(new JSpinner.NumberEditor(spnOrderRefund, "0.00"));
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

        btnContractEquipment = new JRadioButton(resourceMap.getString("panMercenary.IntOpsPayment.title"));
        btnContractEquipment.setToolTipText(resourceMap.getString("panMercenary.IntOpsPayment.tooltip"));
        btnContractPersonnel = new JRadioButton(resourceMap.getString("panMercenary.FMMRPayment.title"));
        btnContractPersonnel.setToolTipText(resourceMap.getString("panMercenary.FMMRPayment.tooltip"));

        if (options.useEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }

        spnEquipPercent = new JSpinner(new SpinnerNumberModel(options.getEquipmentContractPercent(), 0.1, 5.0, 0.1));
        spnEquipPercent.setEditor(new JSpinner.NumberEditor(spnEquipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnEquipPercent.getEditor()).getTextField().setEditable(false);

        spnDropshipPercent = new JSpinner(new SpinnerNumberModel(options.getDropshipContractPercent(), 0.0, 1.0, 0.1));
        spnDropshipPercent.setEditor(new JSpinner.NumberEditor(spnDropshipPercent, "0.0"));
        ((JSpinner.NumberEditor) spnDropshipPercent.getEditor()).getTextField().setEditable(false);

        spnJumpshipPercent = new JSpinner(new SpinnerNumberModel(options.getJumpshipContractPercent(), 0.0, 1.0, 0.1));
        spnJumpshipPercent.setEditor(new JSpinner.NumberEditor(spnJumpshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnJumpshipPercent.getEditor()).getTextField().setEditable(false);

        spnWarshipPercent = new JSpinner(new SpinnerNumberModel(options.getWarshipContractPercent(), 0.0, 1.0, 0.1));
        spnWarshipPercent.setEditor(new JSpinner.NumberEditor(spnWarshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnWarshipPercent.getEditor()).getTextField().setEditable(false);

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
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Combat Percent:"), gridBagConstraints);
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
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Dropship Percent:"), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(spnDropshipPercent, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Jumpship Percent:"), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(spnJumpshipPercent, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Warship Percent:"), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        //gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(spnWarshipPercent, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractPersonnel, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        //gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMercenary.add(chkBLCSaleValue, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panMercenary.TabConstraints.tabTitle"), panMercenary); // NOI18N

        Set<String> spaNames = SpecialAbility.getAllSpecialAbilities().keySet();
        //We need to create a temporary hash of special abilities that we can modify without
        //changing the underlying one in case the user cancels the changes
        tempSPA = new Hashtable<String, SpecialAbility>();
        for(String name : spaNames) {
        	tempSPA.put(name, SpecialAbility.getAbility(name).clone());
        }

        panXP.setName("panXP"); // NOI18N
        panXP.setLayout(new java.awt.GridBagLayout());

        lblScenarioXP = new JLabel(resourceMap.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(options.getScenarioXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnScenarioXP.getEditor()).getTextField().setEditable(false);

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
        spnKillXP = new JSpinner(new SpinnerNumberModel(options.getKillXPAward(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKillXP.getEditor()).getTextField().setEditable(false);

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
        spnKills = new JSpinner(new SpinnerNumberModel(options.getKillsForXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKills.getEditor()).getTextField().setEditable(false);

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
        spnTaskXP = new JSpinner(new SpinnerNumberModel(options.getTaskXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnTaskXP.getEditor()).getTextField().setEditable(false);

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
        spnNTasksXP = new JSpinner(new SpinnerNumberModel(options.getNTasksXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnNTasksXP.getEditor()).getTextField().setEditable(false);

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
        spnSuccessXP = new JSpinner(new SpinnerNumberModel(options.getSuccessXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnSuccessXP.getEditor()).getTextField().setEditable(false);

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
        spnMistakeXP = new JSpinner(new SpinnerNumberModel(options.getMistakeXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnMistakeXP.getEditor()).getTextField().setEditable(false);

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

        spnIdleXP = new JSpinner(new SpinnerNumberModel(options.getIdleXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnIdleXP.getEditor()).getTextField().setEditable(false);

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
        ((JSpinner.DefaultEditor) spnMonthsIdleXP.getEditor()).getTextField().setEditable(false);
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
        ((JSpinner.DefaultEditor) spnTargetIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnTargetIdleXP, gridBagConstraints);

        spnContractNegotiationXP = new JSpinner(new SpinnerNumberModel(options.getContractNegotiationXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnContractNegotiationXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnContractNegotiationXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to the selected negotiator for a new contract"), gridBagConstraints);

        spnAdminWeeklyXP = new JSpinner(new SpinnerNumberModel(options.getAdminXP(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to each administrator every Monday for the work of the previous"), gridBagConstraints);

        spnAdminWeeklyXPPeriod = new JSpinner(new SpinnerNumberModel(options.getAdminXPPeriod(), 1, 100, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXPPeriod.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXPPeriod, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("week(s)"), gridBagConstraints);
        
        spnEdgeCost = new JSpinner(new SpinnerNumberModel(options.getEdgeCost(), 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnEdgeCost.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnEdgeCost, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP Cost for 1 Edge Point"), gridBagConstraints);

        txtInstructionsXP = new JTextArea();
        txtInstructionsXP.setText(resourceMap.getString("txtInstructionsXP.text"));
        txtInstructionsXP.setName("txtInstructions");
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setLineWrap(true);
        txtInstructionsXP.setWrapStyleWord(true);
        txtInstructionsXP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructionsXP.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructionsXP.setOpaque(false);
        txtInstructionsXP.setMinimumSize(new Dimension(550, 120));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(txtInstructionsXP, gridBagConstraints);

        String[] colNames = {"+0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10"};
        tableXP = new JTable(SkillType.getSkillCostsArray(), colNames);
        tableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableXP.setRowSelectionAllowed(false);
        tableXP.setColumnSelectionAllowed(false);
        tableXP.setCellSelectionEnabled(true);
        scrXP = new JScrollPane(tableXP);
        scrXP.setMinimumSize(new Dimension(550, 140));
        scrXP.setPreferredSize(new Dimension(550, 140));
        JTable rowTable = new RowNamesTable(tableXP);
        scrXP.setRowHeaderView(rowTable);
        scrXP.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(scrXP, gridBagConstraints);

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
        for (String skillName : SkillType.getSkillList()) {
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
            ((JSpinner.DefaultEditor) spnTarget.getEditor()).getTextField().setEditable(false);
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnGreen.getEditor()).getTextField().setEditable(false);
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnReg.getEditor()).getTextField().setEditable(false);
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnVet.getEditor()).getTextField().setEditable(false);
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnElite.getEditor()).getTextField().setEditable(false);
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

        panSpecialAbilities = new JPanel(new GridBagLayout());

        btnAddSPA = new JButton("Add Another Special Ability");
        btnAddSPA.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddSPA();
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(!getUnusedSPA().isEmpty());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =1.0;

        for(String name : spaNames) {
            panSpecialAbilities.add(new SpecialAbilityPanel(tempSPA.get(name), this), gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        JScrollPane scrSPA = new JScrollPane(panSpecialAbilities);
        scrSPA.setPreferredSize(new java.awt.Dimension(500, 400));

        tabOptions.addTab("Special Abilities", scrSPA); // NOI18N


        panRandomSkill.setName("panRandomSkill"); // NOI18N
        panRandomSkill.setLayout(new java.awt.GridBagLayout());

        JPanel panRollTable = new JPanel(new GridLayout(6, 3, 5, 0));
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        lblOverallRecruitBonus = new JLabel(resourceMap.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resourceMap.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resourceMap.getString("chkExtraRandom.toolTipText"));
        chkExtraRandom.setSelected(rskillPrefs.randomizeSkill());
        chkClanBonus = new JCheckBox(resourceMap.getString("chkClanBonus.text"));
        chkClanBonus.setToolTipText(resourceMap.getString("chkClanBonus.toolTipText"));
        chkClanBonus.setSelected(rskillPrefs.useClanBonuses());
        lblProbAntiMek = new JLabel(resourceMap.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(rskillPrefs.getAntiMekProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getOverallRecruitBonus(), -12, 12, 1));
        ((JSpinner.DefaultEditor) spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus.setToolTipText(resourceMap.getString("spnOverallRecruitBonus.toolTipText"));
        spnTypeRecruitBonus = new JSpinner[Person.T_NUM];
        int nrow = (int) Math.ceil(Person.T_NUM / 4.0);
        JPanel panTypeRecruitBonus = new JPanel(new GridLayout(nrow, 4));
        JSpinner spin;
        JPanel panRecruit;
        for (int i = 0; i < Person.T_NUM; i++) {
            panRecruit = new JPanel(new GridBagLayout());
            gridBagConstraints = new java.awt.GridBagConstraints();
            spin = new JSpinner(new SpinnerNumberModel(rskillPrefs.getRecruitBonus(i), -12, 12, 1));
            ((JSpinner.DefaultEditor) spin.getEditor()).getTextField().setEditable(false);
            spnTypeRecruitBonus[i] = spin;
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(2, 5, 0, 0);
            panRecruit.add(spin, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            panRecruit.add(new JLabel(Person.getRoleDesc(i, false)), gridBagConstraints);
            panTypeRecruitBonus.add(panRecruit);
        }

        panTypeRecruitBonus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panTypeRecruitBonus.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(chkExtraRandom, gridBagConstraints);

        JPanel panClanPheno = new JPanel(new GridLayout(2, 2));
        spnProbPhenoMW = new JSpinner(new SpinnerNumberModel(options.getProbPhenoMW(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbPhenoMW.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoMW = new JPanel();
        panPhenoMW.add(spnProbPhenoMW);
        panPhenoMW.add(new JLabel("Mechwarrior"));
        panPhenoMW.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoMW);
        spnProbPhenoAero = new JSpinner(new SpinnerNumberModel(options.getProbPhenoAero(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbPhenoAero.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoAero = new JPanel();
        panPhenoAero.add(spnProbPhenoAero);
        panPhenoAero.add(new JLabel("Aero Pilot"));
        panPhenoAero.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoAero);
        spnProbPhenoBA = new JSpinner(new SpinnerNumberModel(options.getProbPhenoBA(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbPhenoBA.getEditor()).getTextField().setEditable(false);
        JPanel panPhenoBA = new JPanel();
        panPhenoBA.add(spnProbPhenoBA);
        panPhenoBA.add(new JLabel("Elemental"));
        panPhenoBA.setToolTipText(resourceMap.getString("panPhenoMW.toolTipText"));
        panClanPheno.add(panPhenoBA);
        spnProbPhenoVee = new JSpinner(new SpinnerNumberModel(options.getProbPhenoVee(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbPhenoVee.getEditor()).getTextField().setEditable(false);
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        spnArtyProb = new JSpinner(new SpinnerNumberModel(rskillPrefs.getArtilleryProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resourceMap.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getArtilleryBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSecondSkillProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resourceMap.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSecondSkillBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Skills"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panTactics = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_GREEN), -10,
                                                              10, 1));
        ((JSpinner.DefaultEditor) spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resourceMap.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_REGULAR), -10,
                                                            10, 1));
        ((JSpinner.DefaultEditor) spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resourceMap.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_VETERAN), -10,
                                                            10, 1));
        ((JSpinner.DefaultEditor) spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resourceMap.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(rskillPrefs.getTacticsMod(SkillType.EXP_ELITE), -10,
                                                              10, 1));
        ((JSpinner.DefaultEditor) spnTacticsElite.getEditor()).getTextField().setEditable(false);
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panSmallArms = new JPanel();
        spnCombatSA = new JSpinner(new SpinnerNumberModel(rskillPrefs.getCombatSmallArmsBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resourceMap.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSupportSmallArmsBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSupportSA.getEditor()).getTextField().setEditable(false);
        spnSupportSA.setToolTipText(resourceMap.getString("spnSupportSA.toolTipText"));
        panSmallArms.add(spnCombatSA);
        panSmallArms.add(new JLabel("Combat Personnel"));
        panSmallArms.add(spnSupportSA);
        panSmallArms.add(new JLabel("Support Personnel"));
        panSmallArms.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Small Arms Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panAbilities = new JPanel();
        spnAbilGreen = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_GREEN), -10,
                                                           10, 1));
        ((JSpinner.DefaultEditor) spnAbilGreen.getEditor()).getTextField().setEditable(false);
        spnAbilGreen.setToolTipText(resourceMap.getString("spnAbilGreen.toolTipText"));
        spnAbilReg = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_REGULAR), -10,
                                                         10, 1));
        ((JSpinner.DefaultEditor) spnAbilReg.getEditor()).getTextField().setEditable(false);
        spnAbilReg.setToolTipText(resourceMap.getString("spnAbilReg.toolTipText"));
        spnAbilVet = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_VETERAN), -10,
                                                         10, 1));
        ((JSpinner.DefaultEditor) spnAbilVet.getEditor()).getTextField().setEditable(false);
        spnAbilVet.setToolTipText(resourceMap.getString("spnAbilVet.toolTipText"));
        spnAbilElite = new JSpinner(new SpinnerNumberModel(rskillPrefs.getSpecialAbilBonus(SkillType.EXP_ELITE), -10,
                                                           10, 1));
        ((JSpinner.DefaultEditor) spnAbilElite.getEditor()).getTextField().setEditable(false);
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel panOtherBonuses = new JPanel(new GridLayout(3, 2));
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(lblRank, gridBagConstraints);

        DefaultComboBoxModel<String> rankModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < Ranks.RS_NUM; i++) {
            rankModel.addElement(Ranks.getRankSystemName(i));
        }
        comboRanks.setModel(rankModel);
        comboRanks.setSelectedIndex(campaign.getRanks().getRankSystem());
        comboRanks.setName("comboRanks"); // NOI18N
        comboRanks.setActionCommand("fillRanks");
        comboRanks.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if (evt.getActionCommand().equals("fillRanks"))
            		fillRankInfo();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(comboRanks, gridBagConstraints);

        /*btnAddRank = new JButton("Add Rank");
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
        btnDeleteRank.setEnabled(false);*/

        ranksModel = new RankTableModel(campaign.getRanks().getRanksForModel(), rankColNames);
        tableRanks = new JTable(ranksModel);
        tableRanks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableRanks.setRowSelectionAllowed(false);
        tableRanks.setColumnSelectionAllowed(false);
        tableRanks.setCellSelectionEnabled(true);
        tableRanks.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableRanks.setIntercellSpacing(new Dimension(0, 0));
        tableRanks.setShowGrid(false);
        TableColumn column = null;
        for (int i = 0; i < RankTableModel.COL_NUM; i++) {
            column = tableRanks.getColumnModel().getColumn(i);
            column.setPreferredWidth(ranksModel.getColumnWidth(i));
            column.setCellRenderer(ranksModel.getRenderer());
            if (i == RankTableModel.COL_PAYMULT) {
            	column.setCellEditor(new SpinnerEditor());
            }
        }
        tableRanks.getSelectionModel().addListSelectionListener(
        		new ListSelectionListener() {
                    @Override
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        tableRanksValueChanged(evt);
                    }
                });
        AbstractAction rankCellAction = new AbstractAction() {
			private static final long serialVersionUID = -7586376360964669234L;

			@Override
			public void actionPerformed(ActionEvent e) {
				TableCellListener tcl = (TableCellListener)e.getSource();
				if (!(tcl.getOldValue().equals(tcl.getNewValue()))) {
					comboRanks.setActionCommand("noFillRanks");
					comboRanks.setSelectedIndex(Ranks.RS_CUSTOM);
					comboRanks.setActionCommand("fillRanks");
				}
			}

        };
        @SuppressWarnings("unused") // FIXME:
		TableCellListener rankCellListener = new TableCellListener(tableRanks, rankCellAction);
        scrRanks.setViewportView(tableRanks);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        scrRanks.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrRanks.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panRank.add(scrRanks, gridBagConstraints);

        //scrRanks.setMinimumSize(new Dimension(500, 500));
        //scrRanks.setPreferredSize(new Dimension(500, 500));
        //scrRanks.setMaximumSize(new Dimension(500, 500));

        JTextArea txtInstructionsRanks = new JTextArea();
        txtInstructionsRanks.setText(resourceMap.getString("txtInstructionsRanks.text"));
        txtInstructionsRanks.setEditable(false);
        txtInstructionsRanks.setLineWrap(true);
        txtInstructionsRanks.setWrapStyleWord(true);
        txtInstructionsRanks.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructionsRanks.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructionsRanks.setOpaque(false);
        txtInstructionsRanks.setMinimumSize(new Dimension(250, 120));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        //txtInstructionsRanks.setMinimumSize(new Dimension(400, 400));
        panRank.add(txtInstructionsRanks, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panRank.TabConstraints.tabTitle"), panRank); // NOI18N

        // Name and Portraits tab controls below
        panNameGen.setName("panNameGen"); // NOI18N
        panNameGen.setLayout(new java.awt.GridBagLayout());

        useFactionForNamesBox.setText(resourceMap.getString("useFactionForNamesBox.text")); // NOI18N
        useFactionForNamesBox.setToolTipText(resourceMap.getString("useFactionForNamesBox.toolTipText")); // NOI18N
        useFactionForNamesBox.setName("useFactionForNamesBox"); // NOI18N
        useFactionForNamesBox.setSelected(options.useFactionForNames());
        useFactionForNamesBox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useFactionForNamesBoxEvent(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        int gridy = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(useFactionForNamesBox, gridBagConstraints);


        lblFactionNames.setText(resourceMap.getString("lblFactionNames.text")); // NOI18N
        lblFactionNames.setName("lblFactionNames"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(lblFactionNames, gridBagConstraints);

        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<String>();
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
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(comboFactionNames, gridBagConstraints);

        lblGender.setText(resourceMap.getString("lblGender.text")); // NOI18N
        lblGender.setName("lblGender"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
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
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        panNameGen.add(sldGender, gridBagConstraints);

        panRandomPortrait.setName("panRandomPortait"); // NOI18N
        panRandomPortrait.setLayout(new BorderLayout());

        JPanel panUsePortrait = new JPanel(new GridLayout(nrow, 4));

        chkUsePortrait = new JCheckBox[Person.T_NUM];
        JCheckBox box;
        for (int i = 0; i < Person.T_NUM; i++) {
            box = new JCheckBox(Person.getRoleDesc(i, false));
            box.setSelected(options.usePortraitForType(i));
            panUsePortrait.add(box);
            chkUsePortrait[i] = box;
        }

        panRandomPortrait.add(panUsePortrait, BorderLayout.CENTER);
        JTextArea txtPortraitInst = new JTextArea(resourceMap.getString("txtPortraitInst.text"));
        txtPortraitInst.setPreferredSize(new Dimension(728, 60));
        txtPortraitInst.setEditable(false);
        txtPortraitInst.setLineWrap(true);
        txtPortraitInst.setWrapStyleWord(true);
        txtPortraitInst.setOpaque(false);
        panRandomPortrait.add(txtPortraitInst, BorderLayout.PAGE_START);

        panRandomPortrait.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panRandomPortait.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(panRandomPortrait, gridBagConstraints);

        chkAssignPortraitOnRoleChange.setText(resourceMap.getString("chkAssignPortraitOnRoleChange.text"));
        chkAssignPortraitOnRoleChange.setToolTipText(resourceMap.getString("chkAssignPortraitOnRoleChange.toolTipText"));
        chkAssignPortraitOnRoleChange.setName("chkAssignPortraitOnRoleChange");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(chkAssignPortraitOnRoleChange, gridBagConstraints);
        // assignPortraitOnRoleChange

        tabOptions.addTab(resourceMap.getString("panNameGen.TabConstraints.tabTitle"), panNameGen); // NOI18N

        // Start Personnel Market
        panPersonnelMarket = new JPanel();
        personnelMarketType = new JComboBox<String>();
        personnelMarketReportRefresh = new JCheckBox("Display a report when market refreshes");
        personnelMarketRandomEliteRemoval = new JTextField();
        personnelMarketRandomVeteranRemoval = new JTextField();
        personnelMarketRandomRegularRemoval = new JTextField();
        personnelMarketRandomGreenRemoval = new JTextField();
        personnelMarketRandomUltraGreenRemoval = new JTextField();
        personnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(options.getPersonnelMarketDylansWeight(),
                                                                          0.1, 0.8, 0.1));
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
        personnelMarketRandomUltraGreenRemoval.setText(Integer.toString(options
                                                                                .getPersonnelMarketRandomUltraGreenRemoval()));
        personnelMarketRandomEliteRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_RANDOM
                                                     || personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_DYLANS);
        personnelMarketRandomVeteranRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_RANDOM
                                                       || personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_DYLANS);
        personnelMarketRandomRegularRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_RANDOM
                                                       || personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_DYLANS);
        personnelMarketRandomGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_RANDOM
                                                     || personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_DYLANS);
        personnelMarketRandomUltraGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() == PersonnelMarket
                .TYPE_RANDOM
                                                          || personnelMarketType.getSelectedIndex() ==
                                                             PersonnelMarket.TYPE_DYLANS);

        panPersonnelMarket.setName("panPersonnelMarket");
        panPersonnelMarket.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketTypeLabel, gridBagConstraints);

        personnelMarketType.setSelectedIndex(options.getPersonnelMarketType());
        personnelMarketType.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                personnelMarketRandomEliteRemoval.setEnabled(personnelMarketType.getSelectedIndex() ==
                                                             PersonnelMarket.TYPE_RANDOM
                                                             || personnelMarketType.getSelectedIndex() ==
                                                                PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomVeteranRemoval.setEnabled(personnelMarketType.getSelectedIndex() ==
                                                               PersonnelMarket.TYPE_RANDOM
                                                               || personnelMarketType.getSelectedIndex() ==
                                                                  PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomRegularRemoval.setEnabled(personnelMarketType.getSelectedIndex() ==
                                                               PersonnelMarket.TYPE_RANDOM
                                                               || personnelMarketType.getSelectedIndex() ==
                                                                  PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() ==
                                                             PersonnelMarket.TYPE_RANDOM
                                                             || personnelMarketType.getSelectedIndex() ==
                                                                PersonnelMarket.TYPE_DYLANS);
                personnelMarketRandomUltraGreenRemoval.setEnabled(personnelMarketType.getSelectedIndex() ==
                                                                  PersonnelMarket.TYPE_RANDOM
                                                                  || personnelMarketType.getSelectedIndex() ==
                                                                     PersonnelMarket.TYPE_DYLANS);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketType, gridBagConstraints);

        personnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketReportRefresh, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomEliteRemovalLabel, gridBagConstraints);

        personnelMarketRandomEliteRemoval.setText(Integer.toString(options.getPersonnelMarketRandomEliteRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomEliteRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomVeteranRemovalLabel, gridBagConstraints);

        personnelMarketRandomVeteranRemoval.setText(Integer.toString(options.getPersonnelMarketRandomVeteranRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomVeteranRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomRegularRemovalLabel, gridBagConstraints);

        personnelMarketRandomRegularRemoval.setText(Integer.toString(options.getPersonnelMarketRandomRegularRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomRegularRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomGreenRemovalLabel, gridBagConstraints);

        personnelMarketRandomGreenRemoval.setText(Integer.toString(options.getPersonnelMarketRandomGreenRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomGreenRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomUltraGreenRemovalLabel, gridBagConstraints);

        personnelMarketRandomUltraGreenRemoval.setText(Integer.toString(options
                                                                                .getPersonnelMarketRandomUltraGreenRemoval()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketRandomUltraGreenRemoval, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketDylansWeightLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panPersonnelMarket.add(personnelMarketDylansWeight, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panPersonnelMarket.TabConstraints.tabTitle"),
                          panPersonnelMarket); // NOI18N
        // End Personnel Market

        // Start Against the Bot

        panAtB = new JPanel();
        chkUseAtB = new JCheckBox();

        cbSkillLevel = new JComboBox<String>();
        chkUseShareSystem = new JCheckBox();
        chkSharesExcludeLargeCraft = new JCheckBox();
        chkSharesForAll = new JCheckBox();
        chkRetirementRolls = new JCheckBox();
        chkTrackUnitFatigue = new JCheckBox();
        chkCustomRetirementMods = new JCheckBox();
        chkFoundersNeverRetire = new JCheckBox();
        chkTrackOriginalUnit = new JCheckBox();
        chkLimitLanceWeight = new JCheckBox();
        chkLimitLanceNumUnits = new JCheckBox();
        chkUseLeadership = new JCheckBox();
        chkUseStrategy = new JCheckBox();
        spnBaseStrategyDeployment = new JSpinner();
        spnAdditionalStrategyDeployment = new JSpinner();
        chkAdjustPaymentForStrategy = new JCheckBox();

        chkUseAero = new JCheckBox();
        chkUseVehicles = new JCheckBox();
        chkClanVehicles = new JCheckBox();
        chkDoubleVehicles = new JCheckBox();
        chkAdjustPlayerVehicles = new JCheckBox();
        spnOpforLanceTypeMechs = new JSpinner();
        spnOpforLanceTypeMixed = new JSpinner();
        spnOpforLanceTypeVehicles = new JSpinner();
        chkOpforUsesVTOLs = new JCheckBox();
        chkOpforUsesAero = new JCheckBox();
        chkOpforUsesLocalForces = new JCheckBox();
        chkUseDropShips = new JCheckBox();
        lblOpforAeroChance = new JLabel();
        lblOpforLocalForceChance = new JLabel();
        spnOpforAeroChance = new JSpinner();
        spnOpforLocalForceChance = new JSpinner();

        availableRats = new JList<String>();
        chosenRats = new JList<String>();
        btnAddRat = new JButton();
        btnRemoveRat = new JButton();
        btnMoveRatUp = new JButton();
        btnMoveRatDown = new JButton();
        chkIgnoreRatEra = new JCheckBox();

        spnSearchRadius = new JSpinner();
        spnIntensity = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkMercSizeLimited = new JCheckBox();
        chkRestrictPartsByMission = new JCheckBox();
        chkRegionalMechVariations = new JCheckBox();
        chkUseWeatherConditions = new JCheckBox();
        chkUseLightConditions = new JCheckBox();
        chkUsePlanetaryConditions = new JCheckBox();
        chkUseAtBCapture = new JCheckBox();
        spnStartGameDelay = new JSpinner();

        chkAeroRecruitsHaveUnits = new JCheckBox();
        chkInstantUnitMarketDelivery = new JCheckBox();
        chkContractMarketReportRefresh = new JCheckBox();
        chkUnitMarketReportRefresh = new JCheckBox();


        panAtB.setName("panAtB");
        panAtB.setLayout(new java.awt.GridBagLayout());

        JPanel panSubAtBAdmin = new JPanel(new GridBagLayout());
        JPanel panSubAtBRat = new JPanel(new GridBagLayout());
        JPanel panSubAtBContract = new JPanel(new GridBagLayout());
        JPanel panSubAtBScenario = new JPanel(new GridBagLayout());
        panSubAtBAdmin.setBorder(BorderFactory.createTitledBorder("Unit Administration"));
        panSubAtBRat.setBorder(BorderFactory.createTitledBorder("Random Assignment Tables"));
        panSubAtBContract.setBorder(BorderFactory.createTitledBorder("Contract Operations"));
        panSubAtBScenario.setBorder(BorderFactory.createTitledBorder("Scenarios"));

        chkUseAtB.setText(resourceMap.getString("chkUseAtB.text"));
        chkUseAtB.setToolTipText(resourceMap.getString("chkUseAtB.toolTipText"));
        chkUseAtB.setSelected(options.getUseAtB());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panAtB.add(chkUseAtB, gridBagConstraints);
        chkUseAtB.addActionListener(ev -> {
        	enableAtBComponents(panAtB, chkUseAtB.isSelected());
        	enableAtBComponents(panSubAtBRat,
        			chkUseAtB.isSelected() && btnStaticRATs.isSelected());
        });

        JLabel lblSkillLevel = new JLabel(resourceMap.getString("lblSkillLevel.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        panAtB.add(lblSkillLevel, gridBagConstraints);

        cbSkillLevel.addItem("Ultra-green");
        cbSkillLevel.addItem("Green");
        cbSkillLevel.addItem("Regular");
        cbSkillLevel.addItem("Veteran");
        cbSkillLevel.addItem("Elite");
        cbSkillLevel.setSelectedIndex(options.getSkillLevel());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panAtB.add(cbSkillLevel, gridBagConstraints);

        btnDynamicRATs = new JRadioButton(resourceMap.getString("btnDynamicRATs.text"));
        btnDynamicRATs.setToolTipText(resourceMap.getString("btnDynamicRATs.tooltip"));
        btnDynamicRATs.setSelected(!options.useStaticRATs());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        panAtB.add(btnDynamicRATs, gridBagConstraints);

        btnStaticRATs = new JRadioButton(resourceMap.getString("btnStaticRATs.text"));
        btnStaticRATs.setToolTipText(resourceMap.getString("btnStaticRATs.tooltip"));
        btnStaticRATs.setSelected(options.useStaticRATs());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        panAtB.add(btnStaticRATs, gridBagConstraints);
        btnStaticRATs.addItemListener(ev -> enableAtBComponents(panSubAtBRat,
        		btnStaticRATs.isSelected()));
        
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(panSubAtBAdmin, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        panAtB.add(panSubAtBRat, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        panAtB.add(panSubAtBContract, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        panAtB.add(panSubAtBScenario, gridBagConstraints);

        // AtB options: "Unit Administration" frame controls
        chkUseShareSystem.setText(resourceMap.getString("chkUseShareSystem.text"));
        chkUseShareSystem.setToolTipText(resourceMap.getString("chkUseShareSystem.toolTipText"));
        chkUseShareSystem.setSelected(options.getUseShareSystem());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAtBAdmin.add(chkUseShareSystem, gridBagConstraints);

        chkSharesExcludeLargeCraft.setText(resourceMap.getString("chkSharesExcludeLargeCraft.text"));
        chkSharesExcludeLargeCraft.setToolTipText(resourceMap.getString("chkSharesExcludeLargeCraft.toolTipText"));
        chkSharesExcludeLargeCraft.setSelected(options.getSharesExcludeLargeCraft());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesExcludeLargeCraft, gridBagConstraints);

        chkSharesForAll.setText(resourceMap.getString("chkSharesForAll.text"));
        chkSharesForAll.setToolTipText(resourceMap.getString("chkSharesForAll.toolTipText"));
        chkSharesForAll.setSelected(options.getSharesForAll());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesForAll, gridBagConstraints);

        chkAeroRecruitsHaveUnits.setText(resourceMap.getString("chkAeroRecruitsHaveUnits.text"));
        chkAeroRecruitsHaveUnits.setToolTipText(resourceMap.getString("chkAeroRecruitsHaveUnits.toolTipText"));
        chkAeroRecruitsHaveUnits.setSelected(options.getAeroRecruitsHaveUnits());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAeroRecruitsHaveUnits, gridBagConstraints);

        chkRetirementRolls.setText(resourceMap.getString("chkRetirementRolls.text"));
        chkRetirementRolls.setToolTipText(resourceMap.getString("chkRetirementRolls.toolTipText"));
        chkRetirementRolls.setSelected(options.doRetirementRolls());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkRetirementRolls, gridBagConstraints);

        chkCustomRetirementMods.setText(resourceMap.getString("chkCustomRetirementMods.text"));
        chkCustomRetirementMods.setToolTipText(resourceMap.getString("chkCustomRetirementMods.toolTipText"));
        chkCustomRetirementMods.setSelected(options.getCustomRetirementMods());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkCustomRetirementMods, gridBagConstraints);

        chkFoundersNeverRetire.setText(resourceMap.getString("chkFoundersNeverRetire.text"));
        chkFoundersNeverRetire.setToolTipText(resourceMap.getString("chkFoundersNeverRetire.toolTipText"));
        chkFoundersNeverRetire.setSelected(options.getFoundersNeverRetire());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkFoundersNeverRetire, gridBagConstraints);

        chkTrackUnitFatigue.setText(resourceMap.getString("chkTrackUnitFatigue.text"));
        chkTrackUnitFatigue.setToolTipText(resourceMap.getString("chkTrackUnitFatigue.toolTipText"));
        chkTrackUnitFatigue.setSelected(options.getTrackUnitFatigue());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackUnitFatigue, gridBagConstraints);

        chkUseLeadership.setText(resourceMap.getString("chkUseLeadership.text"));
        chkUseLeadership.setToolTipText(resourceMap.getString("chkUseLeadership.toolTipText"));
        chkUseLeadership.setSelected(options.getUseLeadership());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseLeadership, gridBagConstraints);

        chkTrackOriginalUnit.setText(resourceMap.getString("chkTrackOriginalUnit.text"));
        chkTrackOriginalUnit.setToolTipText(resourceMap.getString("chkTrackOriginalUnit.toolTipText"));
        chkTrackOriginalUnit.setSelected(options.getTrackOriginalUnit());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackOriginalUnit, gridBagConstraints);

        chkUseAero.setText(resourceMap.getString("chkUseAero.text"));
        chkUseAero.setToolTipText(resourceMap.getString("chkUseAero.toolTipText"));
        chkUseAero.setSelected(options.getUseAero());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseAero, gridBagConstraints);

        chkUseVehicles.setText(resourceMap.getString("chkUseVehicles.text"));
        chkUseVehicles.setToolTipText(resourceMap.getString("chkUseVehicles.toolTipText"));
        chkUseVehicles.setSelected(options.getUseVehicles());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseVehicles, gridBagConstraints);

        chkClanVehicles.setText(resourceMap.getString("chkClanVehicles.text"));
        chkClanVehicles.setToolTipText(resourceMap.getString("chkClanVehicles.toolTipText"));
        chkClanVehicles.setSelected(options.getUseVehicles());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkClanVehicles, gridBagConstraints);

        chkInstantUnitMarketDelivery.setText(resourceMap.getString("chkInstantUnitMarketDelivery.text"));
        chkInstantUnitMarketDelivery.setToolTipText(resourceMap.getString("chkInstantUnitMarketDelivery.toolTipText"));
        chkInstantUnitMarketDelivery.setSelected(options.getInstantUnitMarketDelivery());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkInstantUnitMarketDelivery, gridBagConstraints);

        chkContractMarketReportRefresh.setText(resourceMap.getString("chkContractMarketReportRefresh.text"));
        chkContractMarketReportRefresh.setSelected(options.getContractMarketReportRefresh());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkContractMarketReportRefresh, gridBagConstraints);

        chkUnitMarketReportRefresh.setText(resourceMap.getString("chkUnitMarketReportRefresh.text"));
        chkUnitMarketReportRefresh.setSelected(options.getUnitMarketReportRefresh());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUnitMarketReportRefresh, gridBagConstraints);
        
        ButtonGroup group = new ButtonGroup();
        group.add(btnDynamicRATs);
        group.add(btnStaticRATs);

        chosenRatModel = new DefaultListModel<String>();
        for (String rat : options.getRATs()) {
           	List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
            	StringBuilder displayName = new StringBuilder(rat);
            	if (eras.size() > 0) {
            		displayName.append(" (").append(eras.get(0));
                	if (eras.size() > 1) {
                		displayName.append("-").append(eras.get(eras.size() - 1));
                	}
                	displayName.append(")");
            	}
    			chosenRatModel.addElement(displayName.toString());
        	}        	
        }
        chosenRats.setModel(chosenRatModel);
        chosenRats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chosenRats.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				btnRemoveRat.setEnabled(chosenRats.getSelectedIndex() >= 0);
				btnMoveRatUp.setEnabled(chosenRats.getSelectedIndex() > 0);
				btnMoveRatDown.setEnabled(chosenRatModel.size() > chosenRats.getSelectedIndex() + 1);
			}
        });
        availableRatModel = new DefaultListModel<String>();
        for (String rat : RATManager.getAllRATCollections().keySet()) {
           	List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
            	StringBuilder displayName = new StringBuilder(rat);
            	if (eras.size() > 0) {
            		displayName.append(" (").append(eras.get(0));
                	if (eras.size() > 1) {
                		displayName.append("-").append(eras.get(eras.size() - 1));
                	}
                	displayName.append(")");
            	}
            	if (!chosenRatModel.contains(displayName.toString())) {
            		availableRatModel.addElement(displayName.toString());
            	}
        	}        	
        }
        availableRats.setModel(availableRatModel);
        availableRats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableRats.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				btnAddRat.setEnabled(availableRats.getSelectedIndex() >= 0);
			}
        });

        JTextArea txtRatInstructions = new JTextArea();
        txtRatInstructions.setEditable(false);
        txtRatInstructions.setWrapStyleWord(true);
        txtRatInstructions.setLineWrap(true);
        txtRatInstructions.setText(resourceMap.getString("txtRatInstructions.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAtBRat.add(txtRatInstructions, gridBagConstraints);

        JLabel lblChosenRats = new JLabel(resourceMap.getString("lblChosenRats.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        panSubAtBRat.add(lblChosenRats, gridBagConstraints);

        JLabel lblAvailableRats = new JLabel(resourceMap.getString("lblAvailableRats.text"));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        panSubAtBRat.add(lblAvailableRats, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        panSubAtBRat.add(chosenRats, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        panSubAtBRat.add(availableRats, gridBagConstraints);

        JPanel panRatButtons = new JPanel();
        panRatButtons.setLayout(new javax.swing.BoxLayout(panRatButtons, javax.swing.BoxLayout.Y_AXIS));
        btnAddRat.setText(resourceMap.getString("btnAddRat.text"));
        btnAddRat.setToolTipText(resourceMap.getString("btnAddRat.toolTipText"));
        btnAddRat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedIndex = availableRats.getSelectedIndex();
				chosenRatModel.addElement(availableRats.getSelectedValue());
				availableRatModel.removeElementAt(availableRats.getSelectedIndex());
				availableRats.setSelectedIndex(Math.min(selectedIndex, availableRatModel.size() - 1));
			}
        });
        btnAddRat.setEnabled(false);
        panRatButtons.add(btnAddRat);
        btnRemoveRat.setText(resourceMap.getString("btnRemoveRat.text"));
        btnRemoveRat.setToolTipText(resourceMap.getString("btnRemoveRat.toolTipText"));
        btnRemoveRat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedIndex = chosenRats.getSelectedIndex();
				availableRatModel.addElement(chosenRats.getSelectedValue());
				chosenRatModel.removeElementAt(chosenRats.getSelectedIndex());
				chosenRats.setSelectedIndex(Math.min(selectedIndex, chosenRatModel.size() - 1));
			}
        });
        btnRemoveRat.setEnabled(false);
        panRatButtons.add(btnRemoveRat);
        btnMoveRatUp.setText(resourceMap.getString("btnMoveRatUp.text"));
        btnMoveRatUp.setToolTipText(resourceMap.getString("btnMoveRatUp.toolTipText"));
        btnMoveRatUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedIndex = chosenRats.getSelectedIndex();
				String tmp = chosenRatModel.getElementAt(selectedIndex);
				chosenRatModel.setElementAt(chosenRatModel.getElementAt(selectedIndex - 1), selectedIndex);
				chosenRatModel.setElementAt(tmp, selectedIndex - 1);
				chosenRats.setSelectedIndex(selectedIndex - 1);
			}
        });
        btnMoveRatUp.setEnabled(false);
        panRatButtons.add(btnMoveRatUp);
        btnMoveRatDown.setText(resourceMap.getString("btnMoveRatDown.text"));
        btnMoveRatDown.setToolTipText(resourceMap.getString("btnMoveRatDown.toolTipText"));
        btnMoveRatDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedIndex = chosenRats.getSelectedIndex();
				String tmp = chosenRatModel.getElementAt(selectedIndex);
				chosenRatModel.setElementAt(chosenRatModel.getElementAt(selectedIndex + 1), selectedIndex);
				chosenRatModel.setElementAt(tmp, selectedIndex + 1);
				chosenRats.setSelectedIndex(selectedIndex + 1);
			}
        });
        btnMoveRatDown.setEnabled(false);
        panRatButtons.add(btnMoveRatDown);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        panSubAtBRat.add(panRatButtons, gridBagConstraints);
        
        chkIgnoreRatEra.setText(resourceMap.getString("chkIgnoreRatEra.text"));
        chkIgnoreRatEra.setToolTipText(resourceMap.getString("chkIgnoreRatEra.toolTipText"));
        chkIgnoreRatEra.setSelected(options.canIgnoreRatEra());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        panSubAtBRat.add(chkIgnoreRatEra, gridBagConstraints);
        
        JLabel lblSearchRadius = new JLabel(resourceMap.getString("lblSearchRadius.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAtBContract.add(lblSearchRadius, gridBagConstraints);

        spnSearchRadius.setModel(new SpinnerNumberModel(options.getSearchRadius(), 100, 2500, 100));
        spnSearchRadius.setToolTipText(resourceMap.getString("spnSearchRadius.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        panSubAtBContract.add(spnSearchRadius, gridBagConstraints);

        chkVariableContractLength.setText(resourceMap.getString("chkVariableContractLength.text"));
        chkVariableContractLength.setSelected(options.getVariableContractLength());
        chkVariableContractLength.setToolTipText(resourceMap.getString("chkVariableContractLength.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkVariableContractLength, gridBagConstraints);

        chkMercSizeLimited.setText(resourceMap.getString("chkMercSizeLimited.text"));
        chkMercSizeLimited.setSelected(options.isMercSizeLimited());
        chkMercSizeLimited.setToolTipText(resourceMap.getString("chkMercSizeLimited.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkMercSizeLimited, gridBagConstraints);

        chkRestrictPartsByMission.setText(resourceMap.getString("chkRestrictPartsByMission.text"));
        chkRestrictPartsByMission.setSelected(options.getRestrictPartsByMission());
        chkRestrictPartsByMission.setToolTipText(resourceMap.getString("chkRestrictPartsByMission.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkRestrictPartsByMission, gridBagConstraints);

        chkLimitLanceWeight.setText(resourceMap.getString("chkLimitLanceWeight.text"));
        chkLimitLanceWeight.setSelected(options.getLimitLanceWeight());
        chkLimitLanceWeight.setToolTipText(resourceMap.getString("chkLimitLanceWeight.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceWeight, gridBagConstraints);

        chkLimitLanceNumUnits.setText(resourceMap.getString("chkLimitLanceNumUnits.text"));
        chkLimitLanceNumUnits.setSelected(options.getLimitLanceNumUnits());
        chkLimitLanceNumUnits.setToolTipText(resourceMap.getString("chkLimitLanceNumUnits.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceNumUnits, gridBagConstraints);

        JLabel lblLanceStructure = new JLabel(resourceMap.getString("lblLanceStructure.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblLanceStructure, gridBagConstraints);

        chkUseStrategy.setText(resourceMap.getString("chkUseStrategy.text"));
        chkUseStrategy.setSelected(options.getUseStrategy());
        chkUseStrategy.setToolTipText(resourceMap.getString("chkUseStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkUseStrategy, gridBagConstraints);

        JLabel lblBaseStrategyDeployment = new JLabel(resourceMap.getString("lblBaseStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBaseStrategyDeployment, gridBagConstraints);

        spnBaseStrategyDeployment.setModel(new SpinnerNumberModel(options.getBaseStrategyDeployment(), 0, 10, 1));
        spnBaseStrategyDeployment.setToolTipText(resourceMap.getString("spnBaseStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        panSubAtBContract.add(spnBaseStrategyDeployment, gridBagConstraints);

        JLabel lblAdditionalStrategyDeployment = new JLabel(resourceMap.getString("lblAdditionalStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblAdditionalStrategyDeployment, gridBagConstraints);

        spnAdditionalStrategyDeployment.setModel(new SpinnerNumberModel(options.getAdditionalStrategyDeployment(), 0, 10, 1));
        spnAdditionalStrategyDeployment.setToolTipText(resourceMap.getString("spnAdditionalStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        panSubAtBContract.add(spnAdditionalStrategyDeployment, gridBagConstraints);

        chkAdjustPaymentForStrategy.setText(resourceMap.getString("chkAdjustPaymentForStrategy.text"));
        chkAdjustPaymentForStrategy.setSelected(options.getAdjustPaymentForStrategy());
        chkAdjustPaymentForStrategy.setToolTipText(resourceMap.getString("chkAdjustPaymentForStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkAdjustPaymentForStrategy, gridBagConstraints);

        JLabel lblIntensity = new JLabel(resourceMap.getString("lblIntensity.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(lblIntensity, gridBagConstraints);

        spnIntensity.setModel(new SpinnerNumberModel(options.getIntensity(), 0.1, 5.0, 0.1));
        spnIntensity.setToolTipText(resourceMap.getString("spnIntensity.toolTipText"));
        spnIntensity.setValue(options.getIntensity());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(spnIntensity, gridBagConstraints);
        spnIntensity.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				updateBattleChances();
			}
        });

        JLabel lblBattleFrequency = new JLabel(resourceMap.getString("lblBattleFrequency.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(lblBattleFrequency, gridBagConstraints);

        JLabel lblFightChance = new JLabel(resourceMap.getString("lblFightChance.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblFightChance, gridBagConstraints);

        lblFightPct = new JLabel();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        panSubAtBContract.add(lblFightPct, gridBagConstraints);

        JLabel lblDefendChance = new JLabel(resourceMap.getString("lblDefendChance.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblDefendChance, gridBagConstraints);

        lblDefendPct = new JLabel();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        panSubAtBContract.add(lblDefendPct, gridBagConstraints);

        JLabel lblScoutChance = new JLabel(resourceMap.getString("lblScoutChance.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblScoutChance, gridBagConstraints);

        lblScoutPct = new JLabel();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        panSubAtBContract.add(lblScoutPct, gridBagConstraints);

        JLabel lblTrainingChance = new JLabel(resourceMap.getString("lblTrainingChance.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblTrainingChance, gridBagConstraints);

        lblTrainingPct = new JLabel();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        panSubAtBContract.add(lblTrainingPct, gridBagConstraints);

        updateBattleChances();

        int yTablePosition = 0;
        chkDoubleVehicles.setText(resourceMap.getString("chkDoubleVehicles.text"));
        chkDoubleVehicles.setToolTipText(resourceMap.getString("chkDoubleVehicles.toolTipText"));
        chkDoubleVehicles.setSelected(options.getDoubleVehicles());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkDoubleVehicles, gridBagConstraints);

        JLabel lblOpforLanceType = new JLabel(resourceMap.getString("lblOpforLanceType.text"));
        lblOpforLanceType.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(lblOpforLanceType, gridBagConstraints);

        spnOpforLanceTypeMechs.setModel(new SpinnerNumberModel(options.getOpforLanceTypeMechs(), 0, 10, 1));
        spnOpforLanceTypeMechs.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeMixed.setModel(new SpinnerNumberModel(options.getOpforLanceTypeMixed(), 0, 10, 1));
        spnOpforLanceTypeMixed.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeVehicles.setModel(new SpinnerNumberModel(options.getOpforLanceTypeVehicles(), 0, 10, 1));
        spnOpforLanceTypeVehicles.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        JPanel panOpforLanceType = new JPanel();
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeMek.text")));
        panOpforLanceType.add(spnOpforLanceTypeMechs);
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeMixed.text")));
        panOpforLanceType.add(spnOpforLanceTypeMixed);
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeVehicle.text")));
        panOpforLanceType.add(spnOpforLanceTypeVehicles);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLanceType, gridBagConstraints);

        chkOpforUsesVTOLs.setText(resourceMap.getString("chkOpforUsesVTOLs.text"));
        chkOpforUsesVTOLs.setToolTipText(resourceMap.getString("chkOpforUsesVTOLs.toolTipText"));
        chkOpforUsesVTOLs.setSelected(options.getOpforUsesVTOLs());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkOpforUsesVTOLs, gridBagConstraints);
        
        JPanel panOpforAero = new JPanel();
        chkOpforUsesAero.setText(resourceMap.getString("chkOpforUsesAero.text"));
        chkOpforUsesAero.setToolTipText(resourceMap.getString("chkOpforUsesAero.toolTipText"));
        chkOpforUsesAero.setSelected(options.getAllowOpforAeros());
        lblOpforAeroChance.setText(resourceMap.getString("lblOpforAeroLikelyhood.text"));
        lblOpforAeroChance.setToolTipText(resourceMap.getString("lblOpforAeroLikelyhood.toolTipText"));
        spnOpforAeroChance.setModel(new SpinnerNumberModel(options.getOpforAeroChance(), 0, 6, 1));
        panOpforAero.add(chkOpforUsesAero);
        panOpforAero.add(spnOpforAeroChance);
        panOpforAero.add(lblOpforAeroChance);        
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforAero, gridBagConstraints);
        
        JPanel panOpforLocal = new JPanel();
        chkOpforUsesLocalForces.setText(resourceMap.getString("chkOpforUsesLocalForces.text"));
        chkOpforUsesLocalForces.setToolTipText(resourceMap.getString("chkOpforUsesLocalForces.toolTipText"));
        chkOpforUsesLocalForces.setSelected(options.getAllowOpforLocalUnits());
        lblOpforLocalForceChance.setText(resourceMap.getString("lblOpforAeroLikelyhood.text"));
        lblOpforLocalForceChance.setToolTipText(resourceMap.getString("lblOpforLocalForceLikelyhood.toolTipText"));
        spnOpforLocalForceChance.setModel(new SpinnerNumberModel(options.getOpforLocalUnitChance(), 0, 6, 1));
        panOpforLocal.add(chkOpforUsesLocalForces);
        panOpforLocal.add(spnOpforLocalForceChance);
        panOpforLocal.add(lblOpforLocalForceChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLocal, gridBagConstraints);

        chkAdjustPlayerVehicles.setText(resourceMap.getString("chkAdjustPlayerVehicles.text"));
        chkAdjustPlayerVehicles.setToolTipText(resourceMap.getString("chkAdjustPlayerVehicles.toolTipText"));
        chkAdjustPlayerVehicles.setSelected(options.getAdjustPlayerVehicles());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkAdjustPlayerVehicles, gridBagConstraints);

        chkRegionalMechVariations.setText(resourceMap.getString("chkRegionalMechVariations.text"));
        chkRegionalMechVariations.setToolTipText(resourceMap.getString("chkRegionalMechVariations.toolTipText"));
        chkRegionalMechVariations.setSelected(options.getRegionalMechVariations());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkRegionalMechVariations, gridBagConstraints);

        chkUseDropShips.setText(resourceMap.getString("chkUseDropShips.text"));
        chkUseDropShips.setToolTipText(resourceMap.getString("chkUseDropShips.toolTipText"));
        chkUseDropShips.setSelected(options.getUseDropShips());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseDropShips, gridBagConstraints);

        chkUseWeatherConditions.setText(resourceMap.getString("chkUseWeatherConditions.text"));
        chkUseWeatherConditions.setToolTipText(resourceMap.getString("chkUseWeatherConditions.toolTipText"));
        chkUseWeatherConditions.setSelected(options.getUseWeatherConditions());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseWeatherConditions, gridBagConstraints);

        chkUseLightConditions.setText(resourceMap.getString("chkUseLightConditions.text"));
        chkUseLightConditions.setToolTipText(resourceMap.getString("chkUseLightConditions.toolTipText"));
        chkUseLightConditions.setSelected(options.getUseLightConditions());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseLightConditions, gridBagConstraints);

        chkUsePlanetaryConditions.setText(resourceMap.getString("chkUsePlanetaryConditions.text"));
        chkUsePlanetaryConditions.setToolTipText(resourceMap.getString("chkUsePlanetaryConditions.toolTipText"));
        chkUsePlanetaryConditions.setSelected(options.getUsePlanetaryConditions());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUsePlanetaryConditions, gridBagConstraints);

        chkUseAtBCapture.setText(resourceMap.getString("chkUseAtBCapture.text"));
        chkUseAtBCapture.setToolTipText(resourceMap.getString("chkUseAtBCapture.toolTipText"));
        chkUseAtBCapture.setSelected(options.getUseAtBCapture());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseAtBCapture, gridBagConstraints);

        JTextArea txtStartGameDelay = new JTextArea();
        txtStartGameDelay.setText(resourceMap.getString("txtStartGameDelay.text"));
        txtStartGameDelay.setName("txtStartGameDelay");
        txtStartGameDelay.setEditable(false);
        txtStartGameDelay.setLineWrap(true);
        txtStartGameDelay.setWrapStyleWord(true);
        txtStartGameDelay.setOpaque(false);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(txtStartGameDelay, gridBagConstraints);

        JLabel lblStartGameDelay = new JLabel(resourceMap.getString("spnStartGameDelay.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(lblStartGameDelay, gridBagConstraints);

        spnStartGameDelay.setModel(new SpinnerNumberModel(options.getStartGameDelay(), 0, 1500, 25));
        spnStartGameDelay.setToolTipText(resourceMap.getString("spnStartGameDelay.toolTipText"));
        spnStartGameDelay.setValue(options.getStartGameDelay());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yTablePosition;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(spnStartGameDelay, gridBagConstraints);

        JScrollPane scrAtB = new JScrollPane(panAtB);
        scrAtB.setPreferredSize(new java.awt.Dimension(500, 400));

        tabOptions.addTab(resourceMap.getString("panAtB.TabConstraints.tabTitle"),
                scrAtB); // NOI18N
		enableAtBComponents(panAtB, chkUseAtB.isSelected());
		enableAtBComponents(panSubAtBRat, chkUseAtB.isSelected()
				&& btnStaticRATs.isSelected());

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() { 
                scrSPA.getVerticalScrollBar().setValue(0);
                scrAtB.getVerticalScrollBar().setValue(0);
            }
         });        

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabOptions, gridBagConstraints);

        btnOkay.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOkay.setName("btnOkay"); // NOI18N
        btnOkay.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkayActionPerformed();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnOkay, gridBagConstraints);

        btnSave.setText(resourceMap.getString("btnSave.text")); // NOI18N
        btnSave.setName("btnSave"); // NOI18N
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnSave, gridBagConstraints);

        btnLoad.setText(resourceMap.getString("btnLoad.text")); // NOI18N
        btnLoad.setName("btnLoad"); // NOI18N
        btnLoad.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoadActionPerformed();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnLoad, gridBagConstraints);


        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNameActionPerformed

    private void factionSelected() {
        if (useFactionForNamesBox.isSelected()) {
            switchFaction();
        }
    }

    private void switchFaction() {
        String factionCode = Faction.getFactionFromFullNameAndYear(String.valueOf(comboFaction.getSelectedItem()), date.get(Calendar.YEAR))
                                    .getNameGenerator();
        boolean found = false;
        for (Iterator<String> i = campaign.getRNG().getFactions(); i.hasNext(); ) {
            String nextFaction = (String) i.next();
            if (nextFaction.equals(factionCode)) {
                found = true;
                break;
            }
        }
        if (found) {
            comboFactionNames.setSelectedItem(factionCode);
        }
    }

    private void fillRankInfo() {
        Ranks ranks = new Ranks(comboRanks.getSelectedIndex());
        ranksModel.setDataVector(ranks.getRanksForModel(), rankColNames);
        TableColumn column = null;
        for (int i = 0; i < RankTableModel.COL_NUM; i++) {
            column = tableRanks.getColumnModel().getColumn(i);
            column.setPreferredWidth(ranksModel.getColumnWidth(i));
            column.setCellRenderer(ranksModel.getRenderer());
            if (i == RankTableModel.COL_PAYMULT) {
            	column.setCellEditor(new SpinnerEditor());
            }
        }
    }

    @SuppressWarnings("unused") // FIXME:
	private void tableRanksValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = tableRanks.getSelectedRow();
        //btnDeleteRank.setEnabled(row != -1);
    }

    @SuppressWarnings("unused") // FIXME
	private void addRank() {
        Object[] rank = {"Unknown", false, 1.0};
        int row = tableRanks.getSelectedRow();
        if (row == -1) {
            if (ranksModel.getRowCount() > 0) {
                rank[1] = ranksModel.getValueAt(ranksModel.getRowCount() - 1, 1);
            }
            ranksModel.addRow(rank);
            tableRanks.setRowSelectionInterval(tableRanks.getRowCount() - 1, tableRanks.getRowCount() - 1);
        } else {
            rank[1] = ranksModel.getValueAt(row, 1);
            ranksModel.insertRow(row + 1, rank);
            tableRanks.setRowSelectionInterval(row + 1, row + 1);
        }
    }

    @SuppressWarnings("unused") // FIXME
	private void removeRank() {
        int row = tableRanks.getSelectedRow();
        if (row > -1) {
            ranksModel.removeRow(row);
        }
        if (tableRanks.getRowCount() == 0) {
        	return;
        }
        if (tableRanks.getRowCount() > row) {
            tableRanks.setRowSelectionInterval(row, row);
        } else {
            tableRanks.setRowSelectionInterval(row - 1, row - 1);
        }
    }

    private void useFactionForNamesBoxEvent(java.awt.event.ActionEvent evt) {
        if (useFactionForNamesBox.isSelected()) {
            comboFactionNames.setEnabled(false);
            switchFaction();
        } else {
            comboFactionNames.setEnabled(true);
        }
    }

    private void btnLoadActionPerformed() {
    	ArrayList<GamePreset> presets = GamePreset.getGamePresetsIn(MekHQ.PRESET_DIR);

		if(!presets.isEmpty()) {
			ChooseGamePresetDialog cgpd = new ChooseGamePresetDialog(null, true, presets);
			cgpd.setVisible(true);
			if(!cgpd.wasCancelled() && null != cgpd.getSelectedPreset()) {
				cgpd.getSelectedPreset().apply(campaign);
				////TODO: it would be nice if we could just update the choices in this dialog now
				//rather than closing it, but that is currently not possible given how
				//this dialog is set up
				MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
				this.setVisible(false);
			}
		}
    }

    private void btnSaveActionPerformed() {
        final String METHOD_NAME = "btnSaveActionPerformed()"; //$NON-NLS-1$
    	if (txtName.getText().length() == 0) {
    		return;
    	}
    	GamePresetDescriptionDialog gpdd = new GamePresetDescriptionDialog(null, true, "Enter a title", "Enter description of preset");
        gpdd.setVisible(true);
        if(!gpdd.wasChanged()) {
        	return;
        }

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Saving campaign options..."); //$NON-NLS-1$
        // Choose a file...
        JFileChooser saveOptions = new JFileChooser(MekHQ.PRESET_DIR);
        saveOptions.setDialogTitle("Save Campaign Options as Presets");
        //saveCpgn.setFileFilter(new CampaignFileFilter());
        saveOptions.setSelectedFile(new File("myoptions.xml")); //$NON-NLS-1$
        int returnVal = saveOptions.showSaveDialog(getParent());

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveOptions.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = saveOptions.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        updateOptions();
        GamePreset preset = new GamePreset(gpdd.getTitle(), gpdd.getDesc(), options, rskillPrefs, SkillType.lookupHash, SpecialAbility.getAllSpecialAbilities());

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));
            preset.writeToXml(pw, 0);
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                    "Campaign options saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
            JOptionPane
                    .showMessageDialog(
                            null,
                            "Whoops, for some reason the game presets could not be saved", "Could not save presets",
                            JOptionPane.ERROR_MESSAGE);
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    	this.setVisible(false);
    }

    private void updateOptions() {
    	campaign.setName(txtName.getText());
    	campaign.calendar = date;
        // Ensure that the MegaMek year GameOption matches the campaign year
        GameOptions gameOpts = campaign.getGameOptions();
        int campaignYear = campaign.getCalendar().get(Calendar.YEAR);
        if (gameOpts.intOption("year") != campaignYear) {
            gameOpts.getOption("year").setValue(campaignYear);
        }
        campaign.setFactionCode(Faction.getFactionFromFullNameAndYear
        		(String.valueOf(comboFaction.getSelectedItem()), date.get(Calendar.YEAR)).getShortName());
        if (null != comboFactionNames.getSelectedItem()) {
            campaign.getRNG().setChosenFaction((String) comboFactionNames.getSelectedItem());
        }
        campaign.getRNG().setPerentFemale(sldGender.getValue());
        campaign.setRankSystem(comboRanks.getSelectedIndex());
        if (comboRanks.getSelectedIndex() == Ranks.RS_CUSTOM)
        {
	        campaign.getRanks().setRanksFromModel(ranksModel);
        }
        campaign.setCamoCategory(camoCategory);
        campaign.setCamoFileName(camoFileName);
        campaign.setColorIndex(colorIndex);

        for (int i = 0; i < chkUsePortrait.length; i++) {
            options.setUsePortraitForType(i, chkUsePortrait[i].isSelected());
        }

        updateSkillTypes();
        updateXPCosts();

        // Rules panel
        options.setEraMods(useEraModsCheckBox.isSelected());
        options.setAssignedTechFirst(assignedTechFirstCheckBox.isSelected());
		options.setResetToFirstTech(resetToFirstTechCheckBox.isSelected());
        options.setClanPriceModifier((Double) spnClanPriceModifier.getModel().getValue());
        for (int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            options.setUsedPartsValue((Double) spnUsedPartsValue[i].getModel().getValue(), i);
        }
        options.setDamagedPartsValue((Double) spnDamagedPartsValue.getModel().getValue());
        options.setCanceledOrderReimbursement((Double) spnOrderRefund.getModel().getValue());
        options.setDragoonRating(useUnitRatingCheckBox.isSelected());
        options.setUnitRatingMethod(UnitRatingMethod.getUnitRatingMethod((String) unitRatingMethodCombo
                .getSelectedItem()));
        options.setFactionForNames(useFactionForNamesBox.isSelected());
        options.setUseTactics(useTacticsBox.isSelected());
        campaign.getGameOptions().getOption("command_init").setValue(useTacticsBox.isSelected());
        options.setDestroyByMargin(useDamageMargin.isSelected());
        options.setDestroyMargin((Integer) spnDamageMargin.getModel().getValue());
        options.setDestroyPartTarget((Integer) spnDestroyPartTarget.getModel().getValue());
        options.setCheckMaintenance(checkMaintenance.isSelected());
        options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
        options.setReverseQualityNames(reverseQualityNames.isSelected());
        options.setUseUnofficalMaintenance(useUnofficalMaintenance.isSelected());
        options.setMaintenanceBonus((Integer) spnMaintenanceBonus.getModel().getValue());
        options.setMaintenanceCycleDays((Integer) spnMaintenanceDays.getModel().getValue());
        options.setInitBonus(useInitBonusBox.isSelected());
        campaign.getGameOptions().getOption("individual_initiative").setValue(useInitBonusBox.isSelected());
        options.setToughness(useToughnessBox.isSelected());
        campaign.getGameOptions().getOption("toughness").setValue(useToughnessBox.isSelected());
        options.setArtillery(useArtilleryBox.isSelected());
        campaign.getGameOptions().getOption("artillery_skill").setValue(useArtilleryBox.isSelected());
        options.setAbilities(useAbilitiesBox.isSelected());
        campaign.getGameOptions().getOption("pilot_advantages").setValue(useAbilitiesBox.isSelected());
        options.setEdge(useEdgeBox.isSelected());
        campaign.getGameOptions().getOption("edge").setValue(useEdgeBox.isSelected());
        options.setSupportEdge(useSupportEdgeBox.isSelected());
        options.setImplants(useImplantsBox.isSelected());
        options.setCapturePrisoners(chkCapturePrisoners.isSelected());
        campaign.getGameOptions().getOption("manei_domini").setValue(useImplantsBox.isSelected());
		options.setAltQualityAveraging(altQualityAveragingCheckBox.isSelected());
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
        options.setUseInfantryDontCount(useInfantryDontCountBox.isSelected());
        options.setSellUnits(sellUnitsBox.isSelected());
        options.setSellParts(sellPartsBox.isSelected());
        options.setUsePeacetimeCost(usePeacetimeCostBox.isSelected());
        options.setUseExtendedPartsModifier(useExtendedPartsModifierBox.isSelected());
        options.setShowPeacetimeCost(showPeacetimeCostBox.isSelected());
        options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());

        options.setEquipmentContractBase(btnContractEquipment.isSelected());
        options.setEquipmentContractPercent((Double) spnEquipPercent.getModel().getValue());
        options.setDropshipContractPercent((Double) spnDropshipPercent.getModel().getValue());
        options.setJumpshipContractPercent((Double) spnJumpshipPercent.getModel().getValue());
        options.setWarshipContractPercent((Double) spnWarshipPercent.getModel().getValue());
        options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
        options.setBLCSaleValue(chkBLCSaleValue.isSelected());

        options.setQuirks(useQuirksBox.isSelected());
        campaign.getGameOptions().getOption("stratops_quirks").setValue(useQuirksBox.isSelected());

        options.setWaitingPeriod((Integer) spnAcquireWaitingPeriod.getModel().getValue());
        options.setAcquisitionSkill((String) choiceAcquireSkill.getSelectedItem());
        options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
        options.setClanAcquisitionPenalty((Integer) spnAcquireClanPenalty.getModel().getValue());
        options.setIsAcquisitionPenalty((Integer) spnAcquireIsPenalty.getModel().getValue());
        options.setMaxAcquisitions(Integer.parseInt(txtMaxAcquisitions.getText()));


        options.setNDiceTransitTime((Integer) spnNDiceTransitTime.getModel().getValue());
        options.setConstantTransitTime((Integer) spnConstantTransitTime.getModel().getValue());
        options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());
        options.setAcquireMosBonus((Integer) spnAcquireMosBonus.getModel().getValue());
        options.setAcquireMinimumTime((Integer) spnAcquireMinimum.getModel().getValue());
        options.setAcquireMinimumTimeUnit(choiceAcquireMinimumUnit.getSelectedIndex());
        options.setAcquireMosUnit(choiceAcquireMosUnits.getSelectedIndex());


        options.setScenarioXP((Integer) spnScenarioXP.getModel().getValue());
        options.setKillsForXP((Integer) spnKills.getModel().getValue());
        options.setKillXPAward((Integer) spnKillXP.getModel().getValue());

        options.setTaskXP((Integer) spnTaskXP.getModel().getValue());
        options.setNTasksXP((Integer) spnNTasksXP.getModel().getValue());
        options.setSuccessXP((Integer) spnSuccessXP.getModel().getValue());
        options.setMistakeXP((Integer) spnMistakeXP.getModel().getValue());
        options.setIdleXP((Integer) spnIdleXP.getModel().getValue());
        options.setMonthsIdleXP((Integer) spnMonthsIdleXP.getModel().getValue());
        options.setContractNegotiationXP((Integer) spnContractNegotiationXP.getModel().getValue());
        options.setAdminXP((Integer) spnAdminWeeklyXP.getModel().getValue());
        options.setAdminXPPeriod((Integer) spnAdminWeeklyXPPeriod.getModel().getValue());
        options.setEdgeCost((Integer) spnEdgeCost.getModel().getValue());
        options.setTargetIdleXP((Integer) spnTargetIdleXP.getModel().getValue());

        options.setLimitByYear(limitByYearBox.isSelected());
        options.setDisallowExtinctStuff(disallowExtinctStuffBox.isSelected());
        options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
        options.setAllowISPurchases(allowISPurchasesBox.isSelected());
        options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
        campaign.getGameOptions().getOption("canon_only").setValue(allowCanonOnlyBox.isSelected());
        campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_ERA_BASED).setValue(variableTechLevelBox.isSelected());
        options.setVariableTechLevel(variableTechLevelBox.isSelected() && options.limitByYear());
        options.setfactionIntroDate(factionIntroDateBox.isSelected());
        campaign.updateTechFactionCode();
        options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
        options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
        options.setTechLevel(choiceTechLevel.getSelectedIndex());
        campaign.getGameOptions().getOption("techlevel").setValue((String)choiceTechLevel.getSelectedItem());

        //we need to reset healing time options through the campaign because we may need to
        //loop through personnel to make adjustments
        campaign.setHealingTimeOptions((Integer) spnHealWaitingPeriod.getModel().getValue(),
                                       (Integer) spnNaturalHealWaitingPeriod.getModel().getValue());

        options.setMinimumHitsForVees((Integer) spnMinimumHitsForVees.getModel().getValue());
        options.setUseRandomHitsForVees(useRandomHitsForVees.isSelected());
        options.setTougherHealing(useTougherHealing.isSelected());
        options.setUseUnofficialProcreation(chkUseUnofficialProcreation.isSelected());
        options.setUseUnofficialProcreationNoRelationship(chkUseUnofficialProcreationNoRelationship.isSelected());
        options.setUseParentage(chkUseParentage.isSelected());
        options.setLogConception(chkLogConception.isSelected());
        options.setUseTransfers(chkUseTransfers.isSelected());
        options.setUseTimeInService(chkUseTimeInService.isSelected());
        options.setDefaultPrisonerStatus(comboPrisonerStatus.getSelectedIndex());

        rskillPrefs.setOverallRecruitBonus((Integer) spnOverallRecruitBonus.getModel().getValue());
        for (int i = 0; i < Person.T_NUM; i++) {
            rskillPrefs.setRecruitBonus(i, (Integer) spnTypeRecruitBonus[i].getModel().getValue());
        }
        rskillPrefs.setRandomizeSkill(chkExtraRandom.isSelected());
        rskillPrefs.setAntiMekProb((Integer) spnProbAntiMek.getModel().getValue());
        rskillPrefs.setArtilleryProb((Integer) spnArtyProb.getModel().getValue());
        rskillPrefs.setArtilleryBonus((Integer) spnArtyBonus.getModel().getValue());
        rskillPrefs.setSecondSkillProb((Integer) spnSecondProb.getModel().getValue());
        rskillPrefs.setSecondSkillBonus((Integer) spnSecondBonus.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_GREEN, (Integer) spnTacticsGreen.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_REGULAR, (Integer) spnTacticsReg.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_VETERAN, (Integer) spnTacticsVet.getModel().getValue());
        rskillPrefs.setTacticsMod(SkillType.EXP_ELITE, (Integer) spnTacticsElite.getModel().getValue());
        rskillPrefs.setCombatSmallArmsBonus((Integer) spnCombatSA.getModel().getValue());
        rskillPrefs.setSupportSmallArmsBonus((Integer) spnSupportSA.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_GREEN, (Integer) spnAbilGreen.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_REGULAR, (Integer) spnAbilReg.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_VETERAN, (Integer) spnAbilVet.getModel().getValue());
        rskillPrefs.setSpecialAbilBonus(SkillType.EXP_ELITE, (Integer) spnAbilElite.getModel().getValue());

        options.setProbPhenoMW((Integer) spnProbPhenoMW.getModel().getValue());
        options.setProbPhenoAero((Integer) spnProbPhenoAero.getModel().getValue());
        options.setProbPhenoBA((Integer) spnProbPhenoBA.getModel().getValue());
        options.setProbPhenoVee((Integer) spnProbPhenoVee.getModel().getValue());

        //start salary
        for (int i = 1; i < Person.T_NUM; i++) {
            try {
                int salary = Integer.parseInt(txtSalaryBase[i].getText());
                options.setBaseSalary(salary, i);
            } catch (NumberFormatException ex) {
                //dont change it
            }
        }
        for (int i = 0; i < 5; i++) {
            options.setSalaryXpMultiplier((Double) spnSalaryXp[i].getModel().getValue(), i);
        }
        options.setSalaryCommissionMultiplier((Double) spnSalaryCommision.getModel().getValue());
        options.setSalaryEnlistedMultiplier((Double) spnSalaryEnlisted.getModel().getValue());
        options.setSalaryAntiMekMultiplier((Double) spnSalaryAntiMek.getModel().getValue());
        //end salary

        //start SPA
        SpecialAbility.replaceSpecialAbilities(tempSPA);
        //end SPA

        // Start Personnel Market
        options.setPersonnelMarketDylansWeight((Double) personnelMarketDylansWeight.getValue());
        options.setPersonnelMarketRandomEliteRemoval(Integer.parseInt(personnelMarketRandomEliteRemoval.getText()));
        options.setPersonnelMarketRandomVeteranRemoval(Integer.parseInt(personnelMarketRandomVeteranRemoval.getText()));
        options.setPersonnelMarketRandomRegularRemoval(Integer.parseInt(personnelMarketRandomRegularRemoval.getText()));
        options.setPersonnelMarketRandomGreenRemoval(Integer.parseInt(personnelMarketRandomGreenRemoval.getText()));
        options.setPersonnelMarketRandomUltraGreenRemoval(Integer.parseInt(personnelMarketRandomUltraGreenRemoval
                                                                                   .getText()));
        options.setPersonnelMarketReportRefresh(personnelMarketReportRefresh.isSelected());
        options.setPersonnelMarketType(personnelMarketType.getSelectedIndex());
        // End Personnel Market

        // Start Against the Bot
        options.setUseAtB(chkUseAtB.isSelected());
        options.setSkillLevel(cbSkillLevel.getSelectedIndex());
        options.setUseShareSystem(chkUseShareSystem.isSelected());
        options.setSharesExcludeLargeCraft(chkSharesExcludeLargeCraft.isSelected());
        options.setSharesForAll(chkSharesForAll.isSelected());
        options.setTrackOriginalUnit(chkTrackOriginalUnit.isSelected());
        options.setRetirementRolls(chkRetirementRolls.isSelected());
        options.setCustomRetirementMods(chkCustomRetirementMods.isSelected());
        options.setFoundersNeverRetire(chkFoundersNeverRetire.isSelected());
        options.setTrackUnitFatigue(chkTrackUnitFatigue.isSelected());
        options.setLimitLanceWeight(chkLimitLanceWeight.isSelected());
        options.setLimitLanceNumUnits(chkLimitLanceNumUnits.isSelected());
        options.setUseLeadership(chkUseLeadership.isSelected());
        options.setUseStrategy(chkUseStrategy.isSelected());
        options.setBaseStrategyDeployment((Integer)spnBaseStrategyDeployment.getValue());
        options.setAdditionalStrategyDeployment((Integer)spnAdditionalStrategyDeployment.getValue());
        options.setAdjustPaymentForStrategy(chkAdjustPaymentForStrategy.isSelected());

        options.setUseAero(chkUseAero.isSelected());
        options.setUseVehicles(chkUseVehicles.isSelected());
        options.setClanVehicles(chkClanVehicles.isSelected());
        options.setDoubleVehicles(chkDoubleVehicles.isSelected());
        options.setAdjustPlayerVehicles(chkAdjustPlayerVehicles.isSelected());
        options.setOpforLanceTypeMechs((Integer)spnOpforLanceTypeMechs.getValue());
        options.setOpforLanceTypeMixed((Integer)spnOpforLanceTypeMixed.getValue());
        options.setOpforLanceTypeVehicles((Integer)spnOpforLanceTypeVehicles.getValue());
        options.setOpforUsesVTOLs(chkOpforUsesVTOLs.isSelected());
        options.setAllowOpforAeros(chkOpforUsesAero.isSelected());
        options.setAllowOpforLocalUnits(chkOpforUsesLocalForces.isSelected());
        options.setOpforAeroChance((Integer) spnOpforAeroChance.getValue());
        options.setOpforLocalUnitChance((Integer) spnOpforLocalForceChance.getValue());
        options.setUseDropShips(chkUseDropShips.isSelected());

        options.setStaticRATs(btnStaticRATs.isSelected());
        options.setIgnoreRatEra(chkIgnoreRatEra.isSelected());
        //Strip dates used in display name
        String[] ratList = new String[chosenRatModel.size()];
        for (int i = 0; i < chosenRatModel.size(); i++) {
        	ratList[i] = chosenRatModel.elementAt(i).replaceFirst(" \\(.*?\\)", "");
        }
        options.setRATs(ratList);
        options.setSearchRadius((Integer)spnSearchRadius.getValue());
        options.setIntensity((Double)spnIntensity.getValue());
        options.setVariableContractLength(chkVariableContractLength.isSelected());
        options.setMercSizeLimited(chkMercSizeLimited.isSelected());
        options.setRestrictPartsByMission(chkRestrictPartsByMission.isSelected());
        options.setRegionalMechVariations(chkRegionalMechVariations.isSelected());
        options.setUseWeatherConditions(chkUseWeatherConditions.isSelected());
        options.setUseLightConditions(chkUseLightConditions.isSelected());
        options.setUsePlanetaryConditions(chkUsePlanetaryConditions.isSelected());
        options.setUseAtBCapture(chkUseAtBCapture.isSelected());
        options.setStartGameDelay((Integer)spnStartGameDelay.getValue());

        options.setAeroRecruitsHaveUnits(chkAeroRecruitsHaveUnits.isSelected());
        options.setInstantUnitMarketDelivery(chkInstantUnitMarketDelivery.isSelected());
        options.setContractMarketReportRefresh(chkContractMarketReportRefresh.isSelected());
        options.setContractMarketReportRefresh(chkUnitMarketReportRefresh.isSelected());

        // End Against the Bot
        MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));
    }

    private void btnOkayActionPerformed() {
        if (txtName.getText().length() > 0) {
        	updateOptions();
            this.setVisible(false);
        }
    }

    private void updateXPCosts() {
        for (int i = 0; i < SkillType.skillList.length; i++) {
            for (int j = 0; j < 11; j++) {
                try {
                    int cost = Integer.parseInt((String) tableXP.getValueAt(i, j));
                    SkillType.setCost(SkillType.skillList[i], cost, j);
                } catch (NumberFormatException e) {
                    MekHQ.getLogger().log(getClass(), "updateXPCosts()", LogLevel.ERROR,
                            "unreadable value in skill cost table for " + SkillType.skillList[i]); //$NON-NLS-1$
                }
            }
        }
        //campaign.getSkillCosts().setScenarioXP((Integer)spnScenarioXP.getModel().getValue());
    }

    private void updateSkillTypes() {
        for (String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (null != hashSkillTargets.get(skillName)) {
                type.setTarget((Integer) hashSkillTargets.get(skillName).getModel().getValue());
            }
            if (null != hashGreenSkill.get(skillName)) {
                type.setGreenLevel((Integer) hashGreenSkill.get(skillName).getModel().getValue());
            }
            if (null != hashRegSkill.get(skillName)) {
                type.setRegularLevel((Integer) hashRegSkill.get(skillName).getModel().getValue());
            }
            if (null != hashVetSkill.get(skillName)) {
                type.setVeteranLevel((Integer) hashVetSkill.get(skillName).getModel().getValue());
            }
            if (null != hashEliteSkill.get(skillName)) {
                type.setEliteLevel((Integer) hashEliteSkill.get(skillName).getModel().getValue());
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
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate();
            btnDate.setText(getDateAsString());
            factionModel = new SortedComboBoxModel<String>();
            for (String sname : Faction.choosableFactionCodes) {
                Faction f = Faction.getFaction(sname);
                if (f.validIn(date.get(Calendar.YEAR))) {
                    factionModel.addElement(f.getFullName(date.get(Calendar.YEAR)));
                }
            }
            factionModel.setSelectedItem(campaign.getFaction().getFullName(date.get(Calendar.YEAR)));
            comboFaction.setModel(factionModel);
        }
    }//GEN-LAST:event_btnDateActionPerformed

    private void btnCamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCamoActionPerformed
        CamoChoiceDialog ccd = new CamoChoiceDialog(frame, true, camoCategory, camoFileName, colorIndex, camos);
        ccd.setVisible(true);
        camoCategory = ccd.getCategory();
        camoFileName = ccd.getFileName();
        if (ccd.getColorIndex() != -1) {
            colorIndex = ccd.getColorIndex();
        }
        setCamoIcon();
    }//GEN-LAST:event_btnCamoActionPerformed


    private Vector<String> getUnusedSPA() {
    	Vector<String> unused = new Vector<String>();
    	PilotOptions poptions = new PilotOptions();
    	for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
    		IOptionGroup group = i.nextElement();

    		if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
    			continue;
    		}

    		for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
    			IOption option = j.nextElement();
    			if(null == tempSPA.get(option.getName())) {
    				unused.add(option.getName());
    			}
    		}
    	}
    	
    	for (String key : SpecialAbility.getAllDefaultSpecialAbilities().keySet()) {
            if(null == tempSPA.get(key) && !unused.contains(key)) {
                unused.add(key);
            }
        }
    	
    	return unused;
    }

    public Hashtable<String, SpecialAbility> getCurrentSPA() {
        return tempSPA;
    }

    private void btnAddSPA() {

    	SelectUnusedAbilityDialog suad = new SelectUnusedAbilityDialog(this.frame, getUnusedSPA(), getCurrentSPA());
    	suad.setVisible(true);

    	panSpecialAbilities.removeAll();

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(!getUnusedSPA().isEmpty());


        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =1.0;

        for(String title : tempSPA.keySet()) {
            panSpecialAbilities.add(new SpecialAbilityPanel(tempSPA.get(title), this), gridBagConstraints);
            gridBagConstraints.gridy++;
        }
        panSpecialAbilities.revalidate();
        panSpecialAbilities.repaint();
    }

    public void btnRemoveSPA(String name) {
        tempSPA.remove(name);

        //we also need to cycle through the existing SPAs and remove this one from
        //any prereqs
        for(String key: tempSPA.keySet()) {
        	SpecialAbility otherAbil = tempSPA.get(key);
    		Vector<String> prereq = otherAbil.getPrereqAbilities();
    		Vector<String> invalid = otherAbil.getInvalidAbilities();
    		Vector<String> remove = otherAbil.getRemovedAbilities();
        	if(prereq.remove(name)) {
        		otherAbil.setPrereqAbilities(prereq);
        	}
        	if(invalid.remove(name)) {
        		otherAbil.setInvalidAbilities(invalid);
        	}
        	if(remove.remove(name)) {
        		otherAbil.setRemovedAbilities(remove);
        	}
        }

        panSpecialAbilities.removeAll();

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(true);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx =1.0;
        gridBagConstraints.weighty =1.0;

        for(String title : tempSPA.keySet()) {
            panSpecialAbilities.add(new SpecialAbilityPanel(tempSPA.get(title), this), gridBagConstraints);
            gridBagConstraints.gridy++;
        }
        panSpecialAbilities.revalidate();
        panSpecialAbilities.repaint();
    }

    public String getDateAsString() {
        return dateFormat.format(date.getTime());
    }

    public void setCamoIcon() {
        if (null == camoCategory) {
            return;
        }

        if (Player.NO_CAMO.equals(camoCategory)) {
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
            if (Player.ROOT_CAMO.equals(camoCategory)) {
                camoCategory = ""; //$NON-NLS-1$
            }
            Image camo = (Image) camos.getItem(camoCategory, camoFileName);
            btnCamo.setIcon(new ImageIcon(camo));
        } catch (Exception err) {
            //err.printStackTrace();
        	JOptionPane.showMessageDialog(
        			this,
        			"Cannot find your camo file.\n"
        			+ "Setting to default color.\n"
        			+ "You should browse to the correct camo file,\n"
        			+ "or if it isn't available copy it into MekHQ's"
        			+ "data/images/camo folder.",
        			"Missing Camo File",
        			JOptionPane.WARNING_MESSAGE);
        	camoCategory = Player.NO_CAMO;
        	colorIndex = 0;
        	setCamoIcon();
        }
    }

    private void enableAtBComponents(JPanel panel, boolean enabled) {
    	for (Component c : panel.getComponents()) {
    		if (c.equals(chkUseAtB)) {
    			continue;
    		}
    		if (c instanceof JPanel) {
    			enableAtBComponents((JPanel)c, enabled);
    		} else if (enabled && c.equals(btnAddRat)) {
    			c.setEnabled(availableRats.getSelectedIndex() >= 0);
    		} else if (enabled && c.equals(btnRemoveRat)) {
    			c.setEnabled(chosenRats.getSelectedIndex() >= 0);
    		} else if (enabled && c.equals(btnMoveRatUp)) {
				c.setEnabled(chosenRats.getSelectedIndex() > 0);
    		} else if (enabled && c.equals(btnMoveRatDown)) {
				c.setEnabled(availableRats.getSelectedIndex() >= 0 &&
						chosenRatModel.size() > chosenRats.getSelectedIndex() + 1);
    		} else {
    			c.setEnabled(enabled);
    		}
    	}
    }

    private void updateBattleChances() {
    	double intensity = (Double)spnIntensity.getValue();
    	lblFightPct.setText((int)(40.0 * intensity / (40.0 * intensity + 60.0) * 100.0 + 0.5) + "%");
    	lblDefendPct.setText((int)(20.0 * intensity / (20.0 * intensity + 80.0) * 100.0 + 0.5) + "%");
    	lblScoutPct.setText((int)(60.0 * intensity / (60.0 * intensity + 40.0) * 100.0 + 0.5) + "%");
    	lblTrainingPct.setText((int)(10.0 * intensity / (10.0 * intensity + 90.0) * 100.0 + 0.5) + "%");
    }

    /*
     * Taken from:
     *  http://tips4java.wordpress.com/2008/11/18/row-number-table/
     *	Use a JTable as a renderer for row numbers of a given main table.
     *  This table must be added to the row header of the scrollpane that
     *  contains the main table.
     */
    public class RowNamesTable extends JTable
            implements ChangeListener, PropertyChangeListener {
        /**
         *
         */
        private static final long serialVersionUID = 3151119498072423302L;
        private JTable main;

        public RowNamesTable(JTable table) {
            main = table;
            main.addPropertyChangeListener(this);

            setFocusable(false);
            setAutoCreateColumnsFromModel(false);
            setModel(main.getModel());
            setSelectionModel(main.getSelectionModel());

            TableColumn column = new TableColumn();
            column.setHeaderValue(" ");
            addColumn(column);
            column.setCellRenderer(new RowNumberRenderer());

            getColumnModel().getColumn(0).setPreferredWidth(120);
            setPreferredScrollableViewportSize(getPreferredSize());
        }

        @Override
        public void addNotify() {
            super.addNotify();
            Component c = getParent();
            //  Keep scrolling of the row table in sync with the main table.
            if (c instanceof JViewport) {
                JViewport viewport = (JViewport) c;
                viewport.addChangeListener(this);
            }
        }

        /*
         *  Delegate method to main table
         */
        @Override
        public int getRowCount() {
            return main.getRowCount();
        }

        @Override
        public int getRowHeight(int row) {
            return main.getRowHeight(row);
        }

        /*
         *  This table does not use any data from the main TableModel,
         *  so just return a value based on the row parameter.
         */
        @Override
        public Object getValueAt(int row, int column) {
            return SkillType.skillList[row];
        }

        /*
         *  Don't edit data in the main TableModel by mistake
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        //
        //  Implement the ChangeListener
        //
        @Override
        public void stateChanged(ChangeEvent e) {
            //  Keep the scrolling of the row table in sync with main table
            JViewport viewport = (JViewport) e.getSource();
            JScrollPane scrollPane = (JScrollPane) viewport.getParent();
            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
        }

        //
        //  Implement the PropertyChangeListener
        //
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            //  Keep the row table in sync with the main table

            if ("selectionModel".equals(e.getPropertyName())) {
                setSelectionModel(main.getSelectionModel());
            }

            if ("model".equals(e.getPropertyName())) {
                setModel(main.getModel());
            }
        }

        /*
         *  Borrow the renderer from JDK1.4.2 table header
         */
        private class RowNumberRenderer extends DefaultTableCellRenderer {
            /**
             *
             */
            private static final long serialVersionUID = -5430873664301394767L;

            public RowNumberRenderer() {
                setHorizontalAlignment(JLabel.LEFT);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (table != null) {
                    JTableHeader header = table.getTableHeader();

                    if (header != null) {
                        setForeground(header.getForeground());
                        setBackground(header.getBackground());
                        setFont(header.getFont());
                    }
                }

                if (isSelected) {
                    setFont(getFont().deriveFont(Font.BOLD));
                }

                setText((value == null) ? "" : value.toString());
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));

                return this;
            }
        }
    }

    public static class SpinnerEditor extends DefaultCellEditor {
        /**
		 *
		 */
		private static final long serialVersionUID = -2711422398394960413L;
		JSpinner spinner;
        JSpinner.NumberEditor editor;
        JTextField textField;
        boolean valueSet;

        // Initializes the spinner.
        public SpinnerEditor() {
            super(new JTextField());
            spinner = new JSpinner(new SpinnerNumberModel(1.0, 0, 10, 0.05));
            editor = ((JSpinner.NumberEditor) spinner.getEditor());
            textField = editor.getTextField();
            textField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent fe) {
                    System.err.println("Got focus");
                    //textField.setSelectionStart(0);
                    //textField.setSelectionEnd(1);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (valueSet) {
                                textField.setCaretPosition(1);
                            }
                        }
                    });
                }

                @Override
                public void focusLost(FocusEvent fe) {
                }
            });
            textField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    stopCellEditing();
                }
            });
        }

        // Prepares the spinner component and returns it.
        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column
                                                    ) {
            if (!valueSet) {
                spinner.setValue(value);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textField.requestFocus();
                }
            });
            return spinner;
        }

        @Override
        public boolean isCellEditable(EventObject eo) {
            System.err.println("isCellEditable");
            if (eo instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) eo;
                System.err.println("key event: " + ke.getKeyChar());
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
        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            System.err.println("Stopping edit");
            try {
                editor.commitEdit();
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(null,
                                              "Invalid value, discarding.");
            }
            return super.stopCellEditing();
        }
    }

}
