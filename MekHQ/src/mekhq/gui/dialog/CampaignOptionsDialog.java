/*
 * Copyright (C) 2009, 2020 - The MegaMek Team. All rights reserved
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
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
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Ranks;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.FileDialogs;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.model.RankTableModel;
import mekhq.gui.model.SortedComboBoxModel;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.TableCellListener;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;
import mekhq.preferences.PreferencesNode;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CampaignOptionsDialog extends javax.swing.JDialog {
    //region Variable Declarations
    //region General Variables (ones not relating to a specific tab)
    private static final long serialVersionUID = 1935043247792962964L;
    private Campaign campaign;
    private CampaignOptions options;
    private RandomSkillPreferences rSkillPrefs;
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
    //endregion General Variables (ones not relating to a specific tab)

    //region Shared UI Variables
    private JTabbedPane tabOptions;
    private JButton btnOkay;
    private JButton btnSave;
    private JButton btnLoad;
    private JButton btnCancel;
    //endregion Shared UI Variables

    //region General Tab
    private JPanel panGeneral;
    private JTextField txtName;
    private JComboBox<String> comboFaction;
    SortedComboBoxModel<String> factionModel;
    private JCheckBox useUnitRatingCheckBox;
    private JComboBox<String> unitRatingMethodCombo;
    private JButton btnDate;
    private JButton btnCamo;
    //endregion General Tab

    //region Repair and Maintenance Tab
    private JPanel panRepair;
    //repair
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JSpinner spnDamageMargin;
    private JSpinner spnDestroyPartTarget;
    //maintenance
    private JCheckBox checkMaintenance;
    private JSpinner spnMaintenanceDays;
    private JSpinner spnMaintenanceBonus;
    private JCheckBox useQualityMaintenance;
    private JCheckBox reverseQualityNames;
    private JCheckBox useUnofficialMaintenance;
    private JCheckBox logMaintenance;
    //endregion Repair and Maintenance Tab

    //region Supplies and Acquisitions Tab
    private JPanel panSupplies;
    //acquisition
    private JSpinner spnAcquireWaitingPeriod;
    private JComboBox<String> choiceAcquireSkill;
    private JCheckBox chkSupportStaffOnly;
    private JSpinner spnAcquireClanPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JTextField txtMaxAcquisitions;
    //Delivery
    private JSpinner spnNDiceTransitTime;
    private JSpinner spnConstantTransitTime;
    private JComboBox<String> choiceTransitTimeUnits;
    private JSpinner spnAcquireMinimum;
    private JComboBox<String> choiceAcquireMinimumUnit;
    private JSpinner spnAcquireMosBonus;
    private JComboBox<String> choiceAcquireMosUnits;
    //planetary acquisitions
    private JCheckBox usePlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private JComboBox<String> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JCheckBox usePlanetaryAcquisitionsVerbose;
    private JSpinner[] spnPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    private JCheckBox disallowClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    //endregion Supplies and Acquisitions Tab

    //region Tech Limits Tab
    private JPanel panTech;
    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox factionIntroDateBox;
    private JCheckBox useAmmoByTypeBox;
    //endregion Tech Limits Tab

    //region Personnel Tab
    private JPanel panPersonnel;
    private JCheckBox useTacticsBox;
    private JCheckBox useInitBonusBox;
    private JCheckBox useToughnessBox;
    private JCheckBox useArtilleryBox;
    private JCheckBox useAbilitiesBox;
    private JCheckBox useEdgeBox;
    private JCheckBox useSupportEdgeBox;
    private JCheckBox useImplantsBox;
    private JCheckBox chkCapturePrisoners;
    private JComboBox<String> comboPrisonerStatus;
    private JCheckBox altQualityAveragingCheckBox;
    private JCheckBox useAdvancedMedicalBox;
    private JCheckBox useDylansRandomXpBox;
    private JSpinner spnHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JSpinner spnMinimumHitsForVees;
    private JCheckBox useRandomHitsForVees;
    private JCheckBox useTougherHealing;
    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseTimeInService;
    private JComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private JComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkShowOriginFaction;
    private JCheckBox chkRandomizeOrigin;
    private JCheckBox chkRandomizeDependentsOrigin;
    private JSpinner spnOriginSearchRadius;
    //Family
    private JSpinner spnMinimumMarriageAge;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChange;
    private JCheckBox chkUseRandomMarriages;
    private JSpinner spnChanceRandomMarriages;
    private JSpinner spnMarriageAgeRange;
    private JSpinner[] spnRandomMarriageSurnameWeights;
    private JCheckBox chkUseRandomSameSexMarriages;
    private JSpinner spnChanceRandomSameSexMarriages;
    private JCheckBox chkUseUnofficialProcreation;
    private JSpinner spnChanceProcreation;
    private JCheckBox chkUseUnofficialProcreationNoRelationship;
    private JSpinner spnChanceProcreationNoRelationship;
    private JCheckBox chkDisplayTrueDueDate;
    private JCheckBox chkLogConception;
    private JComboBox<String> comboBabySurnameStyle;
    private JCheckBox chkUseParentage;
    private JComboBox<String> comboDisplayFamilyLevel;
    private JCheckBox chkUseRandomDeaths;
    private JCheckBox chkKeepMarriedNameUponSpouseDeath;
    //Salary
    private JSpinner spnSalaryCommission;
    private JSpinner spnSalaryEnlisted;
    private JSpinner spnSalaryAntiMek;
    private JSpinner[] spnSalaryXp;
    private JSpinner[] spnSalaryBase;
    //endregion Personnel Tab

    //region Finances Tab
    private JPanel panFinances;
    private JCheckBox payForPartsBox;
    private JCheckBox payForRepairsBox;
    private JCheckBox payForUnitsBox;
    private JCheckBox payForSalariesBox;
    private JCheckBox payForOverheadBox;
    private JCheckBox payForMaintainBox;
    private JCheckBox payForTransportBox;
    private JCheckBox sellUnitsBox;
    private JCheckBox sellPartsBox;
    private JCheckBox payForRecruitmentBox;
    private JCheckBox useLoanLimitsBox;
    private JCheckBox usePercentageMaintBox;
    private JCheckBox useInfantryDontCountBox;
    private JCheckBox usePeacetimeCostBox;
    private JCheckBox useExtendedPartsModifierBox;
    private JCheckBox showPeacetimeCostBox;
    private JCheckBox newFinancialYearFinancesToCSVExportBox;
    private JComboBox<FinancialYearDuration> comboFinancialYearDuration;
    private JSpinner spnClanPriceModifier;
    private JSpinner[] spnUsedPartsValue;
    private JSpinner spnDamagedPartsValue;
    private JSpinner spnOrderRefund;
    //endregion Finances Tab

    //region Mercenary Tab
    private JPanel panMercenary;
    private JRadioButton btnContractEquipment;
    private JSpinner spnEquipPercent;
    private JSpinner spnDropshipPercent;
    private JSpinner spnJumpshipPercent;
    private JSpinner spnWarshipPercent;
    private JCheckBox chkEquipContractSaleValue;
    private JRadioButton btnContractPersonnel;
    private JCheckBox chkBLCSaleValue;
    //endregion Mercenary Tab

    //region Experience Tab
    private JPanel panXP;
    private JSpinner spnScenarioXP;
    private JSpinner spnKillXP;
    private JSpinner spnKills;
    private JSpinner spnTaskXP;
    private JSpinner spnNTasksXP;
    private JSpinner spnSuccessXP;
    private JSpinner spnMistakeXP;
    private JSpinner spnIdleXP;
    private JSpinner spnMonthsIdleXP;
    private JSpinner spnTargetIdleXP;
    private JSpinner spnContractNegotiationXP;
    private JSpinner spnAdminWeeklyXP;
    private JSpinner spnAdminWeeklyXPPeriod;
    private JSpinner spnEdgeCost;
    private JTextArea txtInstructionsXP;
    private JScrollPane scrXP;
    private JTable tableXP;
    //endregion Experience Tab

    //region Skills Tab
    private JPanel panSkill;
    //endregion Skills Tab

    //region Special Abilities Tab
    private JPanel panSpecialAbilities;
    Hashtable<String, SpecialAbility> tempSPA;
    private JButton btnAddSPA;
    //endregion Special Abilities Tab

    //region Skill Randomization Tab
    private JPanel panRandomSkill;
    private JCheckBox chkExtraRandom;
    private JSpinner spnProbPhenoMW;
    private JSpinner spnProbPhenoAero;
    private JSpinner spnProbPhenoBA;
    private JSpinner spnProbPhenoVee;
    private JSpinner spnProbAntiMek;
    private JSpinner spnOverallRecruitBonus;
    private JSpinner[] spnTypeRecruitBonus;
    private JSpinner spnArtyProb;
    private JSpinner spnArtyBonus;
    private JSpinner spnTacticsGreen;
    private JSpinner spnTacticsReg;
    private JSpinner spnTacticsVet;
    private JSpinner spnTacticsElite;
    private JSpinner spnCombatSA;
    private JSpinner spnSupportSA;
    private JSpinner spnSecondProb;
    private JSpinner spnSecondBonus;
    private JSpinner spnAbilGreen;
    private JSpinner spnAbilReg;
    private JSpinner spnAbilVet;
    private JSpinner spnAbilElite;
    //endregion Skill Randomization Tab

    //region Rank System Tab
    private JPanel panRank;
    private JComboBox<String> comboRanks;
    @SuppressWarnings("unused")
    private JButton btnAddRank; // FIXME: Unused
    @SuppressWarnings("unused")
    private JButton btnDeleteRank; // FIXME: Unused
    private JTable tableRanks;
    private RankTableModel ranksModel;
    private JScrollPane scrRanks;
    // FIXME: Place in resource files
    String[] rankColNames = { "Rate", "MW Rank", "ASF Rank", "Vee Crew Rank", "Naval Rank", "Infantry Rank", "Tech Rank", "Officer", "Pay Multiplier"};
    //endregion Rank System Tab

    //region Name and Portrait Generation Tab
    private JPanel panNameGen;
    private JCheckBox useFactionForNamesBox;
    private JComboBox<String> comboFactionNames;
    private JSlider sldGender;
    private JPanel panRandomPortrait;
    private JCheckBox[] chkUsePortrait;
    private JCheckBox chkAssignPortraitOnRoleChange;
    //endregion Name and Portrait Generation Tab

    //region Personnel Market Tab
    private JPanel panPersonnelMarket;
    private JComboBox<String> personnelMarketType;
    private JCheckBox personnelMarketReportRefresh;
    private JTextField personnelMarketRandomEliteRemoval;
    private JTextField personnelMarketRandomVeteranRemoval;
    private JTextField personnelMarketRandomRegularRemoval;
    private JTextField personnelMarketRandomGreenRemoval;
    private JTextField personnelMarketRandomUltraGreenRemoval;
    private JSpinner personnelMarketDylansWeight;
    //endregion Personnel Market Tab

    //region Against the Bot Tab
    private JPanel panAtB;
    private JCheckBox chkUseAtB;
    private JComboBox<String> cbSkillLevel;
    //unit administration
    private JCheckBox chkUseShareSystem;
    private JCheckBox chkSharesExcludeLargeCraft;
    private JCheckBox chkSharesForAll;
    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkRetirementRolls;
    private JCheckBox chkCustomRetirementMods;
    private JCheckBox chkFoundersNeverRetire;
    private JCheckBox chkAddDependents;
    private JCheckBox chkDependentsNeverLeave;
    private JCheckBox chkTrackUnitFatigue;
    private JCheckBox chkUseLeadership;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkUseAero;
    private JCheckBox chkUseVehicles;
    private JCheckBox chkClanVehicles;
    private JCheckBox chkInstantUnitMarketDelivery;
    private JCheckBox chkContractMarketReportRefresh;
    private JCheckBox chkUnitMarketReportRefresh;

    //contract operations
    private JSpinner spnSearchRadius;
    private JCheckBox chkVariableContractLength;
    private JCheckBox chkMercSizeLimited;
    private JCheckBox chkRestrictPartsByMission;
    private JCheckBox chkLimitLanceWeight;
    private JCheckBox chkLimitLanceNumUnits;
    private JCheckBox chkUseStrategy;
    private JSpinner spnBaseStrategyDeployment;
    private JSpinner spnAdditionalStrategyDeployment;
    private JCheckBox chkAdjustPaymentForStrategy;
    private JSpinner spnIntensity;
    private JLabel lblFightPct;
    private JLabel lblDefendPct;
    private JLabel lblScoutPct;
    private JLabel lblTrainingPct;

    //RATs
    private JRadioButton btnDynamicRATs;
    private JRadioButton btnStaticRATs;
    private DefaultListModel<String> chosenRatModel;
    private JList<String> chosenRats;
    private DefaultListModel<String> availableRatModel;
    private JList<String> availableRats;
    private JButton btnAddRat;
    private JButton btnRemoveRat;
    private JButton btnMoveRatUp;
    private JButton btnMoveRatDown;
    private JCheckBox chkIgnoreRatEra;

    //scenarios
    private JCheckBox chkDoubleVehicles;
    private JSpinner spnOpforLanceTypeMechs;
    private JSpinner spnOpforLanceTypeMixed;
    private JSpinner spnOpforLanceTypeVehicles;
    private JCheckBox chkOpforUsesVTOLs;
    private JCheckBox chkOpforUsesAero;
    private JSpinner spnOpforAeroChance;
    private JCheckBox chkOpforUsesLocalForces;
    private JSpinner spnOpforLocalForceChance;
    private JCheckBox chkAdjustPlayerVehicles;
    private JCheckBox chkRegionalMechVariations;
    private JCheckBox chkUseDropShips;
    private JCheckBox chkUseWeatherConditions;
    private JCheckBox chkUseLightConditions;
    private JCheckBox chkUsePlanetaryConditions;
    private JCheckBox chkUseAtBCapture;
    private JSpinner spnStartGameDelay;

    //endregion Against the Bot Tab

    //region Miscellaneous Tab
    private JPanel panMisc;
    private JCheckBox chkHistoricalDailyLog;
    //endregion Miscellaneous Tab
    //endregion Variable Declarations

    // TODO: Figure out why these are not used
    private JCheckBox chkClanBonus;

    /**
     * Creates new form CampaignOptionsDialog
     */
    public CampaignOptionsDialog(java.awt.Frame parent, boolean modal, Campaign c, DirectoryItems camos) {
        super(parent, modal);
        this.campaign = c;
        this.options = c.getCampaignOptions();
        this.rSkillPrefs = c.getRandomSkillPreferences();
        //this is a hack but I have no idea what is going on here
        this.frame = parent;
        this.date = campaign.getCalendar();
        dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy");
        this.camoCategory = campaign.getCamoCategory();
        this.camoFileName = campaign.getCamoFileName();
        this.colorIndex = campaign.getColorIndex();
        this.camos = camos;
        hashSkillTargets = new Hashtable<>();
        hashGreenSkill = new Hashtable<>();
        hashRegSkill = new Hashtable<>();
        hashVetSkill = new Hashtable<>();
        hashEliteSkill = new Hashtable<>();
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
        payForRepairsBox.setSelected(options.payForRepairs());
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

        usePlanetaryAcquisitions.setSelected(options.usesPlanetaryAcquisition());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.disallowPlanetAcquisitionClanCrossover());
        disallowClanPartsFromIS.setSelected(options.disallowClanPartsFromIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.usePlanetAcquisitionVerboseReporting());

        useDamageMargin.setSelected(options.isDestroyByMargin());
        useAeroSystemHitsBox.setSelected(options.useAeroSystemHits());
        useQualityMaintenance.setSelected(options.useQualityMaintenance());
        useUnofficialMaintenance.setSelected(options.useUnofficialMaintenance());
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
        variableTechLevelBox.setSelected(options.useVariableTechLevel() && options.limitByYear());
        variableTechLevelBox.setEnabled(options.limitByYear());
        factionIntroDateBox.setSelected(options.useFactionIntroDate());
        useAmmoByTypeBox.setSelected(options.useAmmoByType());

        useQuirksBox.setSelected(options.useQuirks());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());

        setUserPreferences();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    private void initComponents() {
        //region Variable Declaration and Initialisation
        tabOptions = new JTabbedPane();
        panGeneral = new JPanel();
        txtName = new JTextField();
        btnDate = new JButton();
        comboFaction = new JComboBox<>();
        comboFactionNames = new JComboBox<>();
        comboRanks = new JComboBox<>();
        sldGender = new JSlider(SwingConstants.HORIZONTAL);
        btnCamo = new JButton();
        panRepair = new JPanel();
        panSupplies = new JPanel();
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
        unitRatingMethodCombo = new JComboBox<>(UnitRatingMethod.getUnitRatingMethodNames());
        JLabel clanPriceModifierLabel = new JLabel();
        JLabel usedPartsValueLabel = new JLabel();
        JLabel damagedPartsValueLabel = new JLabel();
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
        payForRepairsBox = new JCheckBox();
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
        choiceTechLevel = new JComboBox<>();
        btnOkay = new JButton();
        btnSave = new JButton();
        btnLoad = new JButton();
        btnCancel = new JButton();
        scrRanks = new JScrollPane();

        usePlanetaryAcquisitions = new JCheckBox();
        usePlanetaryAcquisitionsVerbose = new JCheckBox();
        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox();
        comboPlanetaryAcquisitionsFactionLimits = new JComboBox<>();
        disallowClanPartsFromIS = new JCheckBox();
        useDamageMargin = new JCheckBox();
        useAeroSystemHitsBox = new JCheckBox();
        useQualityMaintenance = new JCheckBox();
        useUnofficialMaintenance = new JCheckBox();
        checkMaintenance = new JCheckBox();
        reverseQualityNames = new JCheckBox();

        chkSupportStaffOnly = new JCheckBox();

        panMisc = new JPanel();
        chkHistoricalDailyLog = new JCheckBox();

        java.awt.GridBagConstraints gridBagConstraints;
        int gridy = 0;
        //endregion Variable Declaration and Initialisation

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
        txtName.addActionListener(this::txtNameActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(txtName, gridBagConstraints);

        JLabel lblName = new JLabel(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblName, gridBagConstraints);

        JLabel lblFaction = new JLabel(resourceMap.getString("lblFaction.text")); // NOI18N
        lblFaction.setName("lblFaction"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(lblFaction, gridBagConstraints);

        JLabel lblDate = new JLabel(resourceMap.getString("lblDate.text")); // NOI18N
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
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panGeneral.add(btnDate, gridBagConstraints);

        factionModel = new SortedComboBoxModel<>();
        for (String sName : Faction.choosableFactionCodes) {
            Faction f = Faction.getFaction(sName);
            if (f.validIn(date.get(Calendar.YEAR))) {
                factionModel.addElement(f.getFullName(date.get(Calendar.YEAR)));
            }
        }
        factionModel.setSelectedItem(campaign.getFaction().getFullName(date.get(Calendar.YEAR)));
        comboFaction.setModel(factionModel);
        comboFaction.setMinimumSize(new java.awt.Dimension(400, 30));
        comboFaction.setName("comboFaction"); // NOI18N
        comboFaction.setPreferredSize(new java.awt.Dimension(400, 30));
        comboFaction.addActionListener(evt -> factionSelected());
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
        btnCamo.addActionListener(this::btnCamoActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(btnCamo, gridBagConstraints);

        JLabel lblCamo = new JLabel(resourceMap.getString("lblCamo.text")); // NOI18N
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

        panSubRepair.setBorder(BorderFactory.createTitledBorder("Repair"));
        panSubMaintenance.setBorder(BorderFactory.createTitledBorder("Maintenance"));

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
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panRepair.add(panSubMaintenance, gridBagConstraints);

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

        useDamageMargin.setText(resourceMap.getString("useDamageMargin.text")); // NOI18N
        useDamageMargin.setToolTipText(resourceMap.getString("useDamageMargin.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useDamageMargin, gridBagConstraints);

        useDamageMargin.addActionListener(evt -> {
            if (useDamageMargin.isSelected()) {
                spnDamageMargin.setEnabled(true);
            } else {
                spnDamageMargin.setEnabled(false);
            }
        });

        spnDamageMargin = new JSpinner(new SpinnerNumberModel(options.getDestroyMargin(), 1, 20, 1));
        ((JSpinner.DefaultEditor) spnDamageMargin.getEditor()).getTextField().setEditable(false);
        spnDamageMargin.setEnabled(options.isDestroyByMargin());

        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel("Margin:"));
        pnlDamageMargin.add(spnDamageMargin);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
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
        gridBagConstraints.gridy = 7;
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

        checkMaintenance.addActionListener(evt -> {
            if (checkMaintenance.isSelected()) {
                spnMaintenanceDays.setEnabled(true);
                useQualityMaintenance.setEnabled(true);
                useUnofficialMaintenance.setEnabled(true);
                reverseQualityNames.setEnabled(true);
                spnMaintenanceBonus.setEnabled(true);
                logMaintenance.setEnabled(true);
            } else {
                spnMaintenanceDays.setEnabled(false);
                useQualityMaintenance.setEnabled(false);
                useUnofficialMaintenance.setEnabled(false);
                reverseQualityNames.setEnabled(false);
                spnMaintenanceBonus.setEnabled(false);
                logMaintenance.setEnabled(false);
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


        useUnofficialMaintenance.setText(resourceMap.getString("useUnofficialMaintenance.text")); // NOI18N
        useUnofficialMaintenance.setToolTipText(resourceMap.getString("useUnofficialMaintenance.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useUnofficialMaintenance, gridBagConstraints);

        logMaintenance = new JCheckBox(resourceMap.getString("logMaintenance.text")); // NOI18N
        logMaintenance.setToolTipText(resourceMap.getString("logMaintenance.toolTipText")); // NOI18N
        logMaintenance.setName("logMaintenance");
        logMaintenance.setSelected(options.logMaintenance());
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("panRepair.TabConstraints.tabTitle"), panRepair); // NOI18N

        panSupplies.setName("panSupplies"); // NOI18N
        panSupplies.setLayout(new java.awt.GridBagLayout());

        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());
        JPanel panSubPlanetAcquire = new JPanel(new GridBagLayout());

        panSubAcquire.setBorder(BorderFactory.createTitledBorder("Acquisition"));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder("Delivery"));
        panSubPlanetAcquire.setBorder(BorderFactory.createTitledBorder("Planetary Acquisition"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubAcquire, gridBagConstraints);


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubDelivery, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubPlanetAcquire, gridBagConstraints);


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

        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        acquireSkillModel.setSelectedItem(options.getAcquisitionSkill());
        choiceAcquireSkill = new JComboBox<>(acquireSkillModel);

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

        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        choiceTransitTimeUnits = new JComboBox<>(transitUnitModel);

        DefaultComboBoxModel<String> transitMosUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMosUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));
        choiceAcquireMosUnits = new JComboBox<>(transitMosUnitModel);

        DefaultComboBoxModel<String> transitMinUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        transitMinUnitModel.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        choiceAcquireMinimumUnit = new JComboBox<>(transitMinUnitModel);


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

        usePlanetaryAcquisitions.setText(resourceMap.getString("usePlanetaryAcquisitions.text")); // NOI18N
        usePlanetaryAcquisitions.setToolTipText(resourceMap.getString("usePlanetaryAcquisitions.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitions, gridBagConstraints);

        spnMaxJumpPlanetaryAcquisitions = new JSpinner(new SpinnerNumberModel(options.getMaxJumpsPlanetaryAcquisition(), 0, 5, 1));
        JPanel panMaxJump = new JPanel();
        panMaxJump.add(spnMaxJumpPlanetaryAcquisitions);
        panMaxJump.add(new JLabel("Maximum number of jumps away to search for supplies"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panMaxJump, gridBagConstraints);

        DefaultComboBoxModel<String> factionLimitComboBoxModel = new DefaultComboBoxModel<>();
        factionLimitComboBoxModel.addElement(CampaignOptions.getFactionLimitName(CampaignOptions.PLANET_ACQUISITION_ALL));
        factionLimitComboBoxModel.addElement(CampaignOptions.getFactionLimitName(CampaignOptions.PLANET_ACQUISITION_NEUTRAL));
        factionLimitComboBoxModel.addElement(CampaignOptions.getFactionLimitName(CampaignOptions.PLANET_ACQUISITION_ALLY));
        factionLimitComboBoxModel.addElement(CampaignOptions.getFactionLimitName(CampaignOptions.PLANET_ACQUISITION_SELF));
        comboPlanetaryAcquisitionsFactionLimits.setModel(factionLimitComboBoxModel);
        comboPlanetaryAcquisitionsFactionLimits.setSelectedIndex(options.getPlanetAcquisitionFactionLimit());
        JPanel panFactionLimit = new JPanel();
        panFactionLimit.add(new JLabel("Faction supply limitations"));
        panFactionLimit.add(comboPlanetaryAcquisitionsFactionLimits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panFactionLimit, gridBagConstraints);

        disallowPlanetaryAcquisitionClanCrossover.setText(resourceMap.getString("disallowPlanetaryAcquisitionClanCrossover.text")); // NOI18N
        disallowPlanetaryAcquisitionClanCrossover.setToolTipText(resourceMap.getString("disallowPlanetaryAcquisitionClanCrossover.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowPlanetaryAcquisitionClanCrossover, gridBagConstraints);

        disallowClanPartsFromIS.setText(resourceMap.getString("disallowClanPartsFromIS.text")); // NOI18N
        disallowClanPartsFromIS.setToolTipText(resourceMap.getString("disallowClanPartsFromIS.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowClanPartsFromIS, gridBagConstraints);

        spnPenaltyClanPartsFromIS = new JSpinner(new SpinnerNumberModel(options.getPenaltyClanPartsFroIS(), 0, 12, 1));
        JPanel panPenaltyClanPartsFromIS = new JPanel();
        panPenaltyClanPartsFromIS.add(spnPenaltyClanPartsFromIS);
        JLabel lblPenaltyClanPartsFromIS = new JLabel(resourceMap.getString("spnPenaltyClanPartsFromIS.text"));
        lblPenaltyClanPartsFromIS.setToolTipText(resourceMap.getString("spnPenaltyClanPartsFromIS.toolTipText")); // NOI18N
        panPenaltyClanPartsFromIS.add(lblPenaltyClanPartsFromIS);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panPenaltyClanPartsFromIS, gridBagConstraints);

        usePlanetaryAcquisitionsVerbose.setText(resourceMap.getString("usePlanetaryAcquisitionsVerbose.text")); // NOI18N
        usePlanetaryAcquisitionsVerbose.setToolTipText(resourceMap.getString("usePlanetaryAcquisitionsVerbose.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitionsVerbose, gridBagConstraints);

        JPanel panSocioIndustrialBonus = new JPanel();
        panSocioIndustrialBonus.setLayout(new BoxLayout(panSocioIndustrialBonus, BoxLayout.LINE_AXIS));
        panSocioIndustrialBonus.setBorder(BorderFactory.createTitledBorder("Planet socio-industrial modifiers "));

        JPanel panTechBonus = new JPanel(new GridBagLayout());
        JPanel panIndustryBonus = new JPanel(new GridBagLayout());
        JPanel panOutputBonus = new JPanel(new GridBagLayout());

        spnPlanetAcquireTechBonus = new JSpinner[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        panTechBonus.add(new JLabel("<html><b>Tech<b></html>"), gridBagConstraints);
        panIndustryBonus.add(new JLabel("<html><b>Industry<b></html>"), gridBagConstraints);
        panOutputBonus.add(new JLabel("<html><b>Output<b></html>"), gridBagConstraints);
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            panTechBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panIndustryBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panOutputBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(options.getPlanetTechAcquisitionBonus(i), -12, 12, 1));
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(options.getPlanetIndustryAcquisitionBonus(i), -12, 12, 1));
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(options.getPlanetOutputAcquisitionBonus(i), -12, 12, 1));
            ((JSpinner.DefaultEditor) spnPlanetAcquireTechBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireIndustryBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireOutputBonus[i].getEditor()).getTextField().setEditable(false);

            panTechBonus.add(spnPlanetAcquireTechBonus[i], gridBagConstraints);
            panOutputBonus.add(spnPlanetAcquireOutputBonus[i], gridBagConstraints);
            panIndustryBonus.add(spnPlanetAcquireIndustryBonus[i], gridBagConstraints);

        }


        panSocioIndustrialBonus.add(panTechBonus);
        panSocioIndustrialBonus.add(panIndustryBonus);
        panSocioIndustrialBonus.add(panOutputBonus);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panSocioIndustrialBonus, gridBagConstraints);


        tabOptions.addTab(resourceMap.getString("panSupplies.TabConstraints.tabTitle"), panSupplies); // NOI18N

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

        JLabel lblTechLevel = new JLabel(resourceMap.getString("lblTechLevel.text")); // NOI18N
        lblTechLevel.setName("lblTechLevel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        DefaultComboBoxModel<String> techLevelComboBoxModel = new DefaultComboBoxModel<>();
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

        //region Personnel Tab
        panPersonnel.setName("panPersonnel");
        panPersonnel.setLayout(new java.awt.GridBagLayout());
        gridy = 0;

        useTacticsBox.setText(resourceMap.getString("useTacticsBox.text"));
        useTacticsBox.setToolTipText(resourceMap.getString("useTacticsBox.toolTipText"));
        useTacticsBox.setName("useTacticsBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panPersonnel.add(useTacticsBox, gridBagConstraints);

        useInitBonusBox.setText(resourceMap.getString("useInitBonusBox.text"));
        useInitBonusBox.setToolTipText(resourceMap.getString("useInitBonusBox.toolTipText"));
        useInitBonusBox.setName("useInitBonusBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useInitBonusBox, gridBagConstraints);

        useToughnessBox.setText(resourceMap.getString("useToughnessBox.text"));
        useToughnessBox.setToolTipText(resourceMap.getString("useToughnessBox.toolTipText"));
        useToughnessBox.setName("useToughnessBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useToughnessBox, gridBagConstraints);

        useArtilleryBox.setText(resourceMap.getString("useArtilleryBox.text"));
        useArtilleryBox.setToolTipText(resourceMap.getString("useArtilleryBox.toolTipText"));
        useArtilleryBox.setName("useArtilleryBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useArtilleryBox, gridBagConstraints);

        useAbilitiesBox.setText(resourceMap.getString("useAbilitiesBox.text"));
        useAbilitiesBox.setToolTipText(resourceMap.getString("useAbilitiesBox.toolTipText"));
        useAbilitiesBox.setName("useAbilitiesBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useAbilitiesBox, gridBagConstraints);

        useEdgeBox.setText(resourceMap.getString("useEdgeBox.text"));
        useEdgeBox.setToolTipText(resourceMap.getString("useEdgeBox.toolTipText"));
        useEdgeBox.setName("useEdgeBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useEdgeBox, gridBagConstraints);

        useSupportEdgeBox.setText(resourceMap.getString("useSupportEdgeBox.text"));
        useSupportEdgeBox.setToolTipText(resourceMap.getString("useSupportEdgeBox.toolTipText"));
        useSupportEdgeBox.setName("useSupportEdgeBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useSupportEdgeBox, gridBagConstraints);

        useImplantsBox.setText(resourceMap.getString("useImplantsBox.text"));
        useImplantsBox.setToolTipText(resourceMap.getString("useImplantsBox.toolTipText"));
        useImplantsBox.setName("useImplantsBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useImplantsBox, gridBagConstraints);

        chkCapturePrisoners.setText(resourceMap.getString("chkCapturePrisoners.text"));
        chkCapturePrisoners.setToolTipText(resourceMap.getString("chkCapturePrisoners.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkCapturePrisoners, gridBagConstraints);

        DefaultComboBoxModel<String> prisonerStatusModel = new DefaultComboBoxModel<>();
        prisonerStatusModel.addElement(resourceMap.getString("prisonerStatus.Prisoner"));
        prisonerStatusModel.addElement(resourceMap.getString("prisonerStatus.Bondsman"));
        comboPrisonerStatus = new JComboBox<>(prisonerStatusModel);
        comboPrisonerStatus.setSelectedIndex(options.getDefaultPrisonerStatus());
        JPanel pnlPrisonerStatus = new JPanel();
        pnlPrisonerStatus.add(comboPrisonerStatus);
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("prisonerStatus.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(pnlPrisonerStatus, gridBagConstraints);

        altQualityAveragingCheckBox.setText(resourceMap.getString("altQualityAveragingCheckBox.text"));
        altQualityAveragingCheckBox.setToolTipText(resourceMap.getString("altQualityAveragingCheckBox.toolTipText"));
        altQualityAveragingCheckBox.setName("altQualityAveragingCheckBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(altQualityAveragingCheckBox, gridBagConstraints);

        useAdvancedMedicalBox.setText(resourceMap.getString("useAdvancedMedicalBox.text"));
        useAdvancedMedicalBox.setToolTipText(resourceMap.getString("useAdvancedMedicalBox.toolTipText"));
        useAdvancedMedicalBox.setName("useAdvancedMedicalBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useAdvancedMedicalBox, gridBagConstraints);

        useDylansRandomXpBox.setText(resourceMap.getString("useDylansRandomXpBox.text"));
        useDylansRandomXpBox.setToolTipText(resourceMap.getString("useDylansRandomXpBox.toolTipText"));
        useDylansRandomXpBox.setName("useDylansRandomXpBox");
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useDylansRandomXpBox, gridBagConstraints);

        spnHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getHealingWaitingPeriod(), 1, 30, 1));
        ((JSpinner.DefaultEditor) spnHealWaitingPeriod.getEditor()).getTextField().setEditable(false);
        JPanel pnlHealWaitingPeriod = new JPanel();
        pnlHealWaitingPeriod.add(spnHealWaitingPeriod);
        pnlHealWaitingPeriod.add(new JLabel(resourceMap.getString("healWaitingPeriod.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(pnlHealWaitingPeriod, gridBagConstraints);

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(options.getNaturalHealingWaitingPeriod(),
                1, 365, 1));
        ((JSpinner.DefaultEditor) spnNaturalHealWaitingPeriod.getEditor()).getTextField().setEditable(false);
        JPanel pnlNaturalHealWaitingPeriod = new JPanel();
        pnlNaturalHealWaitingPeriod.add(spnNaturalHealWaitingPeriod);
        pnlNaturalHealWaitingPeriod.add(new JLabel(resourceMap.getString("naturalHealWaitingPeriod.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(pnlNaturalHealWaitingPeriod, gridBagConstraints);

        spnMinimumHitsForVees = new JSpinner(new SpinnerNumberModel(options.getMinimumHitsForVees(), 1, 5, 1));
        ((JSpinner.DefaultEditor) spnMinimumHitsForVees.getEditor()).getTextField().setEditable(false);
        JPanel panMinimumHitsForVees = new JPanel();
        panMinimumHitsForVees.add(spnMinimumHitsForVees);
        panMinimumHitsForVees.add(new JLabel(resourceMap.getString("minimumHitsForVees.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(panMinimumHitsForVees, gridBagConstraints);

        useRandomHitsForVees = new JCheckBox();
        useRandomHitsForVees.setSelected(options.useRandomHitsForVees());
        useRandomHitsForVees.setText(resourceMap.getString("useRandomHitsForVees.text"));
        useRandomHitsForVees.setToolTipText(resourceMap.getString("useRandomHitsForVees.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useRandomHitsForVees, gridBagConstraints);

        useTougherHealing = new JCheckBox(resourceMap.getString("useTougherHealing.text"));
        useTougherHealing.setSelected(options.useTougherHealing());
        useTougherHealing.setToolTipText(resourceMap.getString("useTougherHealing.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(useTougherHealing, gridBagConstraints);

        chkUseTransfers = new JCheckBox(resourceMap.getString("useTransfers.text"));
        chkUseTransfers.setSelected(options.useTransfers());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkUseTransfers, gridBagConstraints);

        chkUseTimeInService = new JCheckBox(resourceMap.getString("useTimeInService.text"));
        chkUseTimeInService.setSelected(options.getUseTimeInService());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkUseTimeInService, gridBagConstraints);

        DefaultComboBoxModel<TimeInDisplayFormat> timeInServiceDisplayFormatModel = new DefaultComboBoxModel<>(TimeInDisplayFormat.values());
        comboTimeInServiceDisplayFormat = new JComboBox<>(timeInServiceDisplayFormatModel);
        comboTimeInServiceDisplayFormat.setName("comboTimeInServiceDisplayFormat");
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        JPanel pnlTimeInServiceDisplayFormat = new JPanel();
        pnlTimeInServiceDisplayFormat.add(comboTimeInServiceDisplayFormat);
        pnlTimeInServiceDisplayFormat.add(new JLabel(resourceMap.getString("timeInServiceDisplayFormat.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(pnlTimeInServiceDisplayFormat, gridBagConstraints);

        chkUseTimeInRank = new JCheckBox(resourceMap.getString("useTimeInRank.text"));
        chkUseTimeInRank.setSelected(options.getUseTimeInRank());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkUseTimeInRank, gridBagConstraints);

        DefaultComboBoxModel<TimeInDisplayFormat> timeInRankDisplayFormatModel = new DefaultComboBoxModel<>(TimeInDisplayFormat.values());
        comboTimeInRankDisplayFormat = new JComboBox<>(timeInRankDisplayFormatModel);
        comboTimeInRankDisplayFormat.setName("comboTimeInRankDisplayFormat");
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        JPanel pnlTimeInRankDisplayFormat = new JPanel();
        pnlTimeInRankDisplayFormat.add(comboTimeInRankDisplayFormat);
        pnlTimeInRankDisplayFormat.add(new JLabel(resourceMap.getString("timeInRankDisplayFormat.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(pnlTimeInRankDisplayFormat, gridBagConstraints);

        chkTrackTotalEarnings = new JCheckBox(resourceMap.getString("trackTotalEarnings.text"));
        chkTrackTotalEarnings.setToolTipText("trackTotalEarnings.toolTipText");
        chkTrackTotalEarnings.setSelected(options.trackTotalEarnings());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkTrackTotalEarnings, gridBagConstraints);

        chkShowOriginFaction = new JCheckBox(resourceMap.getString("showOriginFaction.text"));
        chkShowOriginFaction.setSelected(options.showOriginFaction());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkShowOriginFaction, gridBagConstraints);

        chkRandomizeOrigin = new JCheckBox(resourceMap.getString("randomizeOrigin.text"));
        chkRandomizeOrigin.setSelected(options.randomizeOrigin());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkRandomizeOrigin, gridBagConstraints);

        chkRandomizeDependentsOrigin = new JCheckBox(resourceMap.getString("randomizeDependentsOrigin.text"));
        chkRandomizeDependentsOrigin.setSelected(options.getRandomizeDependentOrigin());
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(chkRandomizeDependentsOrigin, gridBagConstraints);

        spnOriginSearchRadius = new JSpinner(new SpinnerNumberModel(options.getOriginSearchRadius(), 10, 250, 10));
        JPanel panOriginSearchRadius = new JPanel();
        panOriginSearchRadius.add(spnOriginSearchRadius);
        panOriginSearchRadius.add(new JLabel(resourceMap.getString("originSearchRadius.text")));
        gridBagConstraints.gridy = ++gridy;
        panPersonnel.add(panOriginSearchRadius, gridBagConstraints);

        //Family
        JPanel panFamily = new JPanel(new GridBagLayout());
        panFamily.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("FamilyTab.text")));
        int panFamilyGridY = ++gridy;

        spnMinimumMarriageAge = new JSpinner(new SpinnerNumberModel(options.getMinimumMarriageAge(), 14, null, 1));
        JPanel panMinimumMarriageAge = new JPanel();
        panMinimumMarriageAge.add(spnMinimumMarriageAge);
        panMinimumMarriageAge.add(new JLabel(resourceMap.getString("minimumMarriageAge.text")));
        panMinimumMarriageAge.setToolTipText(resourceMap.getString("minimumMarriageAge.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panMinimumMarriageAge, gridBagConstraints);

        spnCheckMutualAncestorsDepth = new JSpinner(new SpinnerNumberModel(options.checkMutualAncestorsDepth(), 0, 20, 1));
        JPanel panCheckMutualAncestorsDepth = new JPanel();
        panCheckMutualAncestorsDepth.add(spnCheckMutualAncestorsDepth);
        panCheckMutualAncestorsDepth.add(new JLabel(resourceMap.getString("checkMutualAncestorsDepth.text")));
        panCheckMutualAncestorsDepth.setToolTipText(resourceMap.getString("checkMutualAncestorsDepth.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panCheckMutualAncestorsDepth, gridBagConstraints);

        chkLogMarriageNameChange = new JCheckBox(resourceMap.getString("logMarriageNameChange.text"));
        chkLogMarriageNameChange.setToolTipText(resourceMap.getString("logMarriageNameChange.toolTipText"));
        chkLogMarriageNameChange.setSelected(options.logMarriageNameChange());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkLogMarriageNameChange, gridBagConstraints);

        chkUseRandomMarriages = new JCheckBox(resourceMap.getString("useRandomMarriages.text"));
        chkUseRandomMarriages.setToolTipText(resourceMap.getString("useRandomMarriages.toolTipText"));
        chkUseRandomMarriages.setSelected(options.useRandomMarriages());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseRandomMarriages, gridBagConstraints);

        spnChanceRandomMarriages = new JSpinner(new SpinnerNumberModel(options.getChanceRandomMarriages() * 100.0, 0, 100, 0.001));
        JPanel panChanceRandomMarriages = new JPanel();
        panChanceRandomMarriages.add(spnChanceRandomMarriages);
        panChanceRandomMarriages.add(new JLabel(resourceMap.getString("chanceRandomMarriages.text")));
        panChanceRandomMarriages.setToolTipText(resourceMap.getString("chanceRandomMarriages.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panChanceRandomMarriages, gridBagConstraints);

        spnMarriageAgeRange = new JSpinner(new SpinnerNumberModel(options.getMarriageAgeRange(), 0, null, 1.0));
        spnMarriageAgeRange.setPreferredSize(new Dimension(50,
                spnMarriageAgeRange.getEditor().getPreferredSize().height + 5));
        JPanel panMarriageAgeRange = new JPanel();
        panMarriageAgeRange.add(spnMarriageAgeRange);
        panMarriageAgeRange.add(new JLabel(resourceMap.getString("marriageAgeRange.text")));
        panMarriageAgeRange.setToolTipText(resourceMap.getString("marriageAgeRange.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panMarriageAgeRange, gridBagConstraints);

        JPanel panRandomMarriageSurnameWeights = new JPanel(new GridLayout((int) Math.ceil(Person.NUM_SURNAME / 3.0), 3));
        panRandomMarriageSurnameWeights.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("randomMarriageSurnameWeights.text")));
        panRandomMarriageSurnameWeights.setToolTipText(resourceMap.getString("randomMarriageSurnameWeights.toolTipText"));
        spnRandomMarriageSurnameWeights = new JSpinner[Person.NUM_SURNAME];
        JSpinner spnRandomMarriageSurnameWeight;
        JPanel panRandomMarriageSurnameWeight;
        for (int i = 0; i < Person.NUM_SURNAME; i++) {
            spnRandomMarriageSurnameWeight = new JSpinner(new SpinnerNumberModel((options.getRandomMarriageSurnameWeights(i) / 10.0), 0, 100, 0.1));
            panRandomMarriageSurnameWeight = new JPanel();
            panRandomMarriageSurnameWeight.add(spnRandomMarriageSurnameWeight);
            panRandomMarriageSurnameWeight.add(new JLabel(Person.SURNAME_TYPE_NAMES[i]));
            panRandomMarriageSurnameWeights.add(panRandomMarriageSurnameWeight);
            spnRandomMarriageSurnameWeights[i] = spnRandomMarriageSurnameWeight;
        }
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panRandomMarriageSurnameWeights, gridBagConstraints);

        chkUseRandomSameSexMarriages = new JCheckBox(resourceMap.getString("useRandomSameSexMarriages.text"));
        chkUseRandomSameSexMarriages.setToolTipText(resourceMap.getString("useRandomSameSexMarriages.toolTipText"));
        chkUseRandomSameSexMarriages.setSelected(options.useRandomSameSexMarriages());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseRandomSameSexMarriages, gridBagConstraints);

        spnChanceRandomSameSexMarriages = new JSpinner(new SpinnerNumberModel(options.getChanceRandomSameSexMarriages() * 100.0, 0, 100, 0.001));
        JPanel panChanceRandomSameSexMarriages = new JPanel();
        panChanceRandomSameSexMarriages.add(spnChanceRandomSameSexMarriages);
        panChanceRandomSameSexMarriages.add(new JLabel(resourceMap.getString("chanceRandomSameSexMarriages.text")));
        panChanceRandomSameSexMarriages.setToolTipText(resourceMap.getString("chanceRandomSameSexMarriages.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panChanceRandomSameSexMarriages, gridBagConstraints);

        chkUseUnofficialProcreation = new JCheckBox(resourceMap.getString("useUnofficialProcreation.text"));
        chkUseUnofficialProcreation.setSelected(options.useUnofficialProcreation());
        chkUseUnofficialProcreation.setToolTipText(resourceMap.getString("useUnofficialProcreation.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseUnofficialProcreation, gridBagConstraints);

        spnChanceProcreation = new JSpinner(new SpinnerNumberModel(options.getChanceProcreation() * 100.0, 0, 100, 0.001));
        JPanel panChanceProcreation = new JPanel();
        panChanceProcreation.add(spnChanceProcreation);
        panChanceProcreation.add(new JLabel(resourceMap.getString("chanceProcreation.text")));
        panChanceProcreation.setToolTipText(resourceMap.getString("chanceProcreation.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panChanceProcreation, gridBagConstraints);

        chkUseUnofficialProcreationNoRelationship = new JCheckBox(resourceMap.getString("useUnofficialProcreationNoRelationship.text"));
        chkUseUnofficialProcreationNoRelationship.setToolTipText(resourceMap.getString("useUnofficialProcreationNoRelationship.toolTipText"));
        chkUseUnofficialProcreationNoRelationship.setSelected(options.useUnofficialProcreationNoRelationship());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseUnofficialProcreationNoRelationship, gridBagConstraints);

        spnChanceProcreationNoRelationship = new JSpinner(new SpinnerNumberModel(options.getChanceProcreationNoRelationship() * 100.0, 0, 100, 0.001));
        JPanel panChanceProcreationNoRelationship = new JPanel();
        panChanceProcreationNoRelationship.add(spnChanceProcreationNoRelationship);
        panChanceProcreationNoRelationship.add(new JLabel(resourceMap.getString("chanceProcreationNoRelationship.text")));
        panChanceProcreationNoRelationship.setToolTipText(resourceMap.getString("chanceProcreationNoRelationship.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(panChanceProcreationNoRelationship, gridBagConstraints);

        chkDisplayTrueDueDate = new JCheckBox(resourceMap.getString("displayTrueDueDate.text"));
        chkDisplayTrueDueDate.setToolTipText(resourceMap.getString("displayTrueDueDate.toolTipText"));
        chkDisplayTrueDueDate.setSelected(options.getDisplayTrueDueDate());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkDisplayTrueDueDate, gridBagConstraints);

        chkLogConception = new JCheckBox(resourceMap.getString("logConception.text"));
        chkLogConception.setSelected(options.logConception());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkLogConception, gridBagConstraints);

        DefaultComboBoxModel<String> babySurnameStyleModel = new DefaultComboBoxModel<>();
        babySurnameStyleModel.addElement(resourceMap.getString("babySurnameStyle.Mother"));
        babySurnameStyleModel.addElement(resourceMap.getString("babySurnameStyle.Father"));
        comboBabySurnameStyle = new JComboBox<>(babySurnameStyleModel);
        comboBabySurnameStyle.setSelectedIndex(options.getBabySurnameStyle());
        JPanel pnlBabySurnameStyle = new JPanel();
        pnlBabySurnameStyle.add(comboBabySurnameStyle);
        pnlBabySurnameStyle.add(new JLabel(resourceMap.getString("babySurnameStyle.text")));
        pnlBabySurnameStyle.setToolTipText(resourceMap.getString("babySurnameStyle.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(pnlBabySurnameStyle, gridBagConstraints);

        chkUseParentage = new JCheckBox(resourceMap.getString("useParentage.text"));
        chkUseParentage.setSelected(options.useParentage());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseParentage, gridBagConstraints);

        DefaultComboBoxModel<String> familyLevelStatusModel = new DefaultComboBoxModel<>();
        familyLevelStatusModel.addElement(resourceMap.getString("displayFamilyLevel.ParentsChildren"));
        familyLevelStatusModel.addElement(resourceMap.getString("displayFamilyLevel.GrandparentsGrandchildren"));
        familyLevelStatusModel.addElement(resourceMap.getString("displayFamilyLevel.AuntsUnclesCousins"));
        comboDisplayFamilyLevel = new JComboBox<>(familyLevelStatusModel);
        comboDisplayFamilyLevel.setSelectedIndex(options.displayFamilyLevel());
        JPanel pnlDisplayFamilyLevel = new JPanel();
        pnlDisplayFamilyLevel.add(comboDisplayFamilyLevel);
        pnlDisplayFamilyLevel.add(new JLabel(resourceMap.getString("displayFamilyLevel.text")));
        pnlDisplayFamilyLevel.setToolTipText(resourceMap.getString("displayFamilyLevel.toolTipText"));
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(pnlDisplayFamilyLevel, gridBagConstraints);

        chkUseRandomDeaths = new JCheckBox(resourceMap.getString("useRandomDeaths.text"));
        chkUseRandomDeaths.setToolTipText(resourceMap.getString("useRandomDeaths.toolTipText"));
        chkUseRandomDeaths.setSelected(options.useRandomDeaths());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkUseRandomDeaths, gridBagConstraints);

        chkKeepMarriedNameUponSpouseDeath = new JCheckBox(resourceMap.getString("keepMarriedNameUponSpouseDeath.text"));
        chkKeepMarriedNameUponSpouseDeath.setSelected(options.getKeepMarriedNameUponSpouseDeath());
        gridBagConstraints.gridy = ++gridy;
        panFamily.add(chkKeepMarriedNameUponSpouseDeath, gridBagConstraints);

        gridBagConstraints.gridy = panFamilyGridY;
        panPersonnel.add(panFamily, gridBagConstraints);

        //Salary
        JPanel panSalary = new JPanel(new GridBagLayout());
        panSalary.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("SalaryTab.text")));

        JPanel panMultiplier = new JPanel(new GridLayout(1, 3));
        panMultiplier.setBorder(BorderFactory.createTitledBorder("Multipliers"));
        spnSalaryCommission = new JSpinner(new SpinnerNumberModel(options.getSalaryCommissionMultiplier(), 0, 10, 0.05));
        ((JSpinner.DefaultEditor) spnSalaryCommission.getEditor()).getTextField().setEditable(false);
        JPanel panSalaryCommission = new JPanel();
        panSalaryCommission.add(spnSalaryCommission);
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
        JPanel panType;
        spnSalaryBase = new JSpinner[Person.T_NUM];
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 1; i < Person.T_NUM; i++) {
            panType = new JPanel(new GridBagLayout());

            gridBagConstraints.gridx = 0;
            gridBagConstraints.weightx = 1.0;
            panType.add(new JLabel(Person.getRoleDesc(i, false)), gridBagConstraints);

            JSpinner spnType = new JSpinner(new SpinnerNumberModel(options.getBaseSalary(i), 0d, null, 10d));
            spnType.setMinimumSize(new Dimension(75, 20));
            spnType.setPreferredSize(new Dimension(75, 25));
            spnSalaryBase[i] = spnType;
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 0.0;
            panType.add(spnType, gridBagConstraints);
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

        JScrollPane scrollPersonnel = new JScrollPane(panPersonnel);
        scrollPersonnel.setPreferredSize(new java.awt.Dimension(500, 400));

        tabOptions.addTab(resourceMap.getString("panPersonnel.TabConstraints.tabTitle"), scrollPersonnel);
        //endregion Personnel Tab

        //region Finances Tab
        panFinances.setName("panFinances"); // NOI18N
        panFinances.setLayout(new java.awt.GridBagLayout());
        gridy = 0;

        payForPartsBox.setText(resourceMap.getString("payForPartsBox.text")); // NOI18N
        payForPartsBox.setToolTipText(resourceMap.getString("payForPartsBox.toolTipText")); // NOI18N
        payForPartsBox.setName("payForPartsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForPartsBox, gridBagConstraints);

        payForRepairsBox.setText(resourceMap.getString("payForRepairsBox.text")); // NOI18N
        payForRepairsBox.setToolTipText(resourceMap.getString("payForRepairsBox.toolTipText")); // NOI18N
        payForRepairsBox.setName("payForRepairsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForRepairsBox, gridBagConstraints);

        payForUnitsBox.setText(resourceMap.getString("payForUnitsBox.text")); // NOI18N
        payForUnitsBox.setToolTipText(resourceMap.getString("payForUnitsBox.toolTipText")); // NOI18N
        payForUnitsBox.setName("payForUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForUnitsBox, gridBagConstraints);

        payForSalariesBox.setText(resourceMap.getString("payForSalariesBox.text")); // NOI18N
        payForSalariesBox.setToolTipText(resourceMap.getString("payForSalariesBox.toolTipText")); // NOI18N
        payForSalariesBox.setName("payForSalariesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForSalariesBox, gridBagConstraints);

        payForOverheadBox.setText(resourceMap.getString("payForOverheadBox.text")); // NOI18N
        payForOverheadBox.setToolTipText(resourceMap.getString("payForOverheadBox.toolTipText")); // NOI18N
        payForOverheadBox.setName("payForOverheadBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForOverheadBox, gridBagConstraints);

        payForMaintainBox.setText(resourceMap.getString("payForMaintainBox.text")); // NOI18N
        payForMaintainBox.setToolTipText(resourceMap.getString("payForMaintainBox.toolTipText")); // NOI18N
        payForMaintainBox.setName("payForMaintainBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForMaintainBox, gridBagConstraints);

        payForTransportBox.setText(resourceMap.getString("payForTransportBox.text")); // NOI18N
        payForTransportBox.setToolTipText(resourceMap.getString("payForTransportBox.toolTipText")); // NOI18N
        payForTransportBox.setName("payForTransportBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForTransportBox, gridBagConstraints);

        sellUnitsBox.setText(resourceMap.getString("sellUnitsBox.text")); // NOI18N
        sellUnitsBox.setToolTipText(resourceMap.getString("sellUnitsBox.toolTipText")); // NOI18N
        sellUnitsBox.setName("sellUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellUnitsBox, gridBagConstraints);

        sellPartsBox.setText(resourceMap.getString("sellPartsBox.text")); // NOI18N
        sellPartsBox.setToolTipText(resourceMap.getString("sellPartsBox.toolTipText")); // NOI18N
        sellPartsBox.setName("sellPartsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellPartsBox, gridBagConstraints);

        payForRecruitmentBox.setText(resourceMap.getString("payForRecruitmentBox.text")); // NOI18N
        payForRecruitmentBox.setToolTipText(resourceMap.getString("payForRecruitmentBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForRecruitmentBox, gridBagConstraints);

        useLoanLimitsBox.setText(resourceMap.getString("useLoanLimitsBox.text")); // NOI18N
        useLoanLimitsBox.setToolTipText(resourceMap.getString("useLoanLimitsBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useLoanLimitsBox, gridBagConstraints);

        // Unofficial maintenance costs
        usePercentageMaintBox = new JCheckBox(resourceMap.getString("usePercentageMaintBox.text")); // NOI18N
        usePercentageMaintBox.setToolTipText(resourceMap.getString("usePercentageMaintBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePercentageMaintBox, gridBagConstraints);

        // Unofficial infantry don't count for contract pay
        useInfantryDontCountBox = new JCheckBox(resourceMap.getString("infantryDontCount.text")); // NOI18N
        useInfantryDontCountBox.setToolTipText(resourceMap.getString("infantryDontCount.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
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
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePeacetimeCostBox, gridBagConstraints);

        useExtendedPartsModifierBox.setText(resourceMap.getString("useExtendedPartsModifierBox.text")); // NOI18N
        useExtendedPartsModifierBox.setName("useExtendedPartsModifierBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useExtendedPartsModifierBox, gridBagConstraints);

        showPeacetimeCostBox.setText(resourceMap.getString("showPeacetimeCostBox.text")); // NOI18N
        showPeacetimeCostBox.setToolTipText(resourceMap.getString("showPeacetimeCostBox.toolTipText")); // NOI18N
        showPeacetimeCostBox.setName("showPeacetimeCostBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(showPeacetimeCostBox, gridBagConstraints);

        DefaultComboBoxModel<FinancialYearDuration> financialYearDurationModel = new DefaultComboBoxModel<>(FinancialYearDuration.values());
        comboFinancialYearDuration = new JComboBox<>(financialYearDurationModel);
        comboFinancialYearDuration.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (isSelected && (index > -1)) {
                        list.setToolTipText((list.getSelectedValue() instanceof FinancialYearDuration)
                                ? ((FinancialYearDuration) list.getSelectedValue()).getToolTipText() : "");
                    }

                    return this;
                }
            });
        comboFinancialYearDuration.setSelectedItem(options.getFinancialYearDuration());
        JPanel pnlFinancialYearDuration = new JPanel();
        pnlFinancialYearDuration.add(new JLabel(resourceMap.getString("financialYearDuration.text")));
        pnlFinancialYearDuration.setToolTipText(resourceMap.getString("financialYearDuration.toolTipText"));
        pnlFinancialYearDuration.add(comboFinancialYearDuration);
        gridBagConstraints.gridy = gridy++;
        panFinances.add(pnlFinancialYearDuration, gridBagConstraints);

        newFinancialYearFinancesToCSVExportBox = new JCheckBox(resourceMap.getString("newFinancialYearFinancesToCSVExportBox.text"));
        newFinancialYearFinancesToCSVExportBox.setToolTipText(resourceMap.getString("newFinancialYearFinancesToCSVExportBox.toolTipText"));
        newFinancialYearFinancesToCSVExportBox.setName("newFinancialYearFinancesToCSVExportBox");
        newFinancialYearFinancesToCSVExportBox.setSelected(options.getNewFinancialYearFinancesToCSVExport());
        gridBagConstraints.gridy = gridy++;
        panFinances.add(newFinancialYearFinancesToCSVExportBox, gridBagConstraints);

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
        //endregion Finances Tab

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

        spnEquipPercent = new JSpinner(new SpinnerNumberModel(options.getEquipmentContractPercent(), 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1));
        spnEquipPercent.setEditor(new JSpinner.NumberEditor(spnEquipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnEquipPercent.getEditor()).getTextField().setEditable(false);

        spnDropshipPercent = new JSpinner(new SpinnerNumberModel(options.getDropshipContractPercent(), 0.0, CampaignOptions.MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnDropshipPercent.setEditor(new JSpinner.NumberEditor(spnDropshipPercent, "0.0"));
        ((JSpinner.NumberEditor) spnDropshipPercent.getEditor()).getTextField().setEditable(false);

        spnJumpshipPercent = new JSpinner(new SpinnerNumberModel(options.getJumpshipContractPercent(), 0.0, CampaignOptions.MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnJumpshipPercent.setEditor(new JSpinner.NumberEditor(spnJumpshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnJumpshipPercent.getEditor()).getTextField().setEditable(false);

        spnWarshipPercent = new JSpinner(new SpinnerNumberModel(options.getWarshipContractPercent(), 0.0, CampaignOptions.MAXIMUM_WARSHIP_EQUIPMENT_PERCENT, 0.1));
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
        tempSPA = new Hashtable<>();
        for(String name : spaNames) {
        	tempSPA.put(name, SpecialAbility.getAbility(name).clone());
        }

        panXP.setName("panXP"); // NOI18N
        panXP.setLayout(new java.awt.GridBagLayout());

        JLabel lblScenarioXP = new JLabel(resourceMap.getString("lblScenarioXP.text"));
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

        JLabel lblKillXP = new JLabel(resourceMap.getString("lblKillXP.text"));
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

        JLabel lblKills = new JLabel(resourceMap.getString("lblKills.text"));
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

        JLabel lblTaskXP = new JLabel(resourceMap.getString("lblKillXP.text"));
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

        JLabel lblTasks = new JLabel(resourceMap.getString("lblTasks.text"));
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

        JLabel lblSuccessXp = new JLabel(resourceMap.getString("lblSuccessXP.text"));
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

        JLabel lblMistakeXP = new JLabel(resourceMap.getString("lblMistakeXP.text"));
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
        btnAddSPA.addActionListener(evt -> btnAddSPA());

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
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("6-9"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("10-11"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]));
        panRollTable.add(new JLabel("1"));
        panRollTable.add(new JLabel("12 or more"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]));
        panRollTable.add(new JLabel("2"));
        panRollTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2d6 + Bonus"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel lblOverallRecruitBonus = new JLabel(resourceMap.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resourceMap.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resourceMap.getString("chkExtraRandom.toolTipText"));
        chkExtraRandom.setSelected(rSkillPrefs.randomizeSkill());
        chkClanBonus = new JCheckBox(resourceMap.getString("chkClanBonus.text"));
        chkClanBonus.setToolTipText(resourceMap.getString("chkClanBonus.toolTipText"));
        chkClanBonus.setSelected(rSkillPrefs.useClanBonuses());
        JLabel lblProbAntiMek = new JLabel(resourceMap.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getAntiMekProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getOverallRecruitBonus(), -12, 12, 1));
        ((JSpinner.DefaultEditor) spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus.setToolTipText(resourceMap.getString("spnOverallRecruitBonus.toolTipText"));
        spnTypeRecruitBonus = new JSpinner[Person.T_NUM];
        int nRow = (int) Math.ceil(Person.T_NUM / 4.0);
        JPanel panTypeRecruitBonus = new JPanel(new GridLayout(nRow, 4));
        JSpinner spin;
        JPanel panRecruit;
        for (int i = 0; i < Person.T_NUM; i++) {
            panRecruit = new JPanel(new GridBagLayout());
            spin = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getRecruitBonus(i), -12, 12, 1));
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
        spnArtyProb = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getArtilleryProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resourceMap.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getArtilleryBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSecondSkillProb(), 0, 100, 5));
        ((JSpinner.DefaultEditor) spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resourceMap.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSecondSkillBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Skills"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panTactics = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getTacticsMod(SkillType.EXP_GREEN), -10,
                                                              10, 1));
        ((JSpinner.DefaultEditor) spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resourceMap.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getTacticsMod(SkillType.EXP_REGULAR), -10,
                                                            10, 1));
        ((JSpinner.DefaultEditor) spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resourceMap.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getTacticsMod(SkillType.EXP_VETERAN), -10,
                                                            10, 1));
        ((JSpinner.DefaultEditor) spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resourceMap.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getTacticsMod(SkillType.EXP_ELITE), -10,
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
        spnCombatSA = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getCombatSmallArmsBonus(), -10, 10, 1));
        ((JSpinner.DefaultEditor) spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resourceMap.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSupportSmallArmsBonus(), -10, 10, 1));
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
        spnAbilGreen = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_GREEN), -10,
                                                           10, 1));
        ((JSpinner.DefaultEditor) spnAbilGreen.getEditor()).getTextField().setEditable(false);
        spnAbilGreen.setToolTipText(resourceMap.getString("spnAbilGreen.toolTipText"));
        spnAbilReg = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_REGULAR), -10,
                                                         10, 1));
        ((JSpinner.DefaultEditor) spnAbilReg.getEditor()).getTextField().setEditable(false);
        spnAbilReg.setToolTipText(resourceMap.getString("spnAbilReg.toolTipText"));
        spnAbilVet = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_VETERAN), -10,
                                                         10, 1));
        ((JSpinner.DefaultEditor) spnAbilVet.getEditor()).getTextField().setEditable(false);
        spnAbilVet.setToolTipText(resourceMap.getString("spnAbilVet.toolTipText"));
        spnAbilElite = new JSpinner(new SpinnerNumberModel(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_ELITE), -10,
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

        JLabel lblRank = new JLabel(resourceMap.getString("lblRank.text")); // NOI18N
        lblRank.setName("lblRank"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRank.add(lblRank, gridBagConstraints);

        DefaultComboBoxModel<String> rankModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < Ranks.RS_NUM; i++) {
            rankModel.addElement(Ranks.getRankSystemName(i));
        }
        comboRanks.setModel(rankModel);
        comboRanks.setSelectedIndex(campaign.getRanks().getRankSystem());
        comboRanks.setName("comboRanks"); // NOI18N
        comboRanks.setActionCommand("fillRanks");
        comboRanks.addActionListener(evt -> {
            if (evt.getActionCommand().equals("fillRanks"))
                fillRankInfo();
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
        TableColumn column;
        for (int i = 0; i < RankTableModel.COL_NUM; i++) {
            column = tableRanks.getColumnModel().getColumn(i);
            column.setPreferredWidth(ranksModel.getColumnWidth(i));
            column.setCellRenderer(ranksModel.getRenderer());
            if (i == RankTableModel.COL_PAYMULT) {
            	column.setCellEditor(new SpinnerEditor());
            }
        }
        tableRanks.getSelectionModel().addListSelectionListener(
                this::tableRanksValueChanged);
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
        useFactionForNamesBox.addActionListener(this::useFactionForNamesBoxEvent);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridy = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(useFactionForNamesBox, gridBagConstraints);


        JLabel lblFactionNames = new JLabel(resourceMap.getString("lblFactionNames.text")); // NOI18N
        lblFactionNames.setName("lblFactionNames"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(lblFactionNames, gridBagConstraints);

        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<>();
        for (Iterator<String> i = campaign.getRNG().getFactions(); i.hasNext(); ) {
            String faction = i.next();
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

        JLabel lblGender = new JLabel(resourceMap.getString("lblGender.text")); // NOI18N
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

        panRandomPortrait.setName("panRandomPortrait"); // NOI18N
        panRandomPortrait.setLayout(new BorderLayout());

        // The math below is used to determine how to split the personnel role options for portraits,
        // which it does into 4 columns with rows equal to the number of roles plus two, with the
        // additional two being the all role and no role options.
        JPanel panUsePortrait = new JPanel(new GridLayout((int) Math.ceil((Person.T_NUM + 2) / 4.0), 4));
        chkUsePortrait = new JCheckBox[Person.T_NUM];
        JCheckBox allPortraitsBox = new JCheckBox(resourceMap.getString("panUsePortrait.all.text"));
        JCheckBox noPortraitsBox = new JCheckBox(resourceMap.getString("panUsePortrait.no.text"));
        allPortraitsBox.addActionListener(evt -> {
            final boolean selected = allPortraitsBox.isSelected();
            for (JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(true);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                noPortraitsBox.setSelected(false);
            }
        });
        noPortraitsBox.addActionListener(evt -> {
            final boolean selected = noPortraitsBox.isSelected();
            for (JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(false);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                allPortraitsBox.setSelected(false);
            }
        });
        panUsePortrait.add(allPortraitsBox);
        panUsePortrait.add(noPortraitsBox);

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
                BorderFactory.createTitledBorder(resourceMap.getString("panRandomPortrait.title")),
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
        personnelMarketType = new JComboBox<>();
        personnelMarketReportRefresh = new JCheckBox("Display a report when market refreshes");
        personnelMarketRandomEliteRemoval = new JTextField();
        personnelMarketRandomVeteranRemoval = new JTextField();
        personnelMarketRandomRegularRemoval = new JTextField();
        personnelMarketRandomGreenRemoval = new JTextField();
        personnelMarketRandomUltraGreenRemoval = new JTextField();
        personnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(options.getPersonnelMarketDylansWeight(),
                                                                          0.1, 0.8, 0.1));
        JLabel personnelMarketTypeLabel = new JLabel("Market Type:");
        JLabel personnelMarketRandomEliteRemovalLabel = new JLabel("Random & Dylan's Elite Removal");
        JLabel personnelMarketRandomVeteranRemovalLabel = new JLabel("Random & Dylan's Veteran Removal");
        JLabel personnelMarketRandomRegularRemovalLabel = new JLabel("Random & Dylan's Regular Removal");
        JLabel personnelMarketRandomGreenRemovalLabel = new JLabel("Random & Dylan's Green Removal");
        JLabel personnelMarketRandomUltraGreenRemovalLabel = new JLabel("Random & Dylan's Ultra-Green Removal");
        JLabel personnelMarketDylansWeightLabel = new JLabel("<html>Weight for Dylan's Method to choose most"
                                                      + "<br />common unit type based on your forces</html>");
        personnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        for (PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance().getAllServices(true)) {
            personnelMarketType.addItem(method.getModuleName());
        }
        personnelMarketType.setSelectedItem(options.getPersonnelMarketType());
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

        personnelMarketType.setSelectedItem(options.getPersonnelMarketType());
        personnelMarketType.addActionListener(evt -> {
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

        cbSkillLevel = new JComboBox<>();
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
        spnOpforAeroChance = new JSpinner();
        spnOpforLocalForceChance = new JSpinner();

        availableRats = new JList<>();
        chosenRats = new JList<>();
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

        // TODO : Switch me to use a modified RandomSkillsGenerator.levelNames
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ULTRA_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]);
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
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesExcludeLargeCraft, gridBagConstraints);

        chkSharesForAll.setText(resourceMap.getString("chkSharesForAll.text"));
        chkSharesForAll.setToolTipText(resourceMap.getString("chkSharesForAll.toolTipText"));
        chkSharesForAll.setSelected(options.getSharesForAll());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesForAll, gridBagConstraints);

        chkAeroRecruitsHaveUnits.setText(resourceMap.getString("chkAeroRecruitsHaveUnits.text"));
        chkAeroRecruitsHaveUnits.setToolTipText(resourceMap.getString("chkAeroRecruitsHaveUnits.toolTipText"));
        chkAeroRecruitsHaveUnits.setSelected(options.getAeroRecruitsHaveUnits());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAeroRecruitsHaveUnits, gridBagConstraints);

        chkRetirementRolls.setText(resourceMap.getString("chkRetirementRolls.text"));
        chkRetirementRolls.setToolTipText(resourceMap.getString("chkRetirementRolls.toolTipText"));
        chkRetirementRolls.setSelected(options.doRetirementRolls());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkRetirementRolls, gridBagConstraints);

        chkCustomRetirementMods.setText(resourceMap.getString("chkCustomRetirementMods.text"));
        chkCustomRetirementMods.setToolTipText(resourceMap.getString("chkCustomRetirementMods.toolTipText"));
        chkCustomRetirementMods.setSelected(options.getCustomRetirementMods());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkCustomRetirementMods, gridBagConstraints);

        chkFoundersNeverRetire.setText(resourceMap.getString("chkFoundersNeverRetire.text"));
        chkFoundersNeverRetire.setToolTipText(resourceMap.getString("chkFoundersNeverRetire.toolTipText"));
        chkFoundersNeverRetire.setSelected(options.getFoundersNeverRetire());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkFoundersNeverRetire, gridBagConstraints);

        chkAddDependents = new JCheckBox(resourceMap.getString("chkAddDependents.text"));
        chkAddDependents.setToolTipText(resourceMap.getString("chkAddDependents.toolTipText"));
        chkAddDependents.setSelected(options.canAtBAddDependents());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAddDependents, gridBagConstraints);

        chkDependentsNeverLeave = new JCheckBox(resourceMap.getString("chkDependentsNeverLeave.text"));
        chkDependentsNeverLeave.setToolTipText(resourceMap.getString("chkDependentsNeverLeave.toolTipText"));
        chkDependentsNeverLeave.setSelected(options.getDependentsNeverLeave());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkDependentsNeverLeave, gridBagConstraints);

        chkTrackUnitFatigue.setText(resourceMap.getString("chkTrackUnitFatigue.text"));
        chkTrackUnitFatigue.setToolTipText(resourceMap.getString("chkTrackUnitFatigue.toolTipText"));
        chkTrackUnitFatigue.setSelected(options.getTrackUnitFatigue());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackUnitFatigue, gridBagConstraints);

        chkUseLeadership.setText(resourceMap.getString("chkUseLeadership.text"));
        chkUseLeadership.setToolTipText(resourceMap.getString("chkUseLeadership.toolTipText"));
        chkUseLeadership.setSelected(options.getUseLeadership());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseLeadership, gridBagConstraints);

        chkTrackOriginalUnit.setText(resourceMap.getString("chkTrackOriginalUnit.text"));
        chkTrackOriginalUnit.setToolTipText(resourceMap.getString("chkTrackOriginalUnit.toolTipText"));
        chkTrackOriginalUnit.setSelected(options.getTrackOriginalUnit());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackOriginalUnit, gridBagConstraints);

        chkUseAero.setText(resourceMap.getString("chkUseAero.text"));
        chkUseAero.setToolTipText(resourceMap.getString("chkUseAero.toolTipText"));
        chkUseAero.setSelected(options.getUseAero());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseAero, gridBagConstraints);

        chkUseVehicles.setText(resourceMap.getString("chkUseVehicles.text"));
        chkUseVehicles.setToolTipText(resourceMap.getString("chkUseVehicles.toolTipText"));
        chkUseVehicles.setSelected(options.getUseVehicles());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseVehicles, gridBagConstraints);

        chkClanVehicles.setText(resourceMap.getString("chkClanVehicles.text"));
        chkClanVehicles.setToolTipText(resourceMap.getString("chkClanVehicles.toolTipText"));
        chkClanVehicles.setSelected(options.getUseVehicles());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkClanVehicles, gridBagConstraints);

        chkInstantUnitMarketDelivery.setText(resourceMap.getString("chkInstantUnitMarketDelivery.text"));
        chkInstantUnitMarketDelivery.setToolTipText(resourceMap.getString("chkInstantUnitMarketDelivery.toolTipText"));
        chkInstantUnitMarketDelivery.setSelected(options.getInstantUnitMarketDelivery());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkInstantUnitMarketDelivery, gridBagConstraints);

        chkContractMarketReportRefresh.setText(resourceMap.getString("chkContractMarketReportRefresh.text"));
        chkContractMarketReportRefresh.setSelected(options.getContractMarketReportRefresh());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkContractMarketReportRefresh, gridBagConstraints);

        chkUnitMarketReportRefresh.setText(resourceMap.getString("chkUnitMarketReportRefresh.text"));
        chkUnitMarketReportRefresh.setSelected(options.getUnitMarketReportRefresh());
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUnitMarketReportRefresh, gridBagConstraints);

        ButtonGroup group = new ButtonGroup();
        group.add(btnDynamicRATs);
        group.add(btnStaticRATs);

        chosenRatModel = new DefaultListModel<>();
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
        chosenRats.addListSelectionListener(arg0 -> {
            btnRemoveRat.setEnabled(chosenRats.getSelectedIndex() >= 0);
            btnMoveRatUp.setEnabled(chosenRats.getSelectedIndex() > 0);
            btnMoveRatDown.setEnabled(chosenRatModel.size() > chosenRats.getSelectedIndex() + 1);
        });
        availableRatModel = new DefaultListModel<>();
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
        availableRats.addListSelectionListener(arg0 -> btnAddRat.setEnabled(availableRats.getSelectedIndex() >= 0));

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
        btnAddRat.addActionListener(arg0 -> {
            int selectedIndex = availableRats.getSelectedIndex();
            chosenRatModel.addElement(availableRats.getSelectedValue());
            availableRatModel.removeElementAt(availableRats.getSelectedIndex());
            availableRats.setSelectedIndex(Math.min(selectedIndex, availableRatModel.size() - 1));
        });
        btnAddRat.setEnabled(false);
        panRatButtons.add(btnAddRat);
        btnRemoveRat.setText(resourceMap.getString("btnRemoveRat.text"));
        btnRemoveRat.setToolTipText(resourceMap.getString("btnRemoveRat.toolTipText"));
        btnRemoveRat.addActionListener(arg0 -> {
            int selectedIndex = chosenRats.getSelectedIndex();
            availableRatModel.addElement(chosenRats.getSelectedValue());
            chosenRatModel.removeElementAt(chosenRats.getSelectedIndex());
            chosenRats.setSelectedIndex(Math.min(selectedIndex, chosenRatModel.size() - 1));
        });
        btnRemoveRat.setEnabled(false);
        panRatButtons.add(btnRemoveRat);
        btnMoveRatUp.setText(resourceMap.getString("btnMoveRatUp.text"));
        btnMoveRatUp.setToolTipText(resourceMap.getString("btnMoveRatUp.toolTipText"));
        btnMoveRatUp.addActionListener(arg0 -> {
            int selectedIndex = chosenRats.getSelectedIndex();
            String tmp = chosenRatModel.getElementAt(selectedIndex);
            chosenRatModel.setElementAt(chosenRatModel.getElementAt(selectedIndex - 1), selectedIndex);
            chosenRatModel.setElementAt(tmp, selectedIndex - 1);
            chosenRats.setSelectedIndex(selectedIndex - 1);
        });
        btnMoveRatUp.setEnabled(false);
        panRatButtons.add(btnMoveRatUp);
        btnMoveRatDown.setText(resourceMap.getString("btnMoveRatDown.text"));
        btnMoveRatDown.setToolTipText(resourceMap.getString("btnMoveRatDown.toolTipText"));
        btnMoveRatDown.addActionListener(arg0 -> {
            int selectedIndex = chosenRats.getSelectedIndex();
            String tmp = chosenRatModel.getElementAt(selectedIndex);
            chosenRatModel.setElementAt(chosenRatModel.getElementAt(selectedIndex + 1), selectedIndex);
            chosenRatModel.setElementAt(tmp, selectedIndex + 1);
            chosenRats.setSelectedIndex(selectedIndex + 1);
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

        spnIntensity.setModel(new SpinnerNumberModel(options.getIntensity(), 0.0, 5.0, 0.1));
        spnIntensity.setToolTipText(resourceMap.getString("spnIntensity.toolTipText"));
        spnIntensity.setValue(options.getIntensity());
        spnIntensity.setMinimumSize(new Dimension(60, 25));
        spnIntensity.setPreferredSize(new Dimension(60, 25));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(spnIntensity, gridBagConstraints);
        spnIntensity.addChangeListener(arg0 -> updateBattleChances());

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
        JLabel lblOpforAeroChance = new JLabel(resourceMap.getString("lblOpforAeroLikelihood.text"));
        lblOpforAeroChance.setToolTipText(resourceMap.getString("lblOpforAeroLikelihood.toolTipText"));
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
        JLabel lblOpforLocalForceChance = new JLabel(resourceMap.getString("lblOpforAeroLikelihood.text"));
        lblOpforLocalForceChance.setToolTipText(resourceMap.getString("lblOpforLocalForceLikelihood.toolTipText"));
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

        javax.swing.SwingUtilities.invokeLater(() -> {
            scrSPA.getVerticalScrollBar().setValue(0);
            scrAtB.getVerticalScrollBar().setValue(0);
        });

        JScrollPane scrMisc = new JScrollPane(panMisc);
        scrMisc.setPreferredSize(new java.awt.Dimension(500, 400));

        chkHistoricalDailyLog.setText(resourceMap.getString("chkShowHistoricalDailyReport.text")); // NOI18N
        chkHistoricalDailyLog.setToolTipText(resourceMap.getString("chkShowHistoricalDailyReport.toolTipText")); // NOI18N
        chkHistoricalDailyLog.setName("chkHistoricalDailyLog"); // NOI18N
        chkHistoricalDailyLog.setSelected(options.historicalDailyLog());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panMisc.add(chkHistoricalDailyLog, gridBagConstraints);

        tabOptions.addTab(resourceMap.getString("misc.TabConstraints.tabTitle"), scrMisc);

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
        btnOkay.addActionListener(evt -> btnOkayActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnOkay, gridBagConstraints);

        btnSave.setText(resourceMap.getString("btnSave.text")); // NOI18N
        btnSave.setName("btnSave"); // NOI18N
        btnSave.addActionListener(evt -> btnSaveActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnSave, gridBagConstraints);

        btnLoad.setText(resourceMap.getString("btnLoad.text")); // NOI18N
        btnLoad.setName("btnLoad"); // NOI18N
        btnLoad.addActionListener(evt -> btnLoadActionPerformed());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnLoad, gridBagConstraints);


        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(this::btnCancelActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.25;
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CampaignOptionsDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

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
            String nextFaction = i.next();
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
        TableColumn column;
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
				// TODO: it would be nice if we could just update the choices in this dialog now
				// TODO: rather than closing it, but that is currently not possible given how
				// TODO: this dialog is set up
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
        Optional<File> maybeFile = FileDialogs.saveCampaignOptions(null);

        if (!maybeFile.isPresent()) {
            return;
        }

        File file = maybeFile.get();

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
        GamePreset preset = new GamePreset(gpdd.getTitle(), gpdd.getDesc(), options, rSkillPrefs, SkillType.lookupHash, SpecialAbility.getAllSpecialAbilities());

        // Then save it out to that file.
        FileOutputStream fos;
        PrintWriter pw;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
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
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
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
    	campaign.setCalendar(date);
        // Ensure that the MegaMek year GameOption matches the campaign year
        GameOptions gameOpts = campaign.getGameOptions();
        int campaignYear = campaign.getGameYear();
        if (gameOpts.intOption("year") != campaignYear) {
            gameOpts.getOption("year").setValue(campaignYear);
        }
        campaign.setFactionCode(Faction.getFactionFromFullNameAndYear
        		(String.valueOf(comboFaction.getSelectedItem()), date.get(Calendar.YEAR)).getShortName());
        if (null != comboFactionNames.getSelectedItem()) {
            campaign.getRNG().setChosenFaction((String) comboFactionNames.getSelectedItem());
        }
        campaign.getRNG().setPercentFemale(sldGender.getValue());
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
        options.setQuirks(useQuirksBox.isSelected());
        campaign.getGameOptions().getOption("stratops_quirks").setValue(useQuirksBox.isSelected());
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
        options.setUseAeroSystemHits(useAeroSystemHitsBox.isSelected());
        options.setCheckMaintenance(checkMaintenance.isSelected());
        options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
        options.setReverseQualityNames(reverseQualityNames.isSelected());
        options.setUseUnofficialMaintenance(useUnofficialMaintenance.isSelected());
        options.setLogMaintenance(logMaintenance.isSelected());
        options.setMaintenanceBonus((Integer) spnMaintenanceBonus.getModel().getValue());
        options.setMaintenanceCycleDays((Integer) spnMaintenanceDays.getModel().getValue());
        options.setPayForParts(payForPartsBox.isSelected());
        options.setPayForRepairs(payForRepairsBox.isSelected());
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
        options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExportBox.isSelected());
        options.setFinancialYearDuration((FinancialYearDuration) comboFinancialYearDuration.getSelectedItem());
        options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());

        options.setEquipmentContractBase(btnContractEquipment.isSelected());
        options.setEquipmentContractPercent((Double) spnEquipPercent.getModel().getValue());
        options.setDropshipContractPercent((Double) spnDropshipPercent.getModel().getValue());
        options.setJumpshipContractPercent((Double) spnJumpshipPercent.getModel().getValue());
        options.setWarshipContractPercent((Double) spnWarshipPercent.getModel().getValue());
        options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
        options.setBLCSaleValue(chkBLCSaleValue.isSelected());

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
        options.setPlanetaryAcquisition(usePlanetaryAcquisitions.isSelected());
        options.setDisallowClanPartsFromIS(disallowClanPartsFromIS.isSelected());
        options.setPlanetAcquisitionVerboseReporting(usePlanetaryAcquisitionsVerbose.isSelected());
        options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
        options.setMaxJumpsPlanetaryAcquisition((int)spnMaxJumpPlanetaryAcquisitions.getModel().getValue());
        options.setPenaltyClanPartsFroIS((int)spnPenaltyClanPartsFromIS.getModel().getValue());
        options.setPlanetAcquisitionFactionLimit(comboPlanetaryAcquisitionsFactionLimits.getSelectedIndex());
        for (int i = ITechnology.RATING_A; i <= ITechnology.RATING_F; i++) {
            options.setPlanetTechAcquisitionBonus((int)spnPlanetAcquireTechBonus[i].getModel().getValue(), i);
            options.setPlanetIndustryAcquisitionBonus((int)spnPlanetAcquireIndustryBonus[i].getModel().getValue(), i);
            options.setPlanetOutputAcquisitionBonus((int)spnPlanetAcquireOutputBonus[i].getModel().getValue(), i);

        }

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
        options.setFactionIntroDate(factionIntroDateBox.isSelected());
        campaign.updateTechFactionCode();
        options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
        options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
        options.setTechLevel(choiceTechLevel.getSelectedIndex());
        campaign.getGameOptions().getOption("techlevel").setValue((String)choiceTechLevel.getSelectedItem());

        rSkillPrefs.setOverallRecruitBonus((Integer) spnOverallRecruitBonus.getModel().getValue());
        for (int i = 0; i < Person.T_NUM; i++) {
            rSkillPrefs.setRecruitBonus(i, (Integer) spnTypeRecruitBonus[i].getModel().getValue());
        }
        rSkillPrefs.setRandomizeSkill(chkExtraRandom.isSelected());
        rSkillPrefs.setAntiMekProb((Integer) spnProbAntiMek.getModel().getValue());
        rSkillPrefs.setArtilleryProb((Integer) spnArtyProb.getModel().getValue());
        rSkillPrefs.setArtilleryBonus((Integer) spnArtyBonus.getModel().getValue());
        rSkillPrefs.setSecondSkillProb((Integer) spnSecondProb.getModel().getValue());
        rSkillPrefs.setSecondSkillBonus((Integer) spnSecondBonus.getModel().getValue());
        rSkillPrefs.setTacticsMod(SkillType.EXP_GREEN, (Integer) spnTacticsGreen.getModel().getValue());
        rSkillPrefs.setTacticsMod(SkillType.EXP_REGULAR, (Integer) spnTacticsReg.getModel().getValue());
        rSkillPrefs.setTacticsMod(SkillType.EXP_VETERAN, (Integer) spnTacticsVet.getModel().getValue());
        rSkillPrefs.setTacticsMod(SkillType.EXP_ELITE, (Integer) spnTacticsElite.getModel().getValue());
        rSkillPrefs.setCombatSmallArmsBonus((Integer) spnCombatSA.getModel().getValue());
        rSkillPrefs.setSupportSmallArmsBonus((Integer) spnSupportSA.getModel().getValue());
        rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_GREEN, (Integer) spnAbilGreen.getModel().getValue());
        rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_REGULAR, (Integer) spnAbilReg.getModel().getValue());
        rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_VETERAN, (Integer) spnAbilVet.getModel().getValue());
        rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_ELITE, (Integer) spnAbilElite.getModel().getValue());

        options.setProbPhenoMW((Integer) spnProbPhenoMW.getModel().getValue());
        options.setProbPhenoAero((Integer) spnProbPhenoAero.getModel().getValue());
        options.setProbPhenoBA((Integer) spnProbPhenoBA.getModel().getValue());
        options.setProbPhenoVee((Integer) spnProbPhenoVee.getModel().getValue());

        //region Personnel Tab

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
        campaign.getGameOptions().getOption("manei_domini").setValue(useImplantsBox.isSelected());
        options.setCapturePrisoners(chkCapturePrisoners.isSelected());
        options.setDefaultPrisonerStatus(comboPrisonerStatus.getSelectedIndex());
        options.setAltQualityAveraging(altQualityAveragingCheckBox.isSelected());
        options.setAdvancedMedical(useAdvancedMedicalBox.isSelected());
        options.setDylansRandomXp(useDylansRandomXpBox.isSelected());
        //we need to reset healing time options through the campaign because we may need to
        //loop through personnel to make adjustments
        campaign.setHealingTimeOptions((Integer) spnHealWaitingPeriod.getModel().getValue(),
                (Integer) spnNaturalHealWaitingPeriod.getModel().getValue());
        options.setMinimumHitsForVees((Integer) spnMinimumHitsForVees.getModel().getValue());
        options.setUseRandomHitsForVees(useRandomHitsForVees.isSelected());
        options.setTougherHealing(useTougherHealing.isSelected());
        options.setUseTransfers(chkUseTransfers.isSelected());
        options.setUseTimeInService(chkUseTimeInService.isSelected());
        options.setTimeInServiceDisplayFormat((TimeInDisplayFormat) comboTimeInServiceDisplayFormat.getSelectedItem());
        options.setUseTimeInRank(chkUseTimeInRank.isSelected());
        options.setTimeInRankDisplayFormat((TimeInDisplayFormat) comboTimeInRankDisplayFormat.getSelectedItem());
        options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
        options.setShowOriginFaction(chkShowOriginFaction.isSelected());
        options.setRandomizeOrigin(chkRandomizeOrigin.isSelected());
        options.setRandomizeDependentOrigin(chkRandomizeDependentsOrigin.isSelected());
        options.setOriginSearchRadius((Integer)spnOriginSearchRadius.getModel().getValue());
        //Family
        options.setMinimumMarriageAge((Integer) spnMinimumMarriageAge.getModel().getValue());
        options.setCheckMutualAncestorsDepth((Integer) spnCheckMutualAncestorsDepth.getModel().getValue());
        options.setLogMarriageNameChange(chkLogMarriageNameChange.isSelected());
        options.setUseRandomMarriages(chkUseRandomMarriages.isSelected());
        options.setChanceRandomMarriages((Double) spnChanceRandomMarriages.getModel().getValue() / 100.0);
        options.setMarriageAgeRange((Integer) spnMarriageAgeRange.getModel().getValue());
        for (int i = 0; i < Person.NUM_SURNAME; i++) {
            int val = (int) Math.round(((Double) spnRandomMarriageSurnameWeights[i].getModel().getValue()) * 10);
            options.setRandomMarriageSurnameWeight(i, val);
        }
        options.setUseRandomSameSexMarriages(chkUseRandomSameSexMarriages.isSelected());
        options.setChanceRandomSameSexMarriages((Double) spnChanceRandomSameSexMarriages.getModel().getValue() / 100.0);
        options.setUseUnofficialProcreation(chkUseUnofficialProcreation.isSelected());
        options.setChanceProcreation((Double) spnChanceProcreation.getModel().getValue() / 100.0);
        options.setUseUnofficialProcreationNoRelationship(chkUseUnofficialProcreationNoRelationship.isSelected());
        options.setChanceProcreationNoRelationship((Double) spnChanceProcreationNoRelationship.getModel().getValue() / 100.0);
        options.setDisplayTrueDueDate(chkDisplayTrueDueDate.isSelected());
        options.setLogConception(chkLogConception.isSelected());
        options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedIndex());
        options.setUseParentage(chkUseParentage.isSelected());
        options.setDisplayFamilyLevel(comboDisplayFamilyLevel.getSelectedIndex());
        options.setUseRandomDeaths(chkUseRandomDeaths.isSelected());
        options.setKeepMarriedNameUponSpouseDeath(chkKeepMarriedNameUponSpouseDeath.isSelected());
        //Salary
        options.setSalaryCommissionMultiplier((Double) spnSalaryCommission.getModel().getValue());
        options.setSalaryEnlistedMultiplier((Double) spnSalaryEnlisted.getModel().getValue());
        options.setSalaryAntiMekMultiplier((Double) spnSalaryAntiMek.getModel().getValue());
        for (int i = 0; i < spnSalaryXp.length; i++) {
            options.setSalaryXpMultiplier((Double) spnSalaryXp[i].getModel().getValue(), i);
        }
        for (int i = 1; i < Person.T_NUM; i++) {
            try {
                options.setBaseSalary(i, (double) spnSalaryBase[i].getValue());
            } catch (Exception ignored) { }
        }
        //endregion Personnel Tab

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
        options.setPersonnelMarketType((String) personnelMarketType.getSelectedItem());
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
        options.setAtBAddDependents(chkAddDependents.isSelected());
        options.setDependentsNeverLeave(chkDependentsNeverLeave.isSelected());
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

        //region Miscellaneous Tab
        options.setHistoricalDailyLog(chkHistoricalDailyLog.isSelected());
        //endregion Miscellaneous Tab

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
            factionModel = new SortedComboBoxModel<>();
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
    	Vector<String> unused = new Vector<>();
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
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(!getUnusedSPA().isEmpty());

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

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
        if (intensity >= AtBContract.MINIMUM_INTENSITY) {
            lblFightPct.setText((int)(40.0 * intensity / (40.0 * intensity + 60.0) * 100.0 + 0.5) + "%");
            lblDefendPct.setText((int)(20.0 * intensity / (20.0 * intensity + 80.0) * 100.0 + 0.5) + "%");
            lblScoutPct.setText((int)(60.0 * intensity / (60.0 * intensity + 40.0) * 100.0 + 0.5) + "%");
            lblTrainingPct.setText((int)(10.0 * intensity / (10.0 * intensity + 90.0) * 100.0 + 0.5) + "%");
        } else {
            lblFightPct.setText("Disabled");
            lblDefendPct.setText("Disabled");
            lblScoutPct.setText("Disabled");
            lblTrainingPct.setText("Disabled");
        }
    }

    /*
     * Taken from:
     *  http://tips4java.wordpress.com/2008/11/18/row-number-table/
     *	Use a JTable as a renderer for row numbers of a given main table.
     *  This table must be added to the row header of the scrollpane that
     *  contains the main table.
     */
    public static class RowNamesTable extends JTable
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
        private static class RowNumberRenderer extends DefaultTableCellRenderer {
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
                    SwingUtilities.invokeLater(() -> {
                        if (valueSet) {
                            textField.setCaretPosition(1);
                        }
                    });
                }

                @Override
                public void focusLost(FocusEvent fe) {
                }
            });
            textField.addActionListener(ae -> stopCellEditing());
        }

        // Prepares the spinner component and returns it.
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            if (!valueSet) {
                spinner.setValue(value);
            }
            SwingUtilities.invokeLater(() -> textField.requestFocus());
            return spinner;
        }

        @Override
        public boolean isCellEditable(EventObject eo) {
            if (eo instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) eo;
                textField.setText(String.valueOf(ke.getKeyChar()));
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
            try {
                editor.commitEdit();
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                JOptionPane.showMessageDialog(null, "Invalid value, discarding.");
            }
            return super.stopCellEditing();
        }
    }
}
