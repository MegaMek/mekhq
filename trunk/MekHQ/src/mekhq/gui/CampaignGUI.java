/*
 * CampaignGUI.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.AmmoType;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Jumpship;
import megamek.common.MULParser;
import megamek.common.Mech;
import megamek.common.MechView;
import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.PilotOptions;
import megameklab.com.util.UnitPrintManager;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.JumpPath;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingFactory;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.RatingReport;
import mekhq.campaign.report.Report;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.campaign.work.Modes;
import mekhq.gui.adapter.FinanceTableMouseAdapter;
import mekhq.gui.adapter.LoanTableMouseAdapter;
import mekhq.gui.adapter.OrgTreeMouseAdapter;
import mekhq.gui.adapter.PartsTableMouseAdapter;
import mekhq.gui.adapter.PersonnelTableMouseAdapter;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.adapter.ScenarioTableMouseAdapter;
import mekhq.gui.adapter.ServicedUnitsTableMouseAdapter;
import mekhq.gui.adapter.UnitTableMouseAdapter;
import mekhq.gui.dialog.AddFundsDialog;
import mekhq.gui.dialog.AdvanceDaysDialog;
import mekhq.gui.dialog.BloodnameDialog;
import mekhq.gui.dialog.CampaignOptionsDialog;
import mekhq.gui.dialog.ChooseMulFilesDialog;
import mekhq.gui.dialog.CompleteMissionDialog;
import mekhq.gui.dialog.ContractMarketDialog;
import mekhq.gui.dialog.CustomizeAtBContractDialog;
import mekhq.gui.dialog.CustomizeMissionDialog;
import mekhq.gui.dialog.CustomizeScenarioDialog;
import mekhq.gui.dialog.DailyReportLogDialog;
import mekhq.gui.dialog.DataLoadingDialog;
import mekhq.gui.dialog.GMToolsDialog;
import mekhq.gui.dialog.GameOptionsDialog;
import mekhq.gui.dialog.HireBulkPersonnelDialog;
import mekhq.gui.dialog.MaintenanceReportDialog;
import mekhq.gui.dialog.ManageAssetsDialog;
import mekhq.gui.dialog.MekHQAboutBox;
import mekhq.gui.dialog.MercRosterDialog;
import mekhq.gui.dialog.MissionTypeDialog;
import mekhq.gui.dialog.NewLoanDialog;
import mekhq.gui.dialog.NewRecruitDialog;
import mekhq.gui.dialog.NewsReportDialog;
import mekhq.gui.dialog.PartsStoreDialog;
import mekhq.gui.dialog.PersonnelMarketDialog;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.dialog.RefitNameDialog;
import mekhq.gui.dialog.ReportDialog;
import mekhq.gui.dialog.ResolveScenarioWizardDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.dialog.UnitMarketDialog;
import mekhq.gui.dialog.UnitSelectorDialog;
import mekhq.gui.handler.OrgTreeTransferHandler;
import mekhq.gui.model.AcquisitionTableModel;
import mekhq.gui.model.DocTableModel;
import mekhq.gui.model.FinanceTableModel;
import mekhq.gui.model.LoanTableModel;
import mekhq.gui.model.OrgTreeModel;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.model.PatientTableModel;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.model.ScenarioTableModel;
import mekhq.gui.model.TaskTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.sorter.PartsDetailSorter;
import mekhq.gui.sorter.RankSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.gui.sorter.TaskSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.gui.sorter.WeightClassSorter;
import mekhq.gui.view.AtBContractViewPanel;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.ContractViewPanel;
import mekhq.gui.view.ForceViewPanel;
import mekhq.gui.view.JumpPathViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.gui.view.PlanetViewPanel;
import mekhq.gui.view.ScenarioViewPanel;
import mekhq.gui.view.UnitViewPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import chat.ChatClient;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -687162569841072579L;

    public static final int UNIT_VIEW_WIDTH = 450;
    public static final int PERSONNEL_VIEW_WIDTH = 500;
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;

    // personnel filter groups
    public static final int PG_ACTIVE = 0;
    public static final int PG_COMBAT = 1;
    public static final int PG_SUPPORT = 2;
    public static final int PG_MW = 3;
    public static final int PG_CREW = 4;
    public static final int PG_PILOT = 5;
    public static final int PG_CPILOT = 6;
    public static final int PG_PROTO = 7;
    public static final int PG_BA = 8;
    public static final int PG_SOLDIER = 9;
    public static final int PG_VESSEL = 10;
    public static final int PG_TECH = 11;
    public static final int PG_DOC = 12;
    public static final int PG_ADMIN = 13;
    public static final int PG_RETIRE = 14;
    public static final int PG_MIA = 15;
    public static final int PG_KIA = 16;
    public static final int PG_NUM = 17;

    // parts filter groups
    private static final int SG_ALL = 0;
    private static final int SG_ARMOR = 1;
    private static final int SG_SYSTEM = 2;
    private static final int SG_EQUIP = 3;
    private static final int SG_LOC = 4;
    private static final int SG_WEAP = 5;
    private static final int SG_AMMO = 6;
    private static final int SG_AMMO_BIN = 7;
    private static final int SG_MISC = 8;
    private static final int SG_ENGINE = 9;
    private static final int SG_GYRO = 10;
    private static final int SG_ACT = 11;
    private static final int SG_NUM = 12;

    // parts views
    private static final int SV_ALL = 0;
    private static final int SV_IN_TRANSIT = 1;
    private static final int SV_RESERVED = 2;
    private static final int SV_UNDAMAGED = 3;
    private static final int SV_DAMAGED = 4;
    private static final int SV_NUM = 5;

    // personnel views
    private static final int PV_GRAPHIC = 0;
    private static final int PV_GENERAL = 1;
    private static final int PV_PILOT = 2;
    private static final int PV_INF = 3;
    private static final int PV_TACTIC = 4;
    private static final int PV_TECH = 5;
    private static final int PV_ADMIN = 6;
    private static final int PV_FLUFF = 7;
    private static final int PV_NUM = 8;

    // unit views
    private static final int UV_GRAPHIC = 0;
    private static final int UV_GENERAL = 1;
    private static final int UV_DETAILS = 2;
    private static final int UV_STATUS = 3;
    private static final int UV_NUM = 4;

    private JFrame frame;

    private MekHQ app;

    private ResourceBundle resourceMap;

    /* for the main panel */
    private JSplitPane mainPanel;
    private JTabbedPane tabMain;
    private DailyReportLogPanel panLog;

    /* For the menu bar */
    private JMenuBar menuBar;
    private JMenu menuThemes;
    private JMenuItem miDetachLog;
    private JMenuItem miAttachLog;
    private JMenuItem miContractMarket;
    private JMenuItem miUnitMarket;
    private JMenuItem miRetirementDefectionDialog;
    private JCheckBoxMenuItem miShowOverview;

    /* For the TO&E tab */
    private JPanel panOrganization;
    private JTree orgTree;
    private JSplitPane splitOrg;
    private JScrollPane scrollForceView;

    /* For the briefing room tab */
    private JPanel panBriefing;
    private JPanel panScenario;
    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JSplitPane splitBrief;
    private JSplitPane splitMission;
    private JTable scenarioTable;
    private JComboBox<String> choiceMission;
    private JScrollPane scrollMissionView;
    private JScrollPane scrollScenarioView;
    private JPanel panMissionButtons;
    private JPanel panScenarioButtons;
    private JButton btnAddScenario;
    private JButton btnAddMission;
    private JButton btnEditMission;
    private JButton btnCompleteMission;
    private JButton btnDeleteMission;
    private JButton btnStartGame;
    private JButton btnJoinGame;
    private JButton btnLoadGame;
    private JButton btnPrintRS;
    private JButton btnGetMul;
    private JButton btnClearAssignedUnits;
    private JButton btnResolveScenario;

    /* For the map tab */
    private JPanel panMapView;
    InterstellarMapPanel panMap;
    private JSplitPane splitMap;
    private JScrollPane scrollPlanetView;
    private JSuggestField suggestPlanet;

    /* For the personnel tab */
    private JPanel panPersonnel;
    private JSplitPane splitPersonnel;
    private JTable personnelTable;
    private JComboBox<String> choicePerson;
    private JComboBox<String> choicePersonView;
    private JScrollPane scrollPersonnelView;

    /* For the hangar tab */
    private JPanel panHangar;
    private JSplitPane splitUnit;
    private JTable unitTable;
    private JTable acquireUnitsTable;
    private JComboBox<String> choiceUnit;
    private JComboBox<String> choiceUnitView;
    private JScrollPane scrollUnitView;

    /* For the warehouse tab */
    private JPanel panSupplies;
    private JSplitPane splitWarehouse;
    private JTable partsTable;
    private JTable acquirePartsTable;
    private JTable whTechTable;
    private JButton btnDoTaskWarehouse;
    private JToggleButton btnShowAllTechsWarehouse;
    private JLabel lblTargetNumWarehouse;
    private JTextArea textTargetWarehouse;
    private JLabel astechPoolLabelWarehouse;
    private JComboBox<String> choiceParts;
    private JComboBox<String> choicePartsView;

    /* For the repair bay tab */
    private JPanel panRepairBay;
    private JPanel panDoTask;
    private JTabbedPane tabTasks;
    private JSplitPane splitServicedUnits;
    private JTable servicedUnitTable;
    private JTable taskTable;
    private JTable acquisitionTable;
    private JTable techTable;
    private JButton btnDoTask;
    private JButton btnUseBonusPart;
    private JToggleButton btnShowAllTechs;
    private JScrollPane scrTextTarget;
    private JScrollPane scrollPartsTable;
    private JLabel lblTargetNum;
    private JTextPane txtServicedUnitView;
    private JTextArea textTarget;
    private JLabel astechPoolLabel;
    private JComboBox<String> choiceLocation;

    /* For the infirmary tab */
    private JPanel panInfirmary;
    private JTable docTable;
    private JButton btnAssignDoc;
    private JButton btnUnassignDoc;
    private JList<Person> listAssignedPatient;
    private JList<Person> listUnassignedPatient;

    /* For the mek lab tab */
    private MekLabPanel panMekLab;

    /* For the finances tab */
    private JPanel panFinances;
    private JTable financeTable;
    private JTable loanTable;
    private JTextArea areaNetWorth;
    private JButton btnAddFunds;
    private JButton btnManageAssets;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblLocation;
    private JLabel lblRating;
    private JLabel lblFunds;
    private JLabel lblTempAstechs;
    private JLabel lblTempMedics;
    @SuppressWarnings("unused")
    private JLabel lblCargo; // FIXME: Re-add this in an optionized form

    /* for the top button panel */
    private JPanel btnPanel;
    private JToggleButton btnGMMode;
    private JToggleButton btnOvertime;
    private JButton btnAdvanceDay;

    /* Table models that we will need */
    private TaskTableModel taskModel;
    private AcquisitionTableModel acquireModel;
    // private ServicedUnitTableModel servicedUnitModel;
    private UnitTableModel servicedUnitModel;
    private TechTableModel techsModel;
    private PatientTableModel assignedPatientModel;
    private PatientTableModel unassignedPatientModel;
    private DocTableModel doctorsModel;
    private PersonnelTableModel personModel;
    private UnitTableModel unitModel;
    private PartsTableModel partsModel;
    private ProcurementTableModel acquirePartsModel;
    private ProcurementTableModel acquireUnitsModel;
    private FinanceTableModel financeModel;
    private LoanTableModel loanModel;
    private ScenarioTableModel scenarioModel;
    private OrgTreeModel orgModel;

    /* table sorters for tables that can be filtered */
    private TableRowSorter<PersonnelTableModel> personnelSorter;
    private TableRowSorter<PartsTableModel> partsSorter;
    private TableRowSorter<ProcurementTableModel> acquirePartsSorter;
    private TableRowSorter<UnitTableModel> unitSorter;
    private TableRowSorter<UnitTableModel> servicedUnitSorter;
    private TableRowSorter<TaskTableModel> taskSorter;
    private TableRowSorter<TechTableModel> techSorter;
    private TableRowSorter<TechTableModel> whTechSorter;

    // Start Overview Tab
    private JPanel panOverview;
    private JTabbedPane tabOverview;
    // Overview Parts In Use
    private JScrollPane scrollOverviewParts;
    private JPanel overviewPartsPanel;
    // Overview Transport
    private JScrollPane scrollOverviewTransport;
    // Overview Personnel
    private JScrollPane scrollOverviewCombatPersonnel;
    private JScrollPane scrollOverviewSupportPersonnel;
    private JSplitPane splitOverviewPersonnel;
    // Overview Hangar
    private JScrollPane scrollOverviewHangar;
    private JTextArea overviewHangarArea;
    private JSplitPane splitOverviewHangar;
    // Overview Rating
    private JScrollPane scrollOverviewUnitRating;
    private IUnitRating rating;
    // Overview Cargo
    private JScrollPane scrollOverviewCargo;
    // End Overview Tab

    ReportHyperlinkListener reportHLL;

    public int selectedMission;

    private DailyReportLogDialog logDialog;
    private GMToolsDialog gmTools;
    private AdvanceDaysDialog advanceDaysDialog;
    private BloodnameDialog bloodnameDialog;

    public CampaignGUI(MekHQ app) {
        this.app = app;
        reportHLL = new ReportHyperlinkListener(this);
        selectedMission = -1;
        initComponents();
    }

    public void showAboutBox() {
        MekHQAboutBox aboutBox = new MekHQAboutBox(getFrame());
        aboutBox.setLocationRelativeTo(getFrame());
        aboutBox.setVisible(true);
    }

    private void showDailyReportDialog() {
        mainPanel.remove(panLog);
        miDetachLog.setEnabled(false);
        miAttachLog.setEnabled(true);
        mainPanel.setOneTouchExpandable(false);
        logDialog.setVisible(true);
        refreshReport();
        this.revalidate();
        this.repaint();
    }

    public void hideDailyReportDialog() {
        logDialog.setVisible(false);
        mainPanel.setRightComponent(panLog);
        mainPanel.setOneTouchExpandable(true);
        miDetachLog.setEnabled(true);
        miAttachLog.setEnabled(false);
        this.revalidate();
        this.repaint();
    }

    public void showRetirementDefectionDialog() {
        /*
         * if there are unresolved personnel, show the results view; otherwise,
         * present the retirement view to give the player a chance to follow a
         * custom schedule
         */
        RetirementDefectionDialog rdd = new RetirementDefectionDialog(this,
                null, getCampaign().getRetirementDefectionTracker()
                        .getRetirees().size() == 0);
        rdd.setVisible(true);
        if (!rdd.wasAborted()) {
            getCampaign().applyRetirement(rdd.totalPayout(),
                    rdd.getUnitAssignments());
        }
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshRating();
    }

    public void toggleOverviewTab() {
        getTabOverview().setVisible(!getTabOverview().isVisible());
        miShowOverview.setSelected(getTabOverview().isVisible());
        showHideTabOverview();
    }

    public void showHideTabOverview() {
        miShowOverview.setSelected(tabOverview.isVisible());
        int drIndex = getTabMain().indexOfComponent(panOverview);
        if (drIndex > -1 && !tabOverview.isVisible()) {
            getTabMain().removeTabAt(drIndex);
        } else {
            if (drIndex == -1) {
                getTabMain().addTab(resourceMap.getString("panOverview.TabConstraints.tabTitle"),
                        panOverview);
            }
        }
    }

    public void showGMToolsDialog() {
        gmTools.setVisible(true);
    }

    public void showAdvanceDaysDialog() {
        advanceDaysDialog = new AdvanceDaysDialog(getFrame(), this, reportHLL);
        advanceDaysDialog.setVisible(true);
        advanceDaysDialog.dispose();
    }

    public void randomizeAllBloodnames() {
        for (Person p : getCampaign().getPersonnel()) {
            if (!p.isClanner()) {
                continue;
            }
            getCampaign().checkBloodnameAdd(p, p.getPrimaryRole());
            getCampaign().personUpdated(p);
        }
        refreshPatientList();
        refreshDoctorsList();
        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshOrganization();
    }

    public void showBloodnameDialog() {
        bloodnameDialog.setFaction(getCampaign().getFactionCode());
        bloodnameDialog.setYear(getCampaign().getCalendar().get(
                java.util.Calendar.YEAR));
        bloodnameDialog.setVisible(true);
    }

    private void initComponents() {

        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI");

        frame = new JFrame("MekHQ"); //$NON-NLS-1$
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabMain = new JTabbedPane();
        tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
        tabMain.setMinimumSize(new java.awt.Dimension(600, 200));
        tabMain.setPreferredSize(new java.awt.Dimension(900, 300));

        initToeTab();
        tabMain.addTab(resourceMap
                .getString("panOrganization.TabConstraints.tabTitle"),
                panOrganization); // NOI18N

        initBriefingTab();
        tabMain.addTab(
                resourceMap.getString("panBriefing.TabConstraints.tabTitle"),
                splitBrief); // NOI18N

        initMap();
        tabMain.addTab(resourceMap.getString("panMap.TabConstraints.tabTitle"),
                splitMap); // NOI18N

        initPersonnelTab();
        tabMain.addTab(
                resourceMap.getString("panPersonnel.TabConstraints.tabTitle"),
                panPersonnel); // NOI18N

        initHangarTab();
        tabMain.addTab(
                resourceMap.getString("panHangar.TabConstraints.tabTitle"),
                panHangar); // NOI18N

        initWarehouseTab();
        tabMain.addTab(
                resourceMap.getString("panSupplies.TabConstraints.tabTitle"),
                splitWarehouse); // NOI18N

        initRepairTab();
        tabMain.addTab(
                resourceMap.getString("panRepairBay.TabConstraints.tabTitle"),
                panRepairBay); // NOI18N

        initInfirmaryTab();
        tabMain.addTab(
                resourceMap.getString("panInfirmary.TabConstraints.tabTitle"),
                panInfirmary); // NOI18N

        panMekLab = new MekLabPanel(this);
        tabMain.addTab(
                resourceMap.getString("panMekLab.TabConstraints.tabTitle"),
                new JScrollPane(getPanMekLab())); // NOI18N

        initFinanceTab();
        tabMain.addTab(
                resourceMap.getString("panFinances.TabConstraints.tabTitle"),
                panFinances); // NOI18N

        initOverviewTab();
        tabMain.addTab(
                resourceMap.getString("panOverview.TabConstraints.tabTitle"),
                panOverview); // NOI18N

        initMain();
        initTopButtons();
        initStatusBar();

        setLayout(new BorderLayout());

        add(mainPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.PAGE_START);
        add(statusPanel, BorderLayout.PAGE_END);

        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        changePersonnelView();
        refreshTaskList();
        refreshAcquireList();
        refreshTechsList();
        refreshPatientList();
        refreshDoctorsList();
        refreshPartsList();
        refreshCalendar();
        refreshReport();
        refreshFunds();
        refreshRating();
        refreshFinancialTransactions();
        refreshOrganization();
        refreshMissions();
        refreshLocation();
        refreshTempAstechs();
        refreshTempMedics();
        refreshScenarioList();
        refreshOverview();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(Math.min(MAX_START_WIDTH, dim.width),
                Math.min(MAX_START_HEIGHT, dim.height));

        // Determine the new location of the window
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the window
        frame.setLocation(x, y);

        initMenu();
        frame.setJMenuBar(menuBar);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();

        if (isMacOSX()) {
            enableFullScreenMode(frame);
        }

        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                getApplication().exit();
            }
        });

        mainPanel.setDividerLocation(0.75);
    }

    private void initToeTab() {
        GridBagConstraints gridBagConstraints;

        panOrganization = new JPanel(new GridBagLayout());

        orgModel = new OrgTreeModel(getCampaign());
        orgTree = new JTree(orgModel);
        orgTree.addMouseListener(new OrgTreeMouseAdapter(this));
        orgTree.setCellRenderer(new ForceRenderer(getIconPackage()));
        orgTree.setRowHeight(60);
        orgTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        orgTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                refreshForceView();
            }
        });
        orgTree.setDragEnabled(true);
        orgTree.setDropMode(DropMode.ON);
        orgTree.setTransferHandler(new OrgTreeTransferHandler(this));

        scrollForceView = new JScrollPane();
        scrollForceView.setMinimumSize(new java.awt.Dimension(550, 600));
        scrollForceView.setPreferredSize(new java.awt.Dimension(550, 600));
        scrollForceView
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        splitOrg = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(orgTree), scrollForceView);
        splitOrg.setOneTouchExpandable(true);
        splitOrg.setResizeWeight(1.0);
        splitOrg.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        // this can mess up the unit view panel so refresh it
                        refreshForceView();
                    }
                });
        gridBagConstraints = new java.awt.GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panOrganization.add(splitOrg, gridBagConstraints);

    }

    private void initBriefingTab() {
        GridBagConstraints gridBagConstraints;

        panBriefing = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(new JLabel(resourceMap.getString("lblMission.text")),
                gridBagConstraints);

        choiceMission = new JComboBox<String>();
        choiceMission.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeMission();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(choiceMission, gridBagConstraints);

        panMissionButtons = new JPanel(new GridLayout(2, 3));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(panMissionButtons, gridBagConstraints);

        btnAddMission = new JButton(resourceMap.getString("btnAddMission.text")); // NOI18N
        btnAddMission.setToolTipText(resourceMap
                .getString("btnAddMission.toolTipText")); // NOI18N
        btnAddMission.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMission();
            }
        });
        panMissionButtons.add(btnAddMission);

        btnAddScenario = new JButton(
                resourceMap.getString("btnAddScenario.text")); // NOI18N
        btnAddScenario.setToolTipText(resourceMap
                .getString("btnAddScenario.toolTipText")); // NOI18N
        btnAddScenario.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addScenario();
            }
        });
        panMissionButtons.add(btnAddScenario);

        btnEditMission = new JButton(
                resourceMap.getString("btnEditMission.text")); // NOI18N
        btnEditMission.setToolTipText(resourceMap
                .getString("btnEditMission.toolTipText")); // NOI18N
        btnEditMission.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editMission();
            }
        });
        panMissionButtons.add(btnEditMission);

        btnCompleteMission = new JButton(
                resourceMap.getString("btnCompleteMission.text")); // NOI18N
        btnCompleteMission.setToolTipText(resourceMap
                .getString("btnCompleteMission.toolTipText")); // NOI18N
        btnCompleteMission.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                completeMission();
            }
        });
        panMissionButtons.add(btnCompleteMission);

        btnDeleteMission = new JButton(
                resourceMap.getString("btnDeleteMission.text")); // NOI18N
        btnDeleteMission.setToolTipText(resourceMap
                .getString("btnDeleteMission.toolTipText")); // NOI18N
        btnDeleteMission.setName("btnDeleteMission"); // NOI18N
        btnDeleteMission.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMission();
            }
        });
        panMissionButtons.add(btnDeleteMission);

        scrollMissionView = new JScrollPane();
        scrollMissionView
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMissionView.setViewportView(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panBriefing.add(scrollMissionView, gridBagConstraints);

        scenarioModel = new ScenarioTableModel(getCampaign());
        scenarioTable = new JTable(scenarioModel);
        scenarioTable.setShowGrid(false);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.addMouseListener(new ScenarioTableMouseAdapter(this));
        scenarioTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        scenarioTable.setIntercellSpacing(new Dimension(0, 0));
        scenarioTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        refreshScenarioView();
                    }
                });
        JScrollPane scrollScenarioTable = new JScrollPane(scenarioTable);
        scrollScenarioTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollScenarioTable.setPreferredSize(new java.awt.Dimension(200, 200));

        splitMission = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panBriefing, scrollScenarioTable);
        splitMission.setOneTouchExpandable(true);
        splitMission.setResizeWeight(1.0);

        panScenario = new JPanel(new GridBagLayout());

        panScenarioButtons = new JPanel(new GridLayout(3, 3));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panScenario.add(panScenarioButtons, gridBagConstraints);

        btnStartGame = new JButton(resourceMap.getString("btnStartGame.text")); // NOI18N
        btnStartGame.setToolTipText(resourceMap
                .getString("btnStartGame.toolTipText")); // NOI18N
        btnStartGame.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startScenario();
            }
        });
        btnStartGame.setEnabled(false);
        panScenarioButtons.add(btnStartGame);

        btnJoinGame = new JButton(resourceMap.getString("btnJoinGame.text")); // NOI18N
        btnJoinGame.setToolTipText(resourceMap
                .getString("btnJoinGame.toolTipText")); // NOI18N
        btnJoinGame.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                joinScenario();
            }
        });
        btnJoinGame.setEnabled(false);
        panScenarioButtons.add(btnJoinGame);

        btnLoadGame = new JButton(resourceMap.getString("btnLoadGame.text")); // NOI18N
        btnLoadGame.setToolTipText(resourceMap
                .getString("btnLoadGame.toolTipText")); // NOI18N
        btnLoadGame.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadScenario();
            }
        });
        btnLoadGame.setEnabled(false);
        panScenarioButtons.add(btnLoadGame);

        btnPrintRS = new JButton(resourceMap.getString("btnPrintRS.text")); // NOI18N
        btnPrintRS.setToolTipText(resourceMap
                .getString("btnPrintRS.toolTipText")); // NOI18N
        btnPrintRS.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printRecordSheets();
            }
        });
        btnPrintRS.setEnabled(false);
        panScenarioButtons.add(btnPrintRS);

        btnGetMul = new JButton(resourceMap.getString("btnGetMul.text")); // NOI18N
        btnGetMul
                .setToolTipText(resourceMap.getString("btnGetMul.toolTipText")); // NOI18N
        btnGetMul.setName("btnGetMul"); // NOI18N
        btnGetMul.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deployListFile();
            }
        });
        btnGetMul.setEnabled(false);
        panScenarioButtons.add(btnGetMul);

        btnResolveScenario = new JButton(
                resourceMap.getString("btnResolveScenario.text")); // NOI18N
        btnResolveScenario.setToolTipText(resourceMap
                .getString("btnResolveScenario.toolTipText")); // NOI18N
        btnResolveScenario.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resolveScenario();
            }
        });
        btnResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnResolveScenario);

        btnClearAssignedUnits = new JButton(
                resourceMap.getString("btnClearAssignedUnits.text")); // NOI18N
        btnClearAssignedUnits.setToolTipText(resourceMap
                .getString("btnClearAssignedUnits.toolTipText")); // NOI18N
        btnClearAssignedUnits.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAssignedUnits();
            }
        });
        btnClearAssignedUnits.setEnabled(false);
        panScenarioButtons.add(btnClearAssignedUnits);

        scrollScenarioView = new JScrollPane();
        scrollScenarioView.setViewportView(null);
        scrollScenarioView.setMinimumSize(new java.awt.Dimension(450, 600));
        scrollScenarioView.setPreferredSize(new java.awt.Dimension(450, 600));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panScenario.add(scrollScenarioView, gridBagConstraints);

        /* ATB */
        panLanceAssignment = new LanceAssignmentView(getCampaign());
        JScrollPane paneLanceDeployment = new JScrollPane(panLanceAssignment);
        paneLanceDeployment.setMinimumSize(new java.awt.Dimension(200, 300));
        paneLanceDeployment.setPreferredSize(new java.awt.Dimension(200, 300));
        paneLanceDeployment.setVisible(getCampaign().getCampaignOptions()
                .getUseAtB());
        splitScenario = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.VERTICAL_SPLIT, panScenario,
                paneLanceDeployment);
        splitScenario.setOneTouchExpandable(true);
        splitScenario.setResizeWeight(1.0);

        splitBrief = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT, splitMission,
                splitScenario);
        splitBrief.setOneTouchExpandable(true);
        splitBrief.setResizeWeight(0.5);
        splitBrief.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        // this can mess up the view panel so refresh it
                        changeMission();
                        refreshScenarioView();
                    }
                });
    }

    private void initMap() {
        GridBagConstraints gridBagConstraints;

        panMapView = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panMapView.add(new JLabel(resourceMap.getString("lblFindPlanet.text")),
                gridBagConstraints);

        suggestPlanet = new JSuggestField(getFrame(), getCampaign()
                .getPlanetNames());
        suggestPlanet.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Planet p = getCampaign().getPlanet(suggestPlanet.getText());
                if (null != p) {
                    panMap.setSelectedPlanet(p);
                    refreshPlanetView();
                }
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(suggestPlanet, gridBagConstraints);

        JButton btnCalculateJumpPath = new JButton(
                resourceMap.getString("btnCalculateJumpPath.text")); // NOI18N
        btnCalculateJumpPath.setToolTipText(resourceMap
                .getString("btnCalculateJumpPath.toolTipText")); // NOI18N
        btnCalculateJumpPath.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calculateJumpPath();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(btnCalculateJumpPath, gridBagConstraints);

        JButton btnBeginTransit = new JButton(
                resourceMap.getString("btnBeginTransit.text")); // NOI18N
        btnBeginTransit.setToolTipText(resourceMap
                .getString("btnBeginTransit.toolTipText")); // NOI18N
        btnBeginTransit.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beginTransit();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(btnBeginTransit, gridBagConstraints);

        panMap = new InterstellarMapPanel(getCampaign(), this);
        // lets go ahead and zoom in on the current location
        panMap.setSelectedPlanet(getCampaign().getLocation().getCurrentPlanet());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panMapView.add(panMap, gridBagConstraints);

        scrollPlanetView = new JScrollPane();
        scrollPlanetView.setMinimumSize(new java.awt.Dimension(400, 600));
        scrollPlanetView.setPreferredSize(new java.awt.Dimension(400, 600));
        scrollPlanetView
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPlanetView.setViewportView(null);
        splitMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panMapView, scrollPlanetView);
        splitMap.setOneTouchExpandable(true);
        splitMap.setResizeWeight(1.0);
        splitMap.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        // this can mess up the planet view panel so refresh it
                        refreshPlanetView();
                    }
                });

        panMap.setCampaign(getCampaign());
    }

	private void initOverviewTab() {
		GridBagConstraints gridBagConstraints;

		panOverview = new JPanel();
		setTabOverview(new JTabbedPane());
		scrollOverviewParts = new JScrollPane();
		overviewPartsPanel = new JPanel(new GridBagLayout());
		scrollOverviewTransport = new JScrollPane();
		scrollOverviewCombatPersonnel = new JScrollPane();
		scrollOverviewSupportPersonnel = new JScrollPane();
		scrollOverviewHangar = new JScrollPane();
		overviewHangarArea = new JTextArea();
		splitOverviewHangar = new JSplitPane();
		scrollOverviewUnitRating = new JScrollPane();
		scrollOverviewCargo = new JScrollPane();

		// Overview tab
		panOverview.setName("panelOverview"); // NOI18N
		panOverview.setLayout(new java.awt.GridBagLayout());

		getTabOverview().setToolTipText(resourceMap.getString("tabOverview.toolTipText")); // NOI18N
		getTabOverview().setMinimumSize(new java.awt.Dimension(250, 250));
		getTabOverview().setName("tabOverview"); // NOI18N
		getTabOverview().setPreferredSize(new java.awt.Dimension(800, 300));

		scrollOverviewTransport.setToolTipText(resourceMap.getString("scrollOverviewTransport.TabConstraints.toolTipText")); // NOI18N
		scrollOverviewTransport.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewTransport.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewTransport.TabConstraints.tabTitle"), scrollOverviewTransport);

		scrollOverviewCargo.setToolTipText(resourceMap.getString("scrollOverviewCargo.TabConstraints.toolTipText")); // NOI18N
		scrollOverviewCargo.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewCargo.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewCargo.TabConstraints.tabTitle"), scrollOverviewCargo);

		scrollOverviewCombatPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewCombatPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
		scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
		scrollOverviewSupportPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
		scrollOverviewSupportPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
        scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());

		splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewCombatPersonnel, scrollOverviewSupportPersonnel);
		splitOverviewPersonnel.setName("splitOverviewPersonnel");
		splitOverviewPersonnel.setOneTouchExpandable(true);
		splitOverviewPersonnel.setResizeWeight(0.5);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		getTabOverview().addTab(resourceMap.getString("scrollOverviewPersonnel.TabConstraints.tabTitle"), splitOverviewPersonnel);

		scrollOverviewHangar.setViewportView(new HangarReport(getCampaign()).getHangarTree());
		overviewHangarArea.setName("overviewHangarArea"); // NOI18N
		overviewHangarArea.setLineWrap(false);
		overviewHangarArea.setFont(new Font("Courier New", Font.PLAIN, 18));
		overviewHangarArea.setText("");
		overviewHangarArea.setEditable(false);
		overviewHangarArea.setName("overviewHangarArea"); // NOI18N
		splitOverviewHangar = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewHangar, overviewHangarArea);
		splitOverviewHangar.setName("splitOverviewHangar");
		splitOverviewHangar.setOneTouchExpandable(true);
		splitOverviewHangar.setResizeWeight(0.5);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		getTabOverview().addTab(resourceMap.getString("scrollOverviewHangar.TabConstraints.tabTitle"), splitOverviewHangar);

		overviewPartsPanel.setName("overviewPartsPanel"); // NOI18N
		scrollOverviewParts.setViewportView(overviewPartsPanel);
		getTabOverview().addTab(resourceMap.getString("scrollOverviewParts.TabConstraints.tabTitle"), scrollOverviewParts);

		rating = UnitRatingFactory.getUnitRating(getCampaign());
		rating.reInitialize();
		scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
		getTabOverview().addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"), scrollOverviewUnitRating);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		//gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
		panOverview.add(getTabOverview(), gridBagConstraints);
	}

    private void initPersonnelTab() {
        GridBagConstraints gridBagConstraints;

        panPersonnel = new JPanel(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panPersonnel.add(
                new JLabel(resourceMap.getString("lblPersonChoice.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> personGroupModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < PG_NUM; i++) {
            personGroupModel.addElement(getPersonnelGroupName(i));
        }
        choicePerson = new JComboBox<String>(personGroupModel);
        choicePerson.setSelectedIndex(0);
        choicePerson.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                filterPersonnel();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panPersonnel.add(choicePerson, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panPersonnel.add(
                new JLabel(resourceMap.getString("lblPersonView.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> personViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < PV_NUM; i++) {
            personViewModel.addElement(getPersonnelViewName(i));
        }
        choicePersonView = new JComboBox<String>(personViewModel);
        choicePersonView.setSelectedIndex(PV_GENERAL);
        choicePersonView.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changePersonnelView();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panPersonnel.add(choicePersonView, gridBagConstraints);

        personModel = new PersonnelTableModel(getCampaign());
        personnelTable = new JTable(personModel);
        personnelTable
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        XTableColumnModel personColumnModel = new XTableColumnModel();
        personnelTable.setColumnModel(personColumnModel);
        personnelTable.createDefaultColumnsFromModel();
        personnelSorter = new TableRowSorter<PersonnelTableModel>(personModel);
        personnelSorter.setComparator(PersonnelTableModel.COL_RANK,
                new RankSorter(getCampaign()));
        personnelSorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        for (int i = PersonnelTableModel.COL_MECH; i < PersonnelTableModel.N_COL; i++) {
            personnelSorter.setComparator(i, new BonusSorter());
        }
        personnelSorter.setComparator(PersonnelTableModel.COL_SALARY,
                new FormattedNumberSorter());
        personnelTable.setRowSorter(personnelSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_RANK,
                SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_SKILL,
                SortOrder.DESCENDING));
        personnelSorter.setSortKeys(sortKeys);
        personnelTable.addMouseListener(new PersonnelTableMouseAdapter(this));
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = personnelTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(personModel.getColumnWidth(i));
            column.setCellRenderer(personModel.getRenderer(
                    choicePersonView.getSelectedIndex() == PV_GRAPHIC,
                    getIconPackage()));
        }
        personnelTable.setIntercellSpacing(new Dimension(0, 0));
        personnelTable.setShowGrid(false);
        changePersonnelView();
        personnelTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent evt) {
                        refreshPersonnelView();
                    }
                });

        scrollPersonnelView = new JScrollPane();
        scrollPersonnelView.setMinimumSize(new java.awt.Dimension(
                PERSONNEL_VIEW_WIDTH, 600));
        scrollPersonnelView.setPreferredSize(new java.awt.Dimension(
                PERSONNEL_VIEW_WIDTH, 600));
        scrollPersonnelView
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPersonnelView.setViewportView(null);

        splitPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(personnelTable), scrollPersonnelView);
        splitPersonnel.setOneTouchExpandable(true);
        splitPersonnel.setResizeWeight(1.0);
        splitPersonnel.addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        // this can mess up the pilot view pane so refresh it
                        refreshPersonnelView();
                    }
                });
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panPersonnel.add(splitPersonnel, gridBagConstraints);

        filterPersonnel();

    }

    private void initHangarTab() {
        GridBagConstraints gridBagConstraints;

        panHangar = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panHangar.add(new JLabel(resourceMap.getString("lblUnitChoice.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> unitGroupModel = new DefaultComboBoxModel<String>();
        unitGroupModel.addElement("All Units");
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitGroupModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        choiceUnit = new JComboBox<String>(unitGroupModel);
        choiceUnit.setSelectedIndex(0);
        choiceUnit.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterUnits();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panHangar.add(choiceUnit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panHangar.add(new JLabel(resourceMap.getString("lblUnitView.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> unitViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < UV_NUM; i++) {
            unitViewModel.addElement(getUnitViewName(i));
        }
        choiceUnitView = new JComboBox<String>(unitViewModel);
        choiceUnitView.setSelectedIndex(UV_GENERAL);
        choiceUnitView.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeUnitView();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panHangar.add(choiceUnitView, gridBagConstraints);

        unitModel = new UnitTableModel(getCampaign());
        unitTable = new JTable(getUnitModel());
        getUnitTable()
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        XTableColumnModel unitColumnModel = new XTableColumnModel();
        getUnitTable().setColumnModel(unitColumnModel);
        getUnitTable().createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<UnitTableModel>(getUnitModel());
        unitSorter.setComparator(UnitTableModel.COL_STATUS,
                new UnitStatusSorter());
        unitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        unitSorter.setComparator(UnitTableModel.COL_WCLASS,
                new WeightClassSorter());
        unitSorter.setComparator(UnitTableModel.COL_COST,
                new FormattedNumberSorter());
        getUnitTable().setRowSorter(unitSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_TYPE,
                SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_WCLASS,
                SortOrder.DESCENDING));
        unitSorter.setSortKeys(sortKeys);
        getUnitTable().addMouseListener(new UnitTableMouseAdapter(this));
        getUnitTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = getUnitTable().getColumnModel().getColumn(i);
            column.setPreferredWidth(getUnitModel().getColumnWidth(i));
            column.setCellRenderer(getUnitModel().getRenderer(
                    choiceUnitView.getSelectedIndex() == UV_GRAPHIC,
                    getIconPackage()));
        }
        getUnitTable().setIntercellSpacing(new Dimension(0, 0));
        getUnitTable().setShowGrid(false);
        changeUnitView();
        getUnitTable().getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        refreshUnitView();
                    }
                });

        acquireUnitsModel = new ProcurementTableModel(getCampaign());
        acquireUnitsTable = new JTable(acquireUnitsModel);
        TableRowSorter<ProcurementTableModel> acquireUnitsSorter = new TableRowSorter<ProcurementTableModel>(
                acquireUnitsModel);
        acquireUnitsSorter.setComparator(ProcurementTableModel.COL_COST,
                new FormattedNumberSorter());
        acquireUnitsSorter.setComparator(ProcurementTableModel.COL_TARGET,
                new TargetSorter());
        acquireUnitsTable.setRowSorter(acquireUnitsSorter);
        column = null;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = acquireUnitsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(acquireUnitsModel.getColumnWidth(i));
            column.setCellRenderer(acquireUnitsModel.getRenderer());
        }
        acquireUnitsTable.setIntercellSpacing(new Dimension(0, 0));
        acquireUnitsTable.setShowGrid(false);
        acquireUnitsTable.addMouseListener(new ProcurementTableMouseAdapter(this));
        acquireUnitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ADD");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "REMOVE");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "REMOVE");

        acquireUnitsTable.getActionMap().put("ADD", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 4958203340754214211L;

            public void actionPerformed(ActionEvent e) {
                if (acquireUnitsTable.getSelectedRow() < 0) {
                    return;
                }
                acquireUnitsModel.incrementItem(acquireUnitsTable
                        .convertRowIndexToModel(acquireUnitsTable
                                .getSelectedRow()));
            }
        });

        acquireUnitsTable.getActionMap().put("REMOVE", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -8377486575329708963L;

            public void actionPerformed(ActionEvent e) {
                if (acquireUnitsTable.getSelectedRow() < 0) {
                    return;
                }
                if (acquireUnitsModel.getAcquisition(
                        acquireUnitsTable
                                .convertRowIndexToModel(acquireUnitsTable
                                        .getSelectedRow())).getQuantity() > 0) {
                    acquireUnitsModel.decrementItem(acquireUnitsTable
                            .convertRowIndexToModel(acquireUnitsTable
                                    .getSelectedRow()));
                }
            }
        });

        JScrollPane scrollAcquireUnitTable = new JScrollPane(acquireUnitsTable);
        JPanel panAcquireUnit = new JPanel(new GridLayout(0, 1));
        panAcquireUnit.setBorder(BorderFactory
                .createTitledBorder("Procurement List"));
        panAcquireUnit.add(scrollAcquireUnitTable);
        panAcquireUnit.setMinimumSize(new Dimension(200, 200));
        panAcquireUnit.setPreferredSize(new Dimension(200, 200));

        JSplitPane splitLeftUnit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(getUnitTable()), panAcquireUnit);
        splitLeftUnit.setOneTouchExpandable(true);
        splitLeftUnit.setResizeWeight(1.0);

        scrollUnitView = new JScrollPane();
        scrollUnitView.setMinimumSize(new java.awt.Dimension(UNIT_VIEW_WIDTH,
                600));
        scrollUnitView.setPreferredSize(new java.awt.Dimension(UNIT_VIEW_WIDTH,
                600));
        scrollUnitView
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollUnitView.setViewportView(null);

        splitUnit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitLeftUnit, scrollUnitView);
        getSplitUnit().setOneTouchExpandable(true);
        getSplitUnit().setResizeWeight(1.0);
        getSplitUnit().addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        // this can mess up the unit view panel so refresh it
                        refreshUnitView();
                    }
                });
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panHangar.add(getSplitUnit(), gridBagConstraints);
    }

    private void initWarehouseTab() {
        GridBagConstraints gridBagConstraints;

        panSupplies = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(
                new JLabel(resourceMap.getString("lblPartsChoice.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < SG_NUM; i++) {
            partsGroupModel.addElement(getPartsGroupName(i));
        }
        choiceParts = new JComboBox<String>(partsGroupModel);
        choiceParts.setSelectedIndex(0);
        choiceParts.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterParts();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(choiceParts, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(
                new JLabel(resourceMap.getString("lblPartsChoiceView.text")),
                gridBagConstraints);

        DefaultComboBoxModel<String> partsGroupViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < SV_NUM; i++) {
            partsGroupViewModel.addElement(getPartsGroupViewName(i));
        }
        choicePartsView = new JComboBox<String>(partsGroupViewModel);
        choicePartsView.setSelectedIndex(0);
        choicePartsView.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterParts();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        panSupplies.add(choicePartsView, gridBagConstraints);

        partsModel = new PartsTableModel();
        partsTable = new JTable(partsModel);
        partsSorter = new TableRowSorter<PartsTableModel>(partsModel);
        partsSorter.setComparator(PartsTableModel.COL_COST,
                new FormattedNumberSorter());
        partsSorter.setComparator(PartsTableModel.COL_DETAIL,
                new PartsDetailSorter());
        partsTable.setRowSorter(partsSorter);
        TableColumn column = null;
        for (int i = 0; i < PartsTableModel.N_COL; i++) {
            column = partsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(partsModel.getColumnWidth(i));
            column.setCellRenderer(partsModel.getRenderer());
        }
        partsTable.setIntercellSpacing(new Dimension(0, 0));
        partsTable.setShowGrid(false);
        partsTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        PartsTableValueChanged(evt);
                    }
                });
        partsTable.addMouseListener(new PartsTableMouseAdapter(this));

        scrollPartsTable = new JScrollPane(partsTable);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSupplies.add(scrollPartsTable, gridBagConstraints);

        acquirePartsModel = new ProcurementTableModel(getCampaign());
        acquirePartsTable = new JTable(acquirePartsModel);
        acquirePartsSorter = new TableRowSorter<ProcurementTableModel>(
                acquirePartsModel);
        acquirePartsSorter.setComparator(ProcurementTableModel.COL_COST,
                new FormattedNumberSorter());
        acquirePartsSorter.setComparator(ProcurementTableModel.COL_TARGET,
                new TargetSorter());
        acquirePartsTable.setRowSorter(acquirePartsSorter);
        column = null;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = acquirePartsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(acquirePartsModel.getColumnWidth(i));
            column.setCellRenderer(acquirePartsModel.getRenderer());
        }
        acquirePartsTable.setIntercellSpacing(new Dimension(0, 0));
        acquirePartsTable.setShowGrid(false);
        acquirePartsTable.addMouseListener(new ProcurementTableMouseAdapter(this));
        acquirePartsTable
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        acquirePartsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        acquirePartsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ADD");
        acquirePartsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "REMOVE");
        acquirePartsTable.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "REMOVE");

        acquirePartsTable.getActionMap().put("ADD", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (acquirePartsTable.getSelectedRow() < 0) {
                    return;
                }
                acquirePartsModel.incrementItem(acquirePartsTable
                        .convertRowIndexToModel(acquirePartsTable
                                .getSelectedRow()));
            }
        });

        acquirePartsTable.getActionMap().put("REMOVE", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (acquirePartsTable.getSelectedRow() < 0) {
                    return;
                }
                if (acquirePartsModel.getAcquisition(
                        acquirePartsTable
                                .convertRowIndexToModel(acquirePartsTable
                                        .getSelectedRow())).getQuantity() > 0) {
                    acquirePartsModel.decrementItem(acquirePartsTable
                            .convertRowIndexToModel(acquirePartsTable
                                    .getSelectedRow()));
                }
            }
        });

        JScrollPane scrollPartsAcquireTable = new JScrollPane(acquirePartsTable);

        JPanel acquirePartsPanel = new JPanel(new GridBagLayout());
        acquirePartsPanel.setBorder(BorderFactory
                .createTitledBorder("Procurement List"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        acquirePartsPanel.add(scrollPartsAcquireTable, gridBagConstraints);
        acquirePartsPanel.setMinimumSize(new Dimension(200, 200));
        acquirePartsPanel.setPreferredSize(new Dimension(200, 200));

        JPanel panelDoTaskWarehouse = new JPanel(new GridBagLayout());

        btnDoTaskWarehouse = new JButton(
                resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTaskWarehouse.setToolTipText(resourceMap
                .getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTaskWarehouse.setEnabled(false);
        btnDoTaskWarehouse.setName("btnDoTask"); // NOI18N
        btnDoTaskWarehouse.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doTask();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panelDoTaskWarehouse.add(btnDoTaskWarehouse, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        panelDoTaskWarehouse.add(
                new JLabel(resourceMap.getString("lblTarget.text")),
                gridBagConstraints);

        lblTargetNumWarehouse = new JLabel(
                resourceMap.getString("lblTargetNum.text"));
        lblTargetNumWarehouse
                .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panelDoTaskWarehouse.add(lblTargetNumWarehouse, gridBagConstraints);

        textTargetWarehouse = new JTextArea();
        textTargetWarehouse.setColumns(20);
        textTargetWarehouse.setEditable(false);
        textTargetWarehouse.setLineWrap(true);
        textTargetWarehouse.setRows(5);
        textTargetWarehouse.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTargetWarehouse.setWrapStyleWord(true);
        textTargetWarehouse.setBorder(null);
        JScrollPane scrTargetWarehouse = new JScrollPane(textTargetWarehouse);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTaskWarehouse.add(scrTargetWarehouse, gridBagConstraints);

        btnShowAllTechsWarehouse = new JToggleButton(
                resourceMap.getString("btnShowAllTechs.text"));
        btnShowAllTechsWarehouse.setToolTipText(resourceMap
                .getString("btnShowAllTechs.toolTipText")); // NOI18N
        btnShowAllTechsWarehouse.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterTechs(true);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTaskWarehouse.add(btnShowAllTechsWarehouse, gridBagConstraints);

        techsModel = new TechTableModel(this);
        whTechTable = new JTable(techsModel);
        whTechTable.setRowHeight(60);
        whTechTable.getColumnModel().getColumn(0)
                .setCellRenderer(techsModel.getRenderer(getIconPackage()));
        whTechTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        updateTechTarget();
                    }
                });
        whTechSorter = new TableRowSorter<TechTableModel>(techsModel);
        whTechSorter.setComparator(0, new TechSorter());
        whTechTable.setRowSorter(whTechSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        whTechSorter.setSortKeys(sortKeys);
        JScrollPane scrollWhTechTable = new JScrollPane(whTechTable);
        scrollWhTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollWhTechTable.setPreferredSize(new java.awt.Dimension(300, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panelDoTaskWarehouse.add(scrollWhTechTable, gridBagConstraints);

        astechPoolLabelWarehouse = new JLabel(
                "<html><b>Astech Pool Minutes:</> "
                        + getCampaign().getAstechPoolMinutes() + " ("
                        + getCampaign().getNumberAstechs() + " Astechs)</html>"); // NOI18N
        astechPoolLabelWarehouse
                .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelDoTaskWarehouse.add(astechPoolLabelWarehouse, gridBagConstraints);

        JSplitPane splitLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panSupplies, acquirePartsPanel);
        splitLeft.setOneTouchExpandable(true);
        splitLeft.setResizeWeight(1.0);
        splitWarehouse = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitLeft, panelDoTaskWarehouse);
        splitWarehouse.setOneTouchExpandable(true);
        splitWarehouse.setResizeWeight(1.0);
    }

    private void initRepairTab() {
        GridBagConstraints gridBagConstraints;

        panRepairBay = new JPanel(new GridBagLayout());

        JPanel panServicedUnits = new JPanel(new GridBagLayout());

        servicedUnitModel = new UnitTableModel(getCampaign());
        servicedUnitTable = new JTable(servicedUnitModel);
        servicedUnitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        servicedUnitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        servicedUnitTable.setColumnModel(new XTableColumnModel());
        servicedUnitTable.createDefaultColumnsFromModel();
        servicedUnitSorter = new TableRowSorter<UnitTableModel>(
                servicedUnitModel);
        servicedUnitSorter.setComparator(UnitTableModel.COL_STATUS,
                new UnitStatusSorter());
        servicedUnitSorter.setComparator(UnitTableModel.COL_TYPE,
                new UnitTypeSorter());
        servicedUnitTable.setRowSorter(servicedUnitSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_TYPE,
                SortOrder.DESCENDING));
        servicedUnitSorter.setSortKeys(sortKeys);
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = ((XTableColumnModel) servicedUnitTable.getColumnModel())
                    .getColumnByModelIndex(i);
            column.setPreferredWidth(servicedUnitModel.getColumnWidth(i));
            column.setCellRenderer(servicedUnitModel.getRenderer(false,
                    getIconPackage()));
            if (i != UnitTableModel.COL_NAME && i != UnitTableModel.COL_STATUS
                    && i != UnitTableModel.COL_REPAIR
                    && i != UnitTableModel.COL_PARTS
                    && i != UnitTableModel.COL_SITE
                    && i != UnitTableModel.COL_TYPE) {
                ((XTableColumnModel) servicedUnitTable.getColumnModel())
                        .setColumnVisible(column, false);
            }
        }
        servicedUnitTable.setIntercellSpacing(new Dimension(0, 0));
        servicedUnitTable.setShowGrid(false);
        servicedUnitTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        servicedUnitTableValueChanged(evt);
                    }
                });
        servicedUnitTable
                .addMouseListener(new ServicedUnitsTableMouseAdapter(this));
        JScrollPane scrollServicedUnitTable = new JScrollPane(servicedUnitTable);
        scrollServicedUnitTable
                .setMinimumSize(new java.awt.Dimension(350, 200));
        scrollServicedUnitTable.setPreferredSize(new java.awt.Dimension(350,
                200));

        txtServicedUnitView = new JTextPane();
        txtServicedUnitView.setEditable(false);
        txtServicedUnitView.setContentType("text/html");
        JScrollPane scrollServicedUnitView = new JScrollPane(
                txtServicedUnitView);
        scrollServicedUnitView.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollServicedUnitView
                .setPreferredSize(new java.awt.Dimension(350, 400));

        splitServicedUnits = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollServicedUnitTable, scrollServicedUnitView);
        splitServicedUnits.setOneTouchExpandable(true);
        splitServicedUnits.setResizeWeight(0.0);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panServicedUnits.add(splitServicedUnits, gridBagConstraints);

        JPanel panTasks = new JPanel(new GridBagLayout());

        techTable = new JTable(techsModel);
        techTable.setRowHeight(60);
        techTable.getColumnModel().getColumn(0)
                .setCellRenderer(techsModel.getRenderer(getIconPackage()));
        techTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(
                            ListSelectionEvent evt) {
                        updateTechTarget();
                    }
                });
        techSorter = new TableRowSorter<TechTableModel>(techsModel);
        techSorter.setComparator(0, new TechSorter());
        techTable.setRowSorter(techSorter);
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        techSorter.setSortKeys(sortKeys);
        JScrollPane scrollTechTable = new JScrollPane(techTable);
        scrollTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTechTable.setPreferredSize(new java.awt.Dimension(300, 300));

        tabTasks = new JTabbedPane();
        tabTasks.setMinimumSize(new java.awt.Dimension(300, 200));
        tabTasks.setName("tabTasks"); // NOI18N
        tabTasks.setPreferredSize(new java.awt.Dimension(300, 300));

        panDoTask = new JPanel(new GridBagLayout());
        panDoTask.setMinimumSize(new java.awt.Dimension(300, 100));
        panDoTask.setName("panelDoTask"); // NOI18N
        panDoTask.setPreferredSize(new java.awt.Dimension(300, 100));

        btnDoTask = new JButton(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask
                .setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doTask();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panDoTask.add(btnDoTask, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        panDoTask.add(new JLabel(resourceMap.getString("lblTarget.text")),
                gridBagConstraints);

        lblTargetNum = new JLabel(resourceMap.getString("lblTargetNum.text")); // NOI18N
        lblTargetNum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTargetNum.setName("lblTargetNum"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panDoTask.add(lblTargetNum, gridBagConstraints);

        textTarget = new JTextArea();
        textTarget.setColumns(20);
        textTarget.setEditable(false);
        textTarget.setLineWrap(true);
        textTarget.setRows(5);
        textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTarget.setWrapStyleWord(true);
        textTarget.setBorder(null);
        textTarget.setName("textTarget"); // NOI18N
        scrTextTarget = new JScrollPane(textTarget);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panDoTask.add(scrTextTarget, gridBagConstraints);

        choiceLocation = new JComboBox<String>();
        choiceLocation.removeAllItems();
        choiceLocation.addItem("All");
        choiceLocation.setEnabled(false);
        choiceLocation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                filterTasks();
            }
        });
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        panDoTask.add(choiceLocation, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panTasks.add(panDoTask, gridBagConstraints);

        taskModel = new TaskTableModel();
        taskTable = new JTable(taskModel);
        taskTable.setRowHeight(70);
        taskTable.getColumnModel().getColumn(0)
                .setCellRenderer(taskModel.getRenderer(getIconPackage()));
        taskTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        taskTableValueChanged(evt);
                    }
                });
        taskSorter = new TableRowSorter<TaskTableModel>(taskModel);
        taskSorter.setComparator(0, new TaskSorter());
        taskTable.setRowSorter(taskSorter);
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        taskSorter.setSortKeys(sortKeys);
        taskTable.addMouseListener(new TaskTableMouseAdapter());
        JScrollPane scrollTaskTable = new JScrollPane(taskTable);
        scrollTaskTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTaskTable.setPreferredSize(new java.awt.Dimension(300, 300));

        btnUseBonusPart = new JButton();
        btnUseBonusPart.setVisible(false);
        btnUseBonusPart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useBonusPart();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panTasks.add(btnUseBonusPart, gridBagConstraints);

        acquireModel = new AcquisitionTableModel();
        acquisitionTable = new JTable(acquireModel);
        acquisitionTable.setName("AcquisitionTable"); // NOI18N
        acquisitionTable.setRowHeight(70);
        acquisitionTable.getColumnModel().getColumn(0)
                .setCellRenderer(acquireModel.getRenderer(getIconPackage()));
        acquisitionTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        acquisitionTableValueChanged(evt);
                    }
                });
        acquisitionTable.addMouseListener(new AcquisitionTableMouseAdapter());
        JScrollPane scrollAcquisitionTable = new JScrollPane(acquisitionTable);
        scrollAcquisitionTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollAcquisitionTable
                .setPreferredSize(new java.awt.Dimension(300, 300));

        tabTasks.addTab(resourceMap
                .getString("scrollTaskTable.TabConstraints.tabTasks"),
                scrollTaskTable); // NOI18N
        tabTasks.addTab(resourceMap
                .getString("scrollAcquisitionTable.TabConstraints.tabTasks"),
                scrollAcquisitionTable); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTasks.add(tabTasks, gridBagConstraints);

        tabTasks.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                taskTabChanged();
            }
        });

        JPanel panTechs = new JPanel(new GridBagLayout());

        btnShowAllTechs = new JToggleButton(
                resourceMap.getString("btnShowAllTechs.text")); // NOI18N
        btnShowAllTechs.setToolTipText(resourceMap
                .getString("btnShowAllTechs.toolTipText")); // NOI18N
        btnShowAllTechs.setName("btnShowAllTechs"); // NOI18N
        btnShowAllTechs.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterTechs(false);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTechs.add(btnShowAllTechs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTechs.add(scrollTechTable, gridBagConstraints);

        astechPoolLabel = new JLabel("<html><b>Astech Pool Minutes:</> "
                + getCampaign().getAstechPoolMinutes() + " ("
                + getCampaign().getNumberAstechs() + " Astechs)</html>"); // NOI18N
        astechPoolLabel
                .setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        astechPoolLabel.setName("astechPoolLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTechs.add(astechPoolLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panRepairBay.add(panServicedUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panRepairBay.add(panTasks, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panRepairBay.add(panTechs, gridBagConstraints);

        filterTechs(true);
        filterTechs(false);
    }

    private void initInfirmaryTab() {
        GridBagConstraints gridBagConstraints;

        panInfirmary = new JPanel(new GridBagLayout());

        doctorsModel = new DocTableModel(getCampaign());
        docTable = new JTable(doctorsModel);
        docTable.setRowHeight(60);
        docTable.getColumnModel().getColumn(0)
                .setCellRenderer(doctorsModel.getRenderer(getIconPackage()));
        docTable.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        docTableValueChanged(evt);
                    }
                });
        JScrollPane scrollDocTable = new JScrollPane(docTable);
        scrollDocTable.setMinimumSize(new java.awt.Dimension(300, 300));
        scrollDocTable.setPreferredSize(new java.awt.Dimension(300, 300));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panInfirmary.add(scrollDocTable, gridBagConstraints);

        btnAssignDoc = new JButton(resourceMap.getString("btnAssignDoc.text")); // NOI18N
        btnAssignDoc.setToolTipText(resourceMap
                .getString("btnAssignDoc.toolTipText")); // NOI18N
        btnAssignDoc.setEnabled(false);
        btnAssignDoc.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                assignDoctor();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panInfirmary.add(btnAssignDoc, gridBagConstraints);

        btnUnassignDoc = new JButton(
                resourceMap.getString("btnUnassignDoc.text")); // NOI18N
        btnUnassignDoc.setEnabled(false);
        btnUnassignDoc.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unassignDoctor();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panInfirmary.add(btnUnassignDoc, gridBagConstraints);

        assignedPatientModel = new PatientTableModel(getCampaign());
        listAssignedPatient = new JList<Person>(assignedPatientModel);
        listAssignedPatient.setCellRenderer(assignedPatientModel
                .getRenderer(getIconPackage()));
        listAssignedPatient.setLayoutOrientation(JList.VERTICAL_WRAP);
        listAssignedPatient.setVisibleRowCount(5);
        listAssignedPatient.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        patientTableValueChanged();
                    }
                });
        JScrollPane scrollAssignedPatient = new JScrollPane(listAssignedPatient);
        scrollAssignedPatient.setMinimumSize(new java.awt.Dimension(300, 360));
        scrollAssignedPatient
                .setPreferredSize(new java.awt.Dimension(300, 360));

        unassignedPatientModel = new PatientTableModel(getCampaign());
        listUnassignedPatient = new JList<Person>(unassignedPatientModel);
        listUnassignedPatient.setCellRenderer(unassignedPatientModel
                .getRenderer(getIconPackage()));
        listUnassignedPatient.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listUnassignedPatient.setVisibleRowCount(-1);
        listUnassignedPatient.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    public void valueChanged(
                            javax.swing.event.ListSelectionEvent evt) {
                        patientTableValueChanged();
                    }
                });
        JScrollPane scrollUnassignedPatient = new JScrollPane(
                listUnassignedPatient);
        scrollUnassignedPatient
                .setMinimumSize(new java.awt.Dimension(300, 200));
        scrollUnassignedPatient.setPreferredSize(new java.awt.Dimension(300,
                300));

        listAssignedPatient.setBorder(BorderFactory
                .createTitledBorder(resourceMap
                        .getString("panAssignedPatient.title")));
        listUnassignedPatient.setBorder(BorderFactory
                .createTitledBorder(resourceMap
                        .getString("panUnassignedPatient.title")));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panInfirmary.add(scrollAssignedPatient, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panInfirmary.add(scrollUnassignedPatient, gridBagConstraints);
    }

    private void initFinanceTab() {
        GridBagConstraints gridBagConstraints;

        panFinances = new JPanel(new GridBagLayout());

        financeModel = new FinanceTableModel();
        financeTable = new JTable(financeModel);
        financeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        financeTable.addMouseListener(new FinanceTableMouseAdapter(this));
        financeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
        for (int i = 0; i < FinanceTableModel.N_COL; i++) {
            column = financeTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(financeModel.getColumnWidth(i));
            column.setCellRenderer(financeModel.getRenderer());
        }
        financeTable.setIntercellSpacing(new Dimension(0, 0));
        financeTable.setShowGrid(false);

        loanModel = new LoanTableModel();
        loanTable = new JTable(loanModel);
        loanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loanTable.addMouseListener(new LoanTableMouseAdapter(this));
        loanTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        column = null;
        for (int i = 0; i < LoanTableModel.N_COL; i++) {
            column = loanTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(loanModel.getColumnWidth(i));
            column.setCellRenderer(loanModel.getRenderer());
        }
        loanTable.setIntercellSpacing(new Dimension(0, 0));
        loanTable.setShowGrid(false);
        JScrollPane scrollLoanTable = new JScrollPane(loanTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        JPanel panBalance = new JPanel(new GridBagLayout());
        panBalance.add(new JScrollPane(financeTable), gridBagConstraints);
        panBalance.setBorder(BorderFactory.createTitledBorder("Balance Sheet"));
        JPanel panLoan = new JPanel(new GridBagLayout());
        panLoan.add(scrollLoanTable, gridBagConstraints);
        scrollLoanTable.setMinimumSize(new java.awt.Dimension(450, 150));
        scrollLoanTable.setPreferredSize(new java.awt.Dimension(450, 150));
        panLoan.setBorder(BorderFactory.createTitledBorder("Active Loans"));
        //JSplitPane splitFinances = new JSplitPane(JSplitPane.VERTICAL_SPLIT,panBalance, panLoan);
        //splitFinances.setOneTouchExpandable(true);
        //splitFinances.setResizeWeight(1.0);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panFinances.add(panBalance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panFinances.add(panLoan, gridBagConstraints);

        JPanel panelFinanceRight = new JPanel(new BorderLayout());

        JPanel pnlFinanceBtns = new JPanel(new GridLayout(2, 2));
        btnAddFunds = new JButton("Add Funds (GM)");
        btnAddFunds.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFundsActionPerformed(evt);
            }
        });
        btnAddFunds.setEnabled(getCampaign().isGM());
        pnlFinanceBtns.add(btnAddFunds);
        JButton btnGetLoan = new JButton("Get Loan");
        btnGetLoan.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showNewLoanDialog();
            }
        });
        pnlFinanceBtns.add(btnGetLoan);

        btnManageAssets = new JButton("Manage Assets (GM)");
        btnManageAssets.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageAssets();
            }
        });
        btnManageAssets.setEnabled(getCampaign().isGM());
        pnlFinanceBtns.add(btnManageAssets);

        panelFinanceRight.add(pnlFinanceBtns, BorderLayout.NORTH);

        areaNetWorth = new JTextArea();
        areaNetWorth.setLineWrap(true);
        areaNetWorth.setWrapStyleWord(true);
        areaNetWorth.setFont(new Font("Courier New", Font.PLAIN, 12));
        areaNetWorth.setText(getCampaign().getFinancialReport());
        areaNetWorth.setEditable(false);

        JScrollPane descriptionScroll = new JScrollPane(areaNetWorth);
        panelFinanceRight.add(descriptionScroll, BorderLayout.CENTER);
        areaNetWorth.setCaretPosition(0);
        descriptionScroll.setMinimumSize(new Dimension(300, 200));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panFinances.add(panelFinanceRight, gridBagConstraints);

    }

    private void initMenu() {

        menuBar = new JMenuBar();

        /* File Menu */
        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text")); // NOI18N

        JMenuItem menuLoad = new JMenuItem(
                resourceMap.getString("menuLoad.text")); // NOI18N
        menuLoad.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoadXmlActionPerformed(evt);
            }
        });
        menuFile.add(menuLoad);

        JMenuItem menuSave = new JMenuItem(
                resourceMap.getString("menuSave.text")); // NOI18N
        menuSave.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSaveXmlActionPerformed(evt);
            }
        });
        menuFile.add(menuSave);

        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text")); // NOI18N
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text")); // NOI18N

        /*
         * Taharqa: I think it is confusing and bad gui feng-shui to put this in
         * the menu even though it is driven by user selections that might not
         * even be in the visible tab at the moment. If we keep this it should
         * be for all personnel and parts and be clearly labeled as such
         *
         * miExportPerson.setText(resourceMap.getString("miExportPerson.text"));
         * // NOI18N miExportPerson.addActionListener(new ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent evt) {
         * miExportPersonActionPerformed(evt); } });
         * menuExport.add(miExportPerson);
         *
         * miExportParts.setText(resourceMap.getString("miExportParts.text"));
         * // NOI18N miExportParts.addActionListener(new ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent evt) {
         * miExportPartsActionPerformed(evt); } });
         * menuExport.add(miExportParts);
         */

        JMenuItem miExportOptions = new JMenuItem(
                resourceMap.getString("miExportOptions.text")); // NOI18N
        miExportOptions.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miExportOptionsActionPerformed(evt);
            }
        });
        menuExport.add(miExportOptions);

        JMenuItem miExportPersonCSV = new JMenuItem(
                resourceMap.getString("miExportPersonCSV.text")); // NOI18N
        miExportPersonCSV.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTable(personnelTable, getCampaign().getName()
                        + getCampaign().getShortDateAsString()
                        + "_ExportedPersonnel" + ".csv");
            }
        });
        menuExport.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(
                resourceMap.getString("miExportUnitCSV.text")); // NOI18N
        miExportUnitCSV.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTable(getUnitTable(), getCampaign().getName()
                        + getCampaign().getShortDateAsString()
                        + "_ExportedUnit" + ".csv");
            }
        });
        menuExport.add(miExportUnitCSV);

        JMenuItem miImportOptions = new JMenuItem(
                resourceMap.getString("miImportOptions.text")); // NOI18N
        miImportOptions.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportOptionsActionPerformed(evt);
            }
        });
        menuImport.add(miImportOptions);

        JMenuItem miImportPerson = new JMenuItem(
                resourceMap.getString("miImportPerson.text")); // NOI18N
        miImportPerson.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportPersonActionPerformed(evt);
            }
        });
        menuImport.add(miImportPerson);

        JMenuItem miImportParts = new JMenuItem(
                resourceMap.getString("miImportParts.text")); // NOI18N
        miImportParts.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miImportPartsActionPerformed(evt);
            }
        });
        menuImport.add(miImportParts);

        JMenuItem miLoadForces = new JMenuItem(
                resourceMap.getString("miLoadForces.text")); // NOI18N
        miLoadForces.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miLoadForcesActionPerformed(evt);
            }
        });
        // miLoadForces.setEnabled(false);
        menuImport.add(miLoadForces);

        menuFile.add(menuImport);
        menuFile.add(menuExport);

        JMenuItem miMercRoster = new JMenuItem(
                resourceMap.getString("miMercRoster.text")); // NOI18N
        miMercRoster.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showMercRosterDialog();
            }
        });
        menuFile.add(miMercRoster);

        JMenuItem menuOptions = new JMenuItem(
                resourceMap.getString("menuOptions.text")); // NOI18N
        menuOptions.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOptionsActionPerformed(evt);
            }
        });
        menuFile.add(menuOptions);

        JMenuItem menuOptionsMM = new JMenuItem(
                resourceMap.getString("menuOptionsMM.text")); // NOI18N
        menuOptionsMM.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOptionsMMActionPerformed(evt);
            }
        });

        menuFile.add(menuOptionsMM);

        menuThemes = new JMenu("Themes");
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem("Exit");
        menuExitItem.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getApplication().exit();
            }
        });
        menuFile.add(menuExitItem);

        menuBar.add(menuFile);

        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text")); // NOI18N

        // Personnel Market
        JMenuItem miPersonnelMarket = new JMenuItem("Personnel Market");
        miPersonnelMarket.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hirePersonMarket();
            }
        });
        menuMarket.add(miPersonnelMarket);

        // Contract Market
        miContractMarket = new JMenuItem("Contract Market");
        miContractMarket.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showContractMarket();
            }
        });
        menuMarket.add(miContractMarket);
        miContractMarket.setVisible(getCampaign().getCampaignOptions()
                .getUseAtB());

        miUnitMarket = new JMenuItem("Unit Market");
        miUnitMarket.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showUnitMarket();
            }
        });
        menuMarket.add(miUnitMarket);
        miUnitMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());

        JMenuItem miPurchaseUnit = new JMenuItem(
                resourceMap.getString("miPurchaseUnit.text")); // NOI18N
        miPurchaseUnit.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPurchaseUnitActionPerformed(evt);
            }
        });
        menuMarket.add(miPurchaseUnit);

        JMenuItem miBuyParts = new JMenuItem(
                resourceMap.getString("miBuyParts.text")); // NOI18N
        miBuyParts.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyParts();
            }
        });
        menuMarket.add(miBuyParts);
        JMenuItem miHireBulk = new JMenuItem("Hire Personnel in Bulk");
        miHireBulk.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hireBulkPersonnel();
            }
        });
        menuMarket.add(miHireBulk);

        JMenu menuHire = new JMenu(resourceMap.getString("menuHire.text")); // NOI18N

        JMenuItem miHire;
        for (int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
            miHire = new JMenuItem(Person.getRoleDesc(i, getCampaign()
                    .getFaction().isClan())); // NOI18N
            miHire.setActionCommand(Integer.toString(i));
            miHire.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    hirePerson(evt);
                }
            });
            menuHire.add(miHire);
        }
        menuMarket.add(menuHire);

        JMenu menuAstechPool = new JMenu("Astech Pool");

        JMenuItem miHireAstechs = new JMenuItem("Hire Astechs");
        miHireAstechs.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Hire How Many Astechs?", 1, 0, 100);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().increaseAstechPool(pvcd.getValue());
                refreshTechsList();
                refreshTempAstechs();
            }
        });
        menuAstechPool.add(miHireAstechs);

        JMenuItem miFireAstechs = new JMenuItem("Release Astechs");
        miFireAstechs.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Release How Many Astechs?", 1, 0,
                        getCampaign().getAstechPool());
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().decreaseAstechPool(pvcd.getValue());
                refreshTechsList();
                refreshTempAstechs();
            }
        });
        menuAstechPool.add(miFireAstechs);

        JMenuItem miFullStrengthAstechs = new JMenuItem(
                "Bring All Tech Teams to Full Strength");
        miFullStrengthAstechs.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int need = (getCampaign().getTechs().size() * 6)
                        - getCampaign().getNumberAstechs();
                if (need > 0) {
                    getCampaign().increaseAstechPool(need);
                }
                refreshTechsList();
                refreshTempAstechs();
            }
        });
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = new JMenuItem(
                "Release All Astechs from Pool");
        miFireAllAstechs.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getCampaign().decreaseAstechPool(getCampaign().getAstechPool());
                refreshTechsList();
                refreshTempAstechs();
            }
        });
        menuAstechPool.add(miFireAllAstechs);
        menuMarket.add(menuAstechPool);

        JMenu menuMedicPool = new JMenu("Medic Pool");
        JMenuItem miHireMedics = new JMenuItem("Hire Medics");
        miHireMedics.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Hire How Many Medics?", 1, 0, 100);
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().increaseMedicPool(pvcd.getValue());
                refreshDoctorsList();
                refreshTempMedics();
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = new JMenuItem("Release Medics");
        miFireMedics.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                        getFrame(), true, "Release How Many Medics?", 1, 0,
                        getCampaign().getMedicPool());
                pvcd.setVisible(true);
                if (pvcd.getValue() < 0) {
                    return;
                }
                getCampaign().decreaseMedicPool(pvcd.getValue());
                refreshDoctorsList();
                refreshTempMedics();
            }
        });
        menuMedicPool.add(miFireMedics);
        JMenuItem miFullStrengthMedics = new JMenuItem(
                "Bring All Medical Teams to Full Strength");
        miFullStrengthMedics.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                int need = (getCampaign().getDoctors().size() * 4)
                        - getCampaign().getNumberMedics();
                if (need > 0) {
                    getCampaign().increaseMedicPool(need);
                }
                refreshDoctorsList();
                refreshTempMedics();
            }
        });
        menuMedicPool.add(miFullStrengthMedics);
        JMenuItem miFireAllMedics = new JMenuItem(
                "Release All Medics from Pool");
        miFireAllMedics.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getCampaign().decreaseMedicPool(getCampaign().getMedicPool());
                refreshDoctorsList();
                refreshTempMedics();
            }
        });
        menuMedicPool.add(miFireAllMedics);
        menuMarket.add(menuMedicPool);
        menuBar.add(menuMarket);

        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text")); // NOI18N

        JMenuItem miDragoonsRating = new JMenuItem(
                resourceMap.getString("miDragoonsRating.text")); // NOI18N
        miDragoonsRating.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new RatingReport(getCampaign()));
            }
        });
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(
                resourceMap.getString("miPersonnelReport.text")); // NOI18N
        miPersonnelReport.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new PersonnelReport(getCampaign()));
            }
        });
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(
                resourceMap.getString("miHangarBreakdown.text")); // NOI18N
        miHangarBreakdown.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new HangarReport(getCampaign()));
            }
        });
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(
                resourceMap.getString("miTransportReport.text")); // NOI18N
        miTransportReport.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new TransportReport(getCampaign()));
            }
        });
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(
                resourceMap.getString("miCargoReport.text")); // NOI18N
        miCargoReport.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReport(new CargoReport(getCampaign()));
            }
        });
        menuReports.add(miCargoReport);

        menuBar.add(menuReports);

        JMenu menuCommunity = new JMenu(
                resourceMap.getString("menuCommunity.text")); // NOI18N

        JMenuItem miChat = new JMenuItem(resourceMap.getString("miChat.text")); // NOI18N
        miChat.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miChatActionPerformed(evt);
            }
        });
        menuCommunity.add(miChat);

        // menuBar.add(menuCommunity);

        JMenu menuView = new JMenu("View"); // NOI18N
        miDetachLog = new JMenuItem("Detach Daily Report Log"); // NOI18N
        miDetachLog.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDailyReportDialog();
            }
        });
        menuView.add(miDetachLog);

        miAttachLog = new JMenuItem("Attach Daily Report Log"); // NOI18N
        miAttachLog.setEnabled(false);
        miAttachLog.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideDailyReportDialog();
            }
        });
        menuView.add(miAttachLog);

        JMenuItem miBloodnameDialog = new JMenuItem("Show Bloodname Dialog...");
        miBloodnameDialog.setEnabled(true);
        miBloodnameDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showBloodnameDialog();
            }
        });
        menuView.add(miBloodnameDialog);

        miRetirementDefectionDialog = new JMenuItem(
                "Show Retirement/Defection Dialog...");
        miRetirementDefectionDialog.setEnabled(true);
        miRetirementDefectionDialog.setVisible(getCampaign()
                .getCampaignOptions().getUseAtB());
        miRetirementDefectionDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showRetirementDefectionDialog();
            }
        });
        menuView.add(miRetirementDefectionDialog);

        miShowOverview = new JCheckBoxMenuItem("Show Overview Tab");
        miShowOverview.setSelected(getTabOverview().isVisible());
        miShowOverview.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                toggleOverviewTab();
            }
        });
        menuView.add(miShowOverview);

        menuBar.add(menuView);

        JMenu menuManage = new JMenu("Manage Campaign");
        menuManage.setName("manageMenu");
        JMenuItem miGMToolsDialog = new JMenuItem("Show GM Tools Dialog");
        miGMToolsDialog.setEnabled(true);
        miGMToolsDialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showGMToolsDialog();
            }
        });
        menuManage.add(miGMToolsDialog);
        JMenuItem miAdvanceMultipleDays = new JMenuItem("Advance Multiple Days");
        miAdvanceMultipleDays.setEnabled(true);
        miAdvanceMultipleDays.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showAdvanceDaysDialog();
            }
        });
        menuManage.add(miAdvanceMultipleDays);
        JMenuItem miBloodnames = new JMenuItem(
                "Randomize Bloodnames All Personnel");
        miBloodnames.setEnabled(true);
        miBloodnames.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                randomizeAllBloodnames();
            }
        });
        menuManage.add(miBloodnames);

        menuBar.add(menuManage);

        JMenu menuHelp = new JMenu(resourceMap.getString("helpMenu.text")); // NOI18N
        menuHelp.setName("helpMenu"); // NOI18N
        JMenuItem menuAboutItem = new JMenuItem("aboutMenuItem"); // NOI18N
        menuAboutItem.setText("About");
        menuAboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAboutBox();
            }
        });
        menuHelp.add(menuAboutItem);
        menuBar.add(menuHelp);
    }

    private void initMain() {

        panLog = new DailyReportLogPanel(reportHLL);
        panLog.setMinimumSize(new java.awt.Dimension(150, 100));
        logDialog = new DailyReportLogDialog(getFrame(), this, reportHLL);
        gmTools = new GMToolsDialog(getFrame());
        bloodnameDialog = new BloodnameDialog(getFrame());

        mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabMain, panLog);
        mainPanel.setOneTouchExpandable(true);
        mainPanel.setResizeWeight(1.0);
    }

    private void initStatusBar() {

        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));

        lblRating = new JLabel();
        lblFunds = new JLabel();
        lblTempAstechs = new JLabel();
        lblTempMedics = new JLabel();

        statusPanel.add(lblRating);
        statusPanel.add(lblFunds);
        statusPanel.add(lblTempAstechs);
        statusPanel.add(lblTempMedics);
    }

    private void initTopButtons() {
        GridBagConstraints gridBagConstraints;

        lblLocation = new JLabel(getCampaign().getLocation().getReport(
                getCampaign().getCalendar().getTime())); // NOI18N

        btnPanel = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 3, 3);
        btnPanel.add(lblLocation, gridBagConstraints);

        btnGMMode = new JToggleButton(resourceMap.getString("btnGMMode.text")); // NOI18N
        btnGMMode
                .setToolTipText(resourceMap.getString("btnGMMode.toolTipText")); // NOI18N
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGMModeActionPerformed(evt);
            }
        });
        btnGMMode.setMinimumSize(new Dimension(150, 25));
        btnGMMode.setPreferredSize(new Dimension(150, 25));
        btnGMMode.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        btnPanel.add(btnGMMode, gridBagConstraints);

        btnOvertime = new JToggleButton(
                resourceMap.getString("btnOvertime.text")); // NOI18N
        btnOvertime.setToolTipText(resourceMap
                .getString("btnOvertime.toolTipText")); // NOI18N
        btnOvertime.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOvertimeActionPerformed(evt);
            }
        });
        btnOvertime.setMinimumSize(new Dimension(150, 25));
        btnOvertime.setPreferredSize(new Dimension(150, 25));
        btnOvertime.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        btnPanel.add(btnOvertime, gridBagConstraints);

        btnAdvanceDay = new JButton(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
        btnAdvanceDay.setToolTipText(resourceMap
                .getString("btnAdvanceDay.toolTipText")); // NOI18N
        btnAdvanceDay.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                advanceDay();
            }
        });
        btnAdvanceDay.setPreferredSize(new Dimension(250, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 15);
        btnPanel.add(btnAdvanceDay, gridBagConstraints);
    }

    private static void enableFullScreenMode(Window window) {
        String className = "com.apple.eawt.FullScreenUtilities";
        String methodName = "setWindowCanFullScreen";

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, new Class<?>[] {
                    Window.class, boolean.class });
            method.invoke(null, window, true);
        } catch (Throwable t) {
            System.err.println("Full screen mode is not supported");
            t.printStackTrace();
        }
    }

    private static boolean isMacOSX() {
        return System.getProperty("os.name").indexOf("Mac OS X") >= 0;
    }

    /**
     * @return the tabOverview
     */
    public JTabbedPane getTabOverview() {
        return tabOverview;
    }

    /**
     * @param tabOverview the tabOverview to set
     */
    public void setTabOverview(JTabbedPane tabOverview) {
        this.tabOverview = tabOverview;
    }

    private void miChatActionPerformed(ActionEvent evt) {
        JDialog chatDialog = new JDialog(getFrame(), "MekHQ Chat", false); //$NON-NLS-1$

        ChatClient client = new ChatClient("test", "localhost");
        client.listen();
        // chatDialog.add(client);
        chatDialog.add(new JLabel("Testing"));
        chatDialog.setResizable(true);
        chatDialog.setVisible(true);
    }

    private void addMission() {
        MissionTypeDialog mtd = new MissionTypeDialog(getFrame(), true,
                getCampaign(), this);
        mtd.setVisible(true);
    }

    private void editMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        if (null != mission) {
            if (getCampaign().getCampaignOptions().getUseAtB()
                    && mission instanceof AtBContract) {
                CustomizeAtBContractDialog cmd = new CustomizeAtBContractDialog(
                        getFrame(), true, (AtBContract) mission, getCampaign(),
                        getIconPackage().getCamos());
                cmd.setVisible(true);
                if (cmd.getMissionId() != -1) {
                    selectedMission = cmd.getMissionId();
                }
            } else {
                CustomizeMissionDialog cmd = new CustomizeMissionDialog(
                        getFrame(), true, mission, getCampaign());
                cmd.setVisible(true);
                if (cmd.getMissionId() != -1) {
                    selectedMission = cmd.getMissionId();
                }
            }
            refreshMissions();
        }

    }

    private void changeTheme(java.awt.event.ActionEvent evt) {
        final String lafClassName = evt.getActionCommand();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(lafClassName);
                    SwingUtilities.updateComponentTreeUI(frame);
                    refreshThemeChoices();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(frame,
                            "Can't change look and feel", "Invalid PLAF",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    private void refreshThemeChoices() {
        menuThemes.removeAll();
        JCheckBoxMenuItem miPlaf;
        for (LookAndFeelInfo plaf : UIManager.getInstalledLookAndFeels()) {
            miPlaf = new JCheckBoxMenuItem(plaf.getName());
            if (plaf.getName().equalsIgnoreCase(
                    UIManager.getLookAndFeel().getName())) {
                miPlaf.setSelected(true);
            }
            menuThemes.add(miPlaf);
            miPlaf.setActionCommand(plaf.getClassName());
            miPlaf.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    changeTheme(evt);
                }
            });
        }
    }

    private void completeMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        if (null != mission) {
            if (mission.hasPendingScenarios()) {
                JOptionPane
                        .showMessageDialog(
                                getFrame(),
                                "You cannot complete a mission that has pending scenarios",
                                "Pending Scenarios",
                                JOptionPane.WARNING_MESSAGE);
            } else {
                CompleteMissionDialog cmd = new CompleteMissionDialog(
                        getFrame(), true, mission);
                cmd.setVisible(true);
                if (cmd.getStatus() > Mission.S_ACTIVE) {
                    getCampaign().completeMission(mission.getId(),
                            cmd.getStatus());

                    if (getCampaign().getCampaignOptions().getUseAtB()
                            && mission instanceof AtBContract) {
                        if (((AtBContract) mission)
                                .contractExtended(getCampaign())) {
                            mission.setStatus(Mission.S_ACTIVE);
                        } else {
                            if (getCampaign().getCampaignOptions()
                                    .doRetirementRolls()) {
                                RetirementDefectionDialog rdd = new RetirementDefectionDialog(
                                        this, (AtBContract) mission, true);
                                rdd.setVisible(true);
                                if (rdd.wasAborted()) {
                                    /*
                                     * Once the retirement rolls have been made,
                                     * the outstanding payouts can be resolved
                                     * without reference to the contract and the
                                     * dialog can be accessed through the menu.
                                     */
                                    if (!getCampaign()
                                            .getRetirementDefectionTracker()
                                            .isOutstanding(mission.getId())) {
                                        mission.setStatus(Mission.S_ACTIVE);
                                    }
                                } else {
                                    if (null != getCampaign()
                                            .getRetirementDefectionTracker()
                                            .getRetirees((AtBContract) mission)
                                            && getCampaign().getFinances()
                                                    .getBalance() >= rdd
                                                    .totalPayout()) {
                                    	final int[] admins = {Person.T_ADMIN_COM, Person.T_ADMIN_HR,
                                    			Person.T_ADMIN_LOG, Person.T_ADMIN_TRA};
                                    	for (int role : admins) {
                                    		Person admin = getCampaign().findBestInRole(role, SkillType.S_ADMIN);
                                    		if (admin != null) {
                                                admin.setXp(admin.getXp() + 1);
                                                getCampaign()
                                                        .addReport(
                                                                admin.getHyperlinkedName()
                                                                        + " has gained 1 XP.");
                                    		}
                                    	}
                                    }
                                    if (!getCampaign().applyRetirement(
                                            rdd.totalPayout(),
                                            rdd.getUnitAssignments())) {
                                        mission.setStatus(Mission.S_ACTIVE);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!mission.isActive()) {
                    if (getCampaign().getCampaignOptions().getUseAtB()
                            && mission instanceof AtBContract) {
                        ((AtBContract) mission).checkForFollowup(getCampaign());
                    }
                    if (getCampaign().getSortedMissions().size() > 0) {
                        selectedMission = getCampaign().getSortedMissions()
                                .get(0).getId();
                    } else {
                        selectedMission = -1;
                    }
                    refreshMissions();
                }
            }

        }
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshRating();
    }

    private void deleteMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        MekHQ.logMessage("Attempting to Delete Mission, Mission ID: "
                + mission.getId());
        if (0 != JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete this mission?",
                "Delete mission?", JOptionPane.YES_NO_OPTION)) {
            return;
        }
        getCampaign().removeMission(mission.getId());
        if (getCampaign().getSortedMissions().size() > 0) {
            selectedMission = getCampaign().getSortedMissions().get(0).getId();
        } else {
            selectedMission = -1;
        }
        refreshMissions();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshRating();
    }

    private void addScenario() {
        Mission m = getCampaign().getMission(selectedMission);
        if (null != m) {
            CustomizeScenarioDialog csd = new CustomizeScenarioDialog(
                    getFrame(), true, null, m, getCampaign());
            csd.setVisible(true);
            refreshScenarioList();
        }
    }

    private void calculateJumpPath() {
        if (null != panMap.getSelectedPlanet()) {
            panMap.setJumpPath(getCampaign().calculateJumpPath(
                    getCampaign().getCurrentPlanetName(),
                    panMap.getSelectedPlanet().getName()));
            refreshPlanetView();
        }
    }

    private void beginTransit() {
        if (panMap.getJumpPath().isEmpty()) {
            return;
        }
        getCampaign().getLocation().setJumpPath(panMap.getJumpPath());
        refreshPlanetView();
        refreshLocation();
        panMap.setJumpPath(new JumpPath());
        panMap.repaint();
    }

    private void doTask() {// GEN-FIRST:event_btnDoTaskActionPerformed
        int selectedRow = -1;
        int partId = -1;
        int selectedLocation = -1;
        Unit selectedUnit = null;
        // int selectedTechRow = -1;
        Person tech = getSelectedTech();
        if (onWarehouseTab()) {
            selectedRow = partsTable.getSelectedRow();
            // selectedTechRow = whTechTable.getSelectedRow();
            Part part = getSelectedTask();
            if (null == part) {
                return;
            }
            if (null == tech) {
                return;
            }
            partId = part.getId();
            // get a new cloned part to work with and decrement original
            Part repairable = part.clone();
            part.decrementQuantity();
            getCampaign().fixPart(repairable, tech);
            getCampaign().addPart(repairable, 0);
            // if the break off part failed to be repaired, then follow it with
            // the focus
            // otherwise keep the focus on the current row
            if (repairable.needsFixing() && !repairable.isBeingWorkedOn()) {
                partId = repairable.getId();
            }
        } else if (repairsSelected()) {
            selectedRow = taskTable.getSelectedRow();
            // selectedTechRow = TechTable.getSelectedRow();
            selectedLocation = choiceLocation.getSelectedIndex();
            selectedUnit = getSelectedServicedUnit();
            Part part = getSelectedTask();
            if (null == part) {
                return;
            }
            Unit u = part.getUnit();
            if (null != u && u.isSelfCrewed()) {
                tech = u.getEngineer();
            }
            if (null == tech) {
                return;
            }
            if (part.onBadHipOrShoulder() && !part.isSalvaging()) {
                if (part instanceof MekLocation
                        && ((MekLocation) part).isBreached()
                        && 0 != JOptionPane
                                .showConfirmDialog(
                                        frame,
                                        "You are sealing a limb with a bad shoulder or hip.\n"
                                                + "You may continue, but this limb cannot be repaired and you will have to\n"
                                                + "scrap it in order to repair the internal structure and fix the shoulder/hip.\n"
                                                + "Do you wish to continue?",
                                        "Busted Hip/Shoulder",
                                        JOptionPane.YES_NO_OPTION)) {
                    return;
                } else if (part instanceof MekLocation
                        && ((MekLocation) part).isBlownOff()
                        && 0 != JOptionPane
                                .showConfirmDialog(
                                        frame,
                                        "You are re-attaching a limb with a bad shoulder or hip.\n"
                                                + "You may continue, but this limb cannot be repaired and you will have to\n"
                                                + "scrap it in order to repair the internal structure and fix the shoulder/hip.\n"
                                                + "Do you wish to continue?",
                                        "Busted Hip/Shoulder",
                                        JOptionPane.YES_NO_OPTION)) {
                    return;
                } else if (0 != JOptionPane
                        .showConfirmDialog(
                                frame,
                                "You are repairing/replacing a part on a limb with a bad shoulder or hip.\n"
                                        + "You may continue, but this limb cannot be repaired and you will have to\n"
                                        + "remove this equipment if you wish to scrap and then replace the limb.\n"
                                        + "Do you wish to continue?",
                                "Busted Hip/Shoulder",
                                JOptionPane.YES_NO_OPTION)) {
                    return;
                }
            }
            getCampaign().fixPart(part, tech);
            if (null != u && !u.isRepairable()
                    && u.getSalvageableParts().size() == 0) {
                selectedRow = -1;
                getCampaign().removeUnit(u.getId());
            }
            if (null != u && !getCampaign().getServiceableUnits().contains(u)) {
                selectedRow = -1;
            }
        } else if (acquireSelected()) {
            selectedRow = acquisitionTable.getSelectedRow();
            IAcquisitionWork acquisition = getSelectedAcquisition();
            if (null == acquisition) {
                return;
            }
            getCampaign().getShoppingList().addShoppingItem(acquisition, 1,
                    getCampaign());
        }

        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshTaskList();
        refreshAcquireList();
        refreshTechsList();
        refreshPartsList();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshOverview();
        filterTasks();

        // get the selected row back for tasks
        if (selectedRow != -1) {
            if (acquireSelected()) {
                if (acquisitionTable.getRowCount() > 0) {
                    if (acquisitionTable.getRowCount() == selectedRow) {
                        acquisitionTable.setRowSelectionInterval(
                                selectedRow - 1, selectedRow - 1);
                    } else {
                        acquisitionTable.setRowSelectionInterval(selectedRow,
                                selectedRow);
                    }
                }
            } else if (onWarehouseTab()) {
                boolean found = false;
                for (int i = 0; i < partsTable.getRowCount(); i++) {
                    Part p = partsModel.getPartAt(partsTable
                            .convertRowIndexToModel(i));
                    if (p.getId() == partId) {
                        partsTable.setRowSelectionInterval(i, i);
                        partsTable.scrollRectToVisible(partsTable.getCellRect(
                                i, 0, true));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // then set to the current selected row
                    if (partsTable.getRowCount() > 0) {
                        if (partsTable.getRowCount() == selectedRow) {
                            partsTable.setRowSelectionInterval(selectedRow - 1,
                                    selectedRow - 1);
                            // partsTable.scrollRectToVisible(partsTable.getCellRect(selectedRow-1,
                            // 0, true));
                        } else {
                            partsTable.setRowSelectionInterval(selectedRow,
                                    selectedRow);
                            // partsTable.scrollRectToVisible(partsTable.getCellRect(selectedRow,
                            // 0, true));
                        }
                    }
                }
            } else if (repairsSelected()) {
                if (taskTable.getRowCount() > 0) {
                    if (taskTable.getRowCount() == selectedRow) {
                        taskTable.setRowSelectionInterval(selectedRow - 1,
                                selectedRow - 1);
                    } else {
                        taskTable.setRowSelectionInterval(selectedRow,
                                selectedRow);
                    }
                }
            }

			JTable table = techTable;
			if (onWarehouseTab()) {
				table = whTechTable;
			}

			// If requested, switch to top entry
			if(getCampaign().getCampaignOptions().useResetToFirstTech() && table.getRowCount() > 0) {
				table.setRowSelectionInterval(0, 0);
			} else {
				// Or get the selected tech back
				for (int i = 0; i < table.getRowCount(); i++) {
					Person p = techsModel
							.getTechAt(table.convertRowIndexToModel(i));
					if (tech.getId().equals(p.getId())) {
						table.setRowSelectionInterval(i, i);
						break;
					}
				}
			}
        }
        if (selectedLocation != -1) {
            if (selectedUnit == null || getSelectedServicedUnit() == null
                    || !selectedUnit.equals(getSelectedServicedUnit())
                    || selectedLocation >= choiceLocation.getItemCount()) {
                selectedLocation = 0;
            }
            choiceLocation.setSelectedIndex(selectedLocation);
        }

    }// GEN-LAST:event_btnDoTaskActionPerformed

    private void useBonusPart() {
        int selectedRow = -1;
        if (acquireSelected()) {
            selectedRow = acquisitionTable.getSelectedRow();
            IAcquisitionWork acquisition = getSelectedAcquisition();
            if (null == acquisition) {
                return;
            }
            if(acquisition instanceof AmmoBin) {
                acquisition = ((AmmoBin) acquisition).getAcquisitionWork();
            }
            String report = acquisition.find(0);
            if (report.endsWith("0 days.")) {
                AtBContract contract = getCampaign().getAttachedAtBContract(
                        getSelectedAcquisition().getUnit());
                if (null == contract) {
                    for (Mission m : getCampaign().getMissions()) {
                        if (m.isActive() && m instanceof AtBContract
                                && ((AtBContract) m).getNumBonusParts() > 0) {
                            contract = (AtBContract) m;
                            break;
                        }
                    }
                }
                if (null == contract) {
                    MekHQ.logError("AtB: used bonus part but no contract has bonus parts available.");
                } else {
                    contract.useBonusPart();
                }
            }
        }

        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshTaskList();
        refreshAcquireList();
        refreshTechsList();
        refreshPartsList();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        filterTasks();

        if (selectedRow != -1) {
            if (acquireSelected()) {
                if (acquisitionTable.getRowCount() > 0) {
                    if (acquisitionTable.getRowCount() == selectedRow) {
                        acquisitionTable.setRowSelectionInterval(
                                selectedRow - 1, selectedRow - 1);
                    } else {
                        acquisitionTable.setRowSelectionInterval(selectedRow,
                                selectedRow);
                    }
                }
            }
        }
    }

    private Person getSelectedTech() {
        JTable table = techTable;
        if (onWarehouseTab()) {
            table = whTechTable;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return techsModel.getTechAt(table.convertRowIndexToModel(row));
    }

    private Person getSelectedDoctor() {
        int row = docTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return doctorsModel.getDoctorAt(docTable.convertRowIndexToModel(row));
    }

    public Part getSelectedTask() {
        if (onWarehouseTab()) {
            int row = partsTable.getSelectedRow();
            if (row < 0) {
                return null;
            }
            return partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
        }
        int row = taskTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
    }

    private IAcquisitionWork getSelectedAcquisition() {
        int row = acquisitionTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return acquireModel.getAcquisitionAt(acquisitionTable
                .convertRowIndexToModel(row));
    }

    private Unit getSelectedServicedUnit() {
        int row = servicedUnitTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return servicedUnitModel.getUnit(servicedUnitTable
                .convertRowIndexToModel(row));
    }

    private void taskTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        filterTechs(false);
        updateTechTarget();
    }

    private void acquisitionTableValueChanged(
            javax.swing.event.ListSelectionEvent evt) {
        filterTechs(false);
        updateTechTarget();
    }

    private void servicedUnitTableValueChanged(
            javax.swing.event.ListSelectionEvent evt) {
        refreshTaskList();
        refreshAcquireList();
        int selected = servicedUnitTable.getSelectedRow();
        txtServicedUnitView.setText("");
        if (selected > -1) {
            Unit unit = servicedUnitModel.getUnit(servicedUnitTable
                    .convertRowIndexToModel(selected));
            if (null != unit) {
                MechView mv = new MechView(unit.getEntity(), false);
                txtServicedUnitView
                        .setText("<div style='font: 12pt monospaced'>"
                                + mv.getMechReadoutBasic() + "<br>"
                                + mv.getMechReadoutLoadout() + "</div>");
            }
        }
    }

    public void focusOnUnit(UUID id) {
        if (null == id) {
            return;
        }
        if (mainPanel.getDividerLocation() < 700) {
            if (mainPanel.getLastDividerLocation() > 700) {
                mainPanel
                        .setDividerLocation(mainPanel.getLastDividerLocation());
            } else {
                mainPanel.resetToPreferredSizes();
            }
        }
        getSplitUnit().resetToPreferredSizes();
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap
                .getString("panHangar.TabConstraints.tabTitle")));
        int row = -1;
        for (int i = 0; i < getUnitTable().getRowCount(); i++) {
            if (getUnitModel().getUnit(getUnitTable().convertRowIndexToModel(i)).getId()
                    .equals(id)) {
                row = i;
                break;
            }
        }
        if (row == -1) {
            // try expanding the filter to all units
            choiceUnit.setSelectedIndex(0);
            for (int i = 0; i < getUnitTable().getRowCount(); i++) {
                if (getUnitModel().getUnit(getUnitTable().convertRowIndexToModel(i))
                        .getId().equals(id)) {
                    row = i;
                    break;
                }
            }

        }
        if (row != -1) {
            getUnitTable().setRowSelectionInterval(row, row);
            getUnitTable().scrollRectToVisible(getUnitTable().getCellRect(row, 0, true));
        }

    }

    public void focusOnUnitInRepairBay(UUID id) {
        if (null == id) {
            return;
        }
        if (mainPanel.getDividerLocation() < 700) {
            if (mainPanel.getLastDividerLocation() > 700) {
                mainPanel
                        .setDividerLocation(mainPanel.getLastDividerLocation());
            } else {
                mainPanel.resetToPreferredSizes();
            }
        }
        tabMain.setSelectedIndex(6);
        int row = -1;
        for (int i = 0; i < servicedUnitTable.getRowCount(); i++) {
            if (servicedUnitModel
                    .getUnit(servicedUnitTable.convertRowIndexToModel(i))
                    .getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row != -1) {
            servicedUnitTable.setRowSelectionInterval(row, row);
            servicedUnitTable.scrollRectToVisible(servicedUnitTable
                    .getCellRect(row, 0, true));
        }

    }

    public void focusOnPerson(UUID id) {
        if (null == id) {
            return;
        }
        if (mainPanel.getDividerLocation() < 700) {
            if (mainPanel.getLastDividerLocation() > 700) {
                mainPanel
                        .setDividerLocation(mainPanel.getLastDividerLocation());
            } else {
                mainPanel.resetToPreferredSizes();
            }
        }
        splitPersonnel.resetToPreferredSizes();
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap
                .getString("panPersonnel.TabConstraints.tabTitle")));
        int row = -1;
        for (int i = 0; i < personnelTable.getRowCount(); i++) {
            if (personModel.getPerson(personnelTable.convertRowIndexToModel(i))
                    .getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row == -1) {
            // try expanding the filter to all units
            choicePerson.setSelectedIndex(0);
            for (int i = 0; i < personnelTable.getRowCount(); i++) {
                if (personModel
                        .getPerson(personnelTable.convertRowIndexToModel(i))
                        .getId().equals(id)) {
                    row = i;
                    break;
                }
            }

        }
        if (row != -1) {
            personnelTable.setRowSelectionInterval(row, row);
            personnelTable.scrollRectToVisible(personnelTable.getCellRect(row,
                    0, true));
        }
    }

    public void showNews(int id) {
        NewsItem news = getCampaign().getNews().getNewsItem(id);
        if (null != news) {
            NewsReportDialog nrd = new NewsReportDialog(frame, news);
            nrd.setVisible(true);
        }
    }

    private void taskTabChanged() {
        filterTechs(false);
        updateTechTarget();
    }

    private void patientTableValueChanged() {
        updateAssignDoctorEnabled();
    }

    private void docTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        refreshPatientList();
        updateAssignDoctorEnabled();

    }

    private void PartsTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        filterTechs(true);
        updateTechTarget();
    }

    public InterstellarMapPanel getMapPanel() {
        return panMap;
    }

    private void advanceDay() {
        // first check for overdue loan payments - dont allow advancement until
        // these are addressed
        if (getCampaign().checkOverDueLoans()) {
            refreshFunds();
            refreshFinancialTransactions();
            refreshReport();
            return;
        }
        if (getCampaign().checkRetirementDefections()) {
            showRetirementDefectionDialog();
            return;
        }
        if (getCampaign().checkYearlyRetirements()) {
            showRetirementDefectionDialog();
            return;
        }
        if (nagShortMaintenance()) {
            return;
        }
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            if (nagShortDeployments()) {
                return;
            }
            if (nagOutstandingScenarios()) {
                return;
            }
        }
        getCampaign().newDay();
        refreshScenarioList();
        refreshMissions();
        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshTaskList();
        refreshAcquireList();
        refreshTechsList();
        refreshPartsList();
        refreshPatientList();
        refreshDoctorsList();
        refreshCalendar();
        refreshLocation();
        refreshOrganization();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshOverview();
        panMap.repaint();
    }// GEN-LAST:event_btnAdvanceDayActionPerformed

    public boolean nagShortMaintenance() {
        if (!getCampaign().getCampaignOptions().checkMaintenance()) {
            return false;
        }
        Vector<Unit> notMaintained = new Vector<Unit>();
        int totalAstechMinutesNeeded = 0;
        for (Unit u : getCampaign().getUnits()) {
            if (u.requiresMaintenance() && null == u.getTech()) {
                notMaintained.add(u);
            } else {
                // only add astech minutes for non-crewed units
                if (null == u.getEngineer()) {
                    totalAstechMinutesNeeded += (u.getMaintenanceTime() * 6);
                }
            }
        }

        if (notMaintained.size() > 0) {
            if (JOptionPane.YES_OPTION != JOptionPane
                    .showConfirmDialog(
                            null,
                            "You have unmaintained units. Do you really wish to advance the day?",
                            "Unmaintained Units", JOptionPane.YES_NO_OPTION)) {
                return true;
            }
        }

        int minutesAvail = getCampaign().getPossibleAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            minutesAvail += getCampaign().getPossibleAstechPoolOvertime();
        }
        if (minutesAvail < totalAstechMinutesNeeded) {
            int needed = (int) Math
                    .ceil((totalAstechMinutesNeeded - minutesAvail) / 480D);
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
                    "You do not have enough astechs to provide for full maintenance. You need "
                            + needed
                            + " more astech(s). Do you wish to proceed?",
                    "Astech shortage", JOptionPane.YES_NO_OPTION)) {
                return true;
            }
        }

        return false;
    }

    public boolean nagShortDeployments() {
        if (getCampaign().getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            return false;
        }
        for (Mission m : getCampaign().getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract)
                    || !getCampaign().getLocation().isOnPlanet()) {
                continue;
            }
            if (getCampaign().getDeploymentDeficit((AtBContract) m) > 0) {
                return 0 != JOptionPane
                        .showConfirmDialog(
                                null,
                                "You have not met the deployment levels required by contract. Do your really wish to advance the day?",
                                "Unmet deployment requirements",
                                JOptionPane.YES_NO_OPTION);
            }
        }
        return false;
    }

    public boolean nagOutstandingScenarios() {
        for (Mission m : getCampaign().getMissions()) {
            if (!m.isActive() || !(m instanceof AtBContract)) {
                continue;
            }
            for (Scenario s : m.getScenarios()) {
                if (!s.isCurrent() || !(s instanceof AtBScenario)) {
                    continue;
                }
                if (getCampaign().getDate().equals(s.getDate())) {
                    return 0 != JOptionPane
                            .showConfirmDialog(
                                    null,
                                    "You have a pending battle. Failure to deploy will result in a defeat and a minor contract breach. Do your really wish to advance the day?",
                                    "Pending battle", JOptionPane.YES_NO_OPTION);
                }
            }
        }
        return false;
    }

    private void assignDoctor() {
        Person doctor = getSelectedDoctor();
        for (Person p : getSelectedUnassignedPatients()) {
            if (null != p
                    && null != doctor
                    && (p.needsFixing() || (getCampaign().getCampaignOptions()
                            .useAdvancedMedical() && p.needsAMFixing()))
                    && getCampaign().getPatientsFor(doctor) < 25
                    && getCampaign().getTargetFor(p, doctor).getValue() != TargetRoll.IMPOSSIBLE) {
                p.setDoctorId(doctor.getId(), getCampaign()
                        .getCampaignOptions().getHealingWaitingPeriod());
            }
        }

        refreshTechsList();
        refreshDoctorsList();
        refreshPatientList();
    }

    private void unassignDoctor() {
        for (Person p : getSelectedAssignedPatients()) {
            if ((null != p)) {
                p.setDoctorId(null, getCampaign().getCampaignOptions()
                        .getNaturalHealingWaitingPeriod());
            }
        }

        refreshTechsList();
        refreshDoctorsList();
        refreshPatientList();
    }

    private void hirePerson(java.awt.event.ActionEvent evt) {
        int type = Integer.parseInt(evt.getActionCommand());
        NewRecruitDialog npd = new NewRecruitDialog(getFrame(), true,
                getCampaign().newPerson(type), getCampaign(), this,
                getIconPackage().getPortraits());
        npd.setVisible(true);
    }

    public void hirePersonMarket() {
        PersonnelMarketDialog pmd = new PersonnelMarketDialog(getFrame(), this,
                getCampaign(), getIconPackage().getPortraits());
        pmd.setVisible(true);
    }

    private void hireBulkPersonnel() {
        HireBulkPersonnelDialog hbpd = new HireBulkPersonnelDialog(getFrame(),
                true, getCampaign(), this);
        hbpd.setVisible(true);
    }

    public void showContractMarket() {
        ContractMarketDialog cmd = new ContractMarketDialog(getFrame(), this,
                getCampaign());
        cmd.setVisible(true);
        refreshMissions();
        refreshFinancialTransactions();
    }

    public void showUnitMarket() {
        UnitMarketDialog umd = new UnitMarketDialog(getFrame(), this,
                getCampaign());
        umd.setVisible(true);
    }

    private void menuSaveXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuSaveActionPerformed
        MekHQ.logMessage("Saving campaign...");
        // Choose a file...
        File file = selectSaveCampaignFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".cpnx")) {
            path += ".cpnx";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));
            getCampaign().writeToXml(pw);
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.logMessage("Campaign saved to " + file);
        } catch (Exception ex) {
            MekHQ.logError(ex);
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly save your game. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.", "Could not save game",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    private File selectSaveCampaignFile() {
        JFileChooser saveCpgn = new JFileChooser("./campaigns/");
        saveCpgn.setDialogTitle("Save Campaign");
        saveCpgn.setFileFilter(new CampaignFileFilter());
        saveCpgn.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString() + ".cpnx")); //$NON-NLS-1$
        int returnVal = saveCpgn.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveCpgn.getSelectedFile() == null)) {
            // I want a file, y'know!
            return null;
        }

        File file = saveCpgn.getSelectedFile();

        return file;
    }

    private void menuLoadXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuLoadActionPerformed
        File f = selectLoadCampaignFile();
        if (null == f) {
            return;
        }
        DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(
                getApplication(), getFrame(), f);
        // TODO: does this effectively deal with memory management issues?
        dataLoadingDialog.setVisible(true);
    }

    private File selectLoadCampaignFile() {
        JFileChooser loadCpgn = new JFileChooser("./campaigns/");
        loadCpgn.setDialogTitle("Load Campaign");
        loadCpgn.setFileFilter(new CampaignFileFilter());
        int returnVal = loadCpgn.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadCpgn.getSelectedFile() == null)) {
            // I want a file, y'know!
            return null;
        }

        File file = loadCpgn.getSelectedFile();

        return file;
    }

    public static String getPersonnelGroupName(int group) {
        switch (group) {
            case PG_ACTIVE:
                return "Active Personnel";
            case PG_COMBAT:
                return "Combat Personnel";
            case PG_MW:
                return "Mechwarriors";
            case PG_CREW:
                return "Vehicle Crews";
            case PG_PILOT:
                return "Aerospace Pilots";
            case PG_CPILOT:
                return "Conventional Pilots";
            case PG_PROTO:
                return "Protomech Pilots";
            case PG_BA:
                return "Battle Armor Infantry";
            case PG_SOLDIER:
                return "Conventional Infantry";
            case PG_SUPPORT:
                return "Support Personnel";
            case PG_VESSEL:
                return "Large Vessel Crews";
            case PG_TECH:
                return "Techs";
            case PG_DOC:
                return "Medical Staff";
            case PG_ADMIN:
                return "Administrators";
            case PG_RETIRE:
                return "Retired Personnel";
            case PG_MIA:
                return "Personnel MIA";
            case PG_KIA:
                return "Rolls of Honor (KIA)";
            default:
                return "?";
        }
    }

    public static String getPartsGroupName(int group) {
        switch (group) {
            case SG_ALL:
                return "All Parts";
            case SG_ARMOR:
                return "Armor";
            case SG_SYSTEM:
                return "System Components";
            case SG_EQUIP:
                return "Equipment";
            case SG_LOC:
                return "Locations";
            case SG_WEAP:
                return "Weapons";
            case SG_AMMO:
                return "Ammunition";
            case SG_AMMO_BIN:
                return "Ammunition Bins";
            case SG_MISC:
                return "Miscellaneous Equipment";
            case SG_ENGINE:
                return "Engines";
            case SG_GYRO:
                return "Gyros";
            case SG_ACT:
                return "Actuators";
            default:
                return "?";
        }
    }

    public static String getPartsGroupViewName(int view) {
        switch (view) {
            case SV_ALL:
                return "All";
            case SV_IN_TRANSIT:
                return "In Transit";
            case SV_RESERVED:
                return "Reserved for Refit/Repair";
            case SV_UNDAMAGED:
                return "Undamaged";
            case SV_DAMAGED:
                return "Damaged";
            default:
                return "?";
        }
    }

    public static String getPersonnelViewName(int group) {
        switch (group) {
            case PV_GRAPHIC:
                return "Graphic";
            case PV_GENERAL:
                return "General";
            case PV_PILOT:
                return "Piloting/Gunnery Skills";
            case PV_INF:
                return "Infantry Skills";
            case PV_TACTIC:
                return "Tactical Skills";
            case PV_TECH:
                return "Tech Skills";
            case PV_ADMIN:
                return "Admin Skills";
            case PV_FLUFF:
                return "Fluff Information";
            default:
                return "?";
        }
    }

    public static String getUnitViewName(int group) {
        switch (group) {
            case UV_GRAPHIC:
                return "Graphic";
            case UV_GENERAL:
                return "General";
            case UV_DETAILS:
                return "Details";
            case UV_STATUS:
                return "Status";
            default:
                return "?";
        }
    }

    private void btnOvertimeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOvertimeActionPerformed
        getCampaign().setOvertime(btnOvertime.isSelected());
        refreshTechsList();
        refreshTaskList();
        refreshAcquireList();
        filterTasks();
    }// GEN-LAST:event_btnOvertimeActionPerformed

    private void btnGMModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGMModeActionPerformed
        getCampaign().setGMMode(btnGMMode.isSelected());
        btnAddFunds.setEnabled(btnGMMode.isSelected());
        btnManageAssets.setEnabled(btnGMMode.isSelected());
        refreshOverview();
    }// GEN-LAST:event_btnGMModeActionPerformed

    private void menuOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuOptionsActionPerformed
        boolean atb = getCampaign().getCampaignOptions().getUseAtB();
        CampaignOptionsDialog cod = new CampaignOptionsDialog(getFrame(), true,
                getCampaign(), getIconPackage().getCamos());
        cod.setVisible(true);
        if (atb != getCampaign().getCampaignOptions().getUseAtB()) {
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                getCampaign().initAtB();
                refreshLanceAssignments();
            }
            splitScenario.getBottomComponent().setVisible(
                    getCampaign().getCampaignOptions().getUseAtB());
            splitScenario.resetToPreferredSizes();
            miContractMarket.setVisible(getCampaign().getCampaignOptions()
                    .getUseAtB());
            miUnitMarket.setVisible(getCampaign().getCampaignOptions()
                    .getUseAtB());
            miRetirementDefectionDialog.setVisible(getCampaign()
                    .getCampaignOptions().getUseAtB());
        }
        refreshCalendar();
        getCampaign().reloadNews();
        changePersonnelView();
        refreshPersonnelList();
        refreshOverview();
        panMap.repaint();
    }// GEN-LAST:event_menuOptionsActionPerformed

    private void menuOptionsMMActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuOptionsActionPerformed
        GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign());
        god.refreshOptions();
        god.setVisible(true);
        if (!god.wasCancelled()) {
            getCampaign().setGameOptions(god.getOptions());
            god.setCampaignOptionsFromGameOptions();
            refreshCalendar();
            changePersonnelView();
            refreshPersonnelList();
            refreshOverview();
        }
    }// GEN-LAST:event_menuOptionsActionPerformed

    private void miLoadForcesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miLoadForcesActionPerformed
        try {
            loadListFile(true);
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miLoadForcesActionPerformed

    private void miImportPersonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miImportPersonActionPerformed
        try {
            loadPersonFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miImportPersonActionPerformed

    public void miExportPersonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            savePersonFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miExportOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            saveOptionsFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miImportOptionsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            loadOptionsFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miImportPartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miImportPersonActionPerformed
        try {
            loadPartsFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miImportPersonActionPerformed

    public void miExportPartsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miExportPersonActionPerformed
        try {
            savePartsFile();
        } catch (IOException ex) {
            Logger.getLogger(CampaignGUI.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }// GEN-LAST:event_miExportPersonActionPerformed

    private void miPurchaseUnitActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_miPurchaseUnitActionPerformed
        UnitSelectorDialog usd = new UnitSelectorDialog(getFrame(), this,
                getCampaign(), true);

        usd.setVisible(true);
        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshOverview();
    }// GEN-LAST:event_miPurchaseUnitActionPerformed

    private void buyParts() {
        PartsStoreDialog psd = new PartsStoreDialog(true, this);
        psd.setVisible(true);
        refreshPartsList();
        refreshAcquireList();
        refreshOverview();
    }

    private void showNewLoanDialog() {
        NewLoanDialog nld = new NewLoanDialog(getFrame(), true, getCampaign());
        nld.setVisible(true);
        refreshFinancialTransactions();
        refreshFunds();
        refreshReport();
        refreshRating();
    }

    private void showMercRosterDialog() {
        MercRosterDialog mrd = new MercRosterDialog(getFrame(), true,
                getCampaign());
        mrd.setVisible(true);
    }

    public void refitUnit(Refit r, boolean selectModelName) {
        if (r.getOriginalEntity() instanceof Dropship
                || r.getOriginalEntity() instanceof Jumpship) {
            Person engineer = r.getOriginalUnit().getEngineer();
            if (engineer == null) {
                JOptionPane
                        .showMessageDialog(
                                frame,
                                "You cannot refit a ship that does not have an engineer. Assign a qualified vessel crew to this unit.",
                                "No Engineer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            r.setTeamId(engineer.getId());
        } else if (getCampaign().getTechs().size() > 0) {
            String name;
            HashMap<String, Person> techHash = new HashMap<String, Person>();
            for (Person tech : getCampaign().getTechs()) {
                if (getCampaign().isWorkingOnRefit(tech)) {
                    continue;
                }
                name = tech.getFullName()
                        + ", "
                        + tech.getPrimaryRoleDesc()
                        + " ("
                        + getCampaign().getTargetFor(r, tech)
                                .getValueAsString() + "+)";
                techHash.put(name, tech);
            }
            String[] techNames = new String[techHash.keySet().size()];
            int i = 0;
            for (String n : techHash.keySet()) {
                techNames[i] = n;
                i++;
            }
            String s = (String) JOptionPane.showInputDialog(frame,
                    "Which tech should work on the refit?", "Select Tech",
                    JOptionPane.PLAIN_MESSAGE, null, techNames, techNames[0]);
            if (null == s) {
                return;
            }
            r.setTeamId(techHash.get(s).getId());
        } else {
            JOptionPane.showMessageDialog(frame,
                    "You have no techs available to work on this refit.",
                    "No Techs", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectModelName) {
            // select a model name
            RefitNameDialog rnd = new RefitNameDialog(frame, true, r);
            rnd.setVisible(true);
            if (rnd.wasCancelled()) {
                // Set the tech team to null since we may want to change it when we re-do the refit
                r.setTeamId(null);
                return;
            }
        }
        // TODO: allow overtime work?
        // check to see if user really wants to do it - give some info on what
        // will be done
        // TODO: better information
        if (0 != JOptionPane
                .showConfirmDialog(null, "Are you sure you want to refit "
                        + r.getUnit().getName() + "?", "Proceed?",
                        JOptionPane.YES_NO_OPTION)) {
            return;
        }
        try {
            r.begin();
        } catch (EntityLoadingException ex) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            "For some reason, the unit you are trying to customize cannot be loaded\n and so the customization was cancelled. Please report the bug with a description\nof the unit being customized.",
                            "Could not customize unit",
                            JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "IO Exception",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        getCampaign().refit(r);
        getPanMekLab().clearUnit();
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
        refreshUnitList();
        refreshServicedUnitList();
        refreshOrganization();
        refreshPartsList();
        refreshOverview();
    }

    private void showReport(Report report) {
        ReportDialog rd = new ReportDialog(getFrame(), report);
        rd.setVisible(true);
    }

    public void showMaintenanceReport(UUID id) {
        if (null == id) {
            return;
        }
        Unit u = getCampaign().getUnit(id);
        if (null == u) {
            return;
        }
        MaintenanceReportDialog mrd = new MaintenanceReportDialog(getFrame(), u);
        mrd.setVisible(true);
    }

    public UUID selectTech(Unit u, String desc) {
        String name;
        HashMap<String, Person> techHash = new HashMap<String, Person>();
        for (Person tech : getCampaign().getTechs()) {
            if (tech.canTech(u.getEntity()) && !tech.isMothballing()) {
                name = tech.getFullName()
                        + ", "
                        + SkillType.getExperienceLevelName(tech
                                .getSkillForWorkingOn(u).getExperienceLevel())
                        + " (" + Math.max(0, (tech.getMinutesLeft() - tech.getMaintenanceTimeUsing()))
                        + "min)";
                techHash.put(name, tech);
            }
        }
        if (techHash.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "You have no techs available.", "No Techs",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String[] techNames = new String[techHash.keySet().size()];
        int i = 0;
        for (String n : techHash.keySet()) {
            techNames[i] = n;
            i++;
        }
        String s = (String) JOptionPane.showInputDialog(frame,
                "Which tech should work on " + desc + "?", "Select Tech",
                JOptionPane.PLAIN_MESSAGE, null, techNames, techNames[0]);
        if (null == s) {
            return null;
        }
        return techHash.get(s).getId();
    }

    public Part getPartByNameAndDetails(String pnd) {
    	ArrayList<Part> store = getCampaign().getPartsStore().getInventory();
    	for (Part p : store) {
    		String pname = p.getName();
			String details = p.getDetails();
			details = details.replaceFirst("\\d+\\shit\\(s\\),\\s", "");
		    details = details.replaceFirst("\\d+\\shit\\(s\\)", "");
		    if (details.length() > 0 && !(p instanceof Armor || p instanceof BaArmor || p instanceof ProtomekArmor)) {
		    	pname +=  " (" + details + ")";
		    }
		    if (pname.equals(pnd)) {
		    	return p;
		    }
    	}
    	return null;
    }

    public void refreshOverview() {
    	int drIndex = getTabOverview().indexOfComponent(scrollOverviewUnitRating);
        if (!getCampaign().getCampaignOptions().useDragoonRating() && drIndex != -1) {
        	getTabOverview().removeTabAt(drIndex);
        } else {
        	if (drIndex == -1) {
        		getTabOverview().addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"), scrollOverviewUnitRating);
        	}
        }

        scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
        scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
        scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());
        scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
        scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
        HangarReport hr = new HangarReport(getCampaign());
        overviewHangarArea.setText(hr.getHangarTotals());
        scrollOverviewHangar.setViewportView(hr.getHangarTree());
        refreshOverviewPartsInUse();
    }

    public void refreshOverviewPartsInUse() {
    	overviewPartsPanel.removeAll();
    	int i = 0;
    	int j = 0;
    	GridBagConstraints gbc;
    	JLabel partName;
    	JLabel partNum;
    	JButton partBuy;
    	JButton partBuyInBulk;
    	JButton partAddGM;
    	JButton partAddBulkGM;
    	Color bgc = new Color(255, 255, 255);
    	boolean opaque;
    	Hashtable<String, Integer> pinu = getCampaign().getPartsInUse();
    	String[] keys = (String[]) pinu.keySet().toArray(new String[0]);
    	Arrays.sort(keys);
    	for (String pname : keys) {
    		final String pn = pname;
    		j++;
    		if (i % 2 == 0) {
    			opaque = false;
    		} else {
    			opaque = true;
    		}
    		int pnum = pinu.get(pname);
    		partName = new JLabel("<html>&nbsp;&nbsp;&nbsp;"+pname+"&nbsp;&nbsp;&nbsp;</html>");
    		partName.setBackground(bgc);
    		partName.setOpaque(opaque);
    		partName.setVerticalAlignment(SwingConstants.CENTER);
    		gbc = new java.awt.GridBagConstraints();
    		gbc.gridx = 0;
    		gbc.gridy = i;
    		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
    		gbc.fill = GridBagConstraints.BOTH;
    		gbc.weightx = 1.0;
            if(j == pinu.size()) {
            	gbc.weighty = 1.0;
            }
            overviewPartsPanel.add(partName, gbc);
    		partNum = new JLabel("  "+Integer.toString(pnum)+"  ");
    		partNum.setBackground(bgc);
    		partNum.setOpaque(opaque);
    		partNum.setVerticalAlignment(SwingConstants.CENTER);
    		partNum.setHorizontalAlignment(SwingConstants.TRAILING);
    		gbc.gridx = 1;
    		gbc.weightx = 0.0;
    		overviewPartsPanel.add(partNum, gbc);
    		partBuy = new JButton("Buy");
    		partBuy.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	Part p = getPartByNameAndDetails(pn);
	            	getCampaign().getShoppingList().addShoppingItem(p.getAcquisitionWork(), 1, getCampaign());
	            	refreshReport();
	        		refreshAcquireList();
	        		refreshPartsList();
	        		refreshFinancialTransactions();
	                refreshOverview();
	            }
	        });
    		gbc.gridx = 2;
    		overviewPartsPanel.add(partBuy, gbc);
    		partBuyInBulk = new JButton("Buy in Bulk");
    		partBuyInBulk.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	Part p = getPartByNameAndDetails(pn);
	            	int quantity = 1;
	            	PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true, "How Many " + p.getName(), quantity, 1, 100);
	    			pcd.setVisible(true);
	    			quantity = pcd.getValue();
	            	getCampaign().getShoppingList().addShoppingItem(p.getAcquisitionWork(), quantity, getCampaign());
	            	refreshReport();
	        		refreshAcquireList();
	        		refreshPartsList();
	        		refreshFinancialTransactions();
	                refreshOverview();
	            }
	        });
    		gbc.gridx = 3;
    		overviewPartsPanel.add(partBuyInBulk, gbc);
    		partAddGM = new JButton("Add (GM)");
    		partAddGM.setEnabled(getCampaign().isGM());
    		partAddGM.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	Part p = getPartByNameAndDetails(pn);
	            	getCampaign().addPart(p.clone(), 0);
	            	refreshReport();
	        		refreshAcquireList();
	        		refreshPartsList();
	        		refreshFinancialTransactions();
	                refreshOverview();
	            }
	        });
    		gbc.gridx = 4;
    		overviewPartsPanel.add(partAddGM, gbc);
    		partAddBulkGM = new JButton("Add in Bulk (GM)");
    		partAddBulkGM.setEnabled(getCampaign().isGM());
    		partAddBulkGM.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	            	Part p = getPartByNameAndDetails(pn);
	            	int quantity = 1;
	            	PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true, "How Many " + p.getName(), quantity, 1, 100);
	    			pcd.setVisible(true);
	    			quantity = pcd.getValue();
	    			while(quantity > 0) {
	    				getCampaign().addPart(p.clone(), 0);
	    		        quantity--;
	    		    }
	            	refreshReport();
	        		refreshAcquireList();
	        		refreshPartsList();
	        		refreshFinancialTransactions();
	                refreshOverview();
	            }
	        });
    		gbc.gridx = 5;
    		overviewPartsPanel.add(partAddBulkGM, gbc);
            i++;
    	}
    }

    public void refreshPersonnelView() {
        int row = personnelTable.getSelectedRow();
        if (row < 0) {
            scrollPersonnelView.setViewportView(null);
            return;
        }
        Person selectedPerson = personModel.getPerson(personnelTable
                .convertRowIndexToModel(row));
        scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson,
                getCampaign(), getIconPackage().getPortraits()));
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollPersonnelView.getVerticalScrollBar().setValue(0);
            }
        });
    }

    public void refreshUnitView() {
        int row = getUnitTable().getSelectedRow();
        if (row < 0) {
            scrollUnitView.setViewportView(null);
            return;
        }
        Unit selectedUnit = getUnitModel().getUnit(getUnitTable()
                .convertRowIndexToModel(row));
        scrollUnitView.setViewportView(new UnitViewPanel(selectedUnit,
                getCampaign(), getIconPackage().getCamos(), getIconPackage()
                        .getMechTiles()));
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollUnitView.getVerticalScrollBar().setValue(0);
            }
        });
    }

    private void refreshScenarioView() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            scrollScenarioView.setViewportView(null);
            btnStartGame.setEnabled(false);
            btnJoinGame.setEnabled(false);
            btnLoadGame.setEnabled(false);
            btnGetMul.setEnabled(false);
            btnClearAssignedUnits.setEnabled(false);
            btnResolveScenario.setEnabled(false);
            btnPrintRS.setEnabled(false);
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        if (getCampaign().getCampaignOptions().getUseAtB()
                && (scenario instanceof AtBScenario)) {
            scrollScenarioView.setViewportView(new AtBScenarioViewPanel(
                    (AtBScenario) scenario, getCampaign(), getIconPackage(),
                    frame));
        } else {
            scrollScenarioView.setViewportView(new ScenarioViewPanel(scenario,
                    getCampaign(), getIconPackage()));
        }
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                scrollScenarioView.getVerticalScrollBar().setValue(0);
            }
        });
        boolean unitsAssigned = scenario.getForces(getCampaign()).getAllUnits()
                .size() > 0;
        boolean canStartGame = scenario.isCurrent() && unitsAssigned;
        if (getCampaign().getCampaignOptions().getUseAtB()
                && scenario instanceof AtBScenario) {
            canStartGame = canStartGame
                    && getCampaign().getDate().equals(scenario.getDate());
        }
        btnStartGame.setEnabled(canStartGame);
        btnJoinGame.setEnabled(canStartGame);
        btnLoadGame.setEnabled(canStartGame);
        btnGetMul.setEnabled(scenario.isCurrent() && unitsAssigned);
        btnClearAssignedUnits.setEnabled(scenario.isCurrent() && unitsAssigned);
        btnResolveScenario.setEnabled(canStartGame);
        btnPrintRS.setEnabled(scenario.isCurrent() && unitsAssigned);

    }

    public void refreshForceView() {
        Object node = orgTree.getLastSelectedPathComponent();
        if (null == node
                || -1 == orgTree.getRowForPath(orgTree.getSelectionPath())) {
            scrollForceView.setViewportView(null);
            return;
        }
        if (node instanceof Unit) {
            Unit u = ((Unit) node);
            JTabbedPane tabUnit = new JTabbedPane();
            Person p = u.getCommander();
            if (p != null) {
                String name = "Commander";
                if (u.usesSoloPilot()) {
                    name = "Pilot";
                }
                tabUnit.add(name, new PersonViewPanel(p, getCampaign(),
                        getIconPackage().getPortraits()));
            }
            tabUnit.add("Unit", new UnitViewPanel(u, getCampaign(),
                    getIconPackage().getCamos(), getIconPackage()
                            .getMechTiles()));
            scrollForceView.setViewportView(tabUnit);
            // This odd code is to make sure that the scrollbar stays at the top
            // I can't just call it here, because it ends up getting reset
            // somewhere later
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    scrollForceView.getVerticalScrollBar().setValue(0);
                }
            });
        } else if (node instanceof Force) {
            scrollForceView.setViewportView(new ForceViewPanel((Force) node,
                    getCampaign(), getIconPackage()));
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    scrollForceView.getVerticalScrollBar().setValue(0);
                }
            });
        }
    }

    public void refreshLanceAssignments() {
        panLanceAssignment.refresh();
    }

    public void refreshPlanetView() {
        JumpPath path = panMap.getJumpPath();
        if (null != path && !path.isEmpty()) {
            scrollPlanetView.setViewportView(new JumpPathViewPanel(path,
                    getCampaign()));
            return;
        }
        Planet planet = panMap.getSelectedPlanet();
        if (null != planet) {
            scrollPlanetView.setViewportView(new PlanetViewPanel(planet,
                    getCampaign()));
        }
    }

    private void addFundsActionPerformed(java.awt.event.ActionEvent evt) {
        AddFundsDialog addFundsDialog = new AddFundsDialog(getFrame(), true);
        addFundsDialog.setVisible(true);
        if (addFundsDialog.getClosedType() == JOptionPane.OK_OPTION) {
            long funds = addFundsDialog.getFundsQuantity();
            String description = addFundsDialog.getFundsDescription();
            int category = addFundsDialog.getCategory();
            getCampaign().addFunds(funds, description, category);
            refreshReport();
            refreshFunds();
            refreshFinancialTransactions();
        }
    }

    private void manageAssets() {
        ManageAssetsDialog mad = new ManageAssetsDialog(getFrame(),
                getCampaign());
        mad.setVisible(true);
        refreshReport();
        refreshFunds();
        refreshFinancialTransactions();
    }

    protected void loadListFile(boolean allowNewPilots) throws IOException {
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Units");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".mul");
            }

            @Override
            public String getDescription() {
                return "MUL file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File unitFile = loadList.getSelectedFile();

        if (unitFile != null) {
            // I need to get the parser myself, because I want to pull both
            // entities and pilots from it
            // Create an empty parser.
            MULParser parser = new MULParser();

            // Open up the file.
            InputStream listStream = new FileInputStream(unitFile);

            // Read a Vector from the file.
            try {
                parser.parse(listStream);
                listStream.close();
            } catch (Exception excep) {
                excep.printStackTrace(System.err);
                // throw new IOException("Unable to read from: " +
                // unitFile.getName());
            }

            // Was there any error in parsing?
            if (parser.hasWarningMessage()) {
                MekHQ.logMessage(parser.getWarningMessage());
            }

            // Add the units from the file.
            for (Entity entity : parser.getEntities()) {
                getCampaign().addUnit(entity, allowNewPilots, 0);
            }

            // add any ejected pilots
            for (Crew pilot : parser.getPilots()) {
                if (pilot.isEjected()) {
                    // getCampaign().addPilot(pilot, PilotPerson.T_MECHWARRIOR,
                    // false);
                }
            }
        }

        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        refreshPatientList();
        refreshReport();
        refreshOverview();
    }

    protected void loadPersonFile() throws IOException {
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Personnel");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".prsx");
            }

            @Override
            public String getDescription() {
                return "Personnel file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File personnelFile = loadList.getSelectedFile();

        if (personnelFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(personnelFile);

            MekHQ.logMessage("Starting load of personnel file from XML...");
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.logError(ex);
            }

            Element personnelEle = xmlDoc.getDocumentElement();
            NodeList nl = personnelEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            personnelEle.normalize();

            Version version = new Version(personnelEle.getAttribute("version"));

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    MekHQ.logMessage("Unknown node type not loaded in Personnel nodes: "
                            + wn2.getNodeName());

                    continue;
                }

                Person p = Person.generateInstanceFromXML(wn2, getCampaign(),
                        version);
                if (getCampaign().getPerson(p.getId()) != null
                        && getCampaign().getPerson(p.getId()).getFullName()
                                .equals(p.getFullName())) {
                    MekHQ.logMessage("ERROR: Cannot load person who exists, ignoring. (Name: "
                            + p.getFullName() + ")");
                    p = null;
                }
                if (p != null) {
                    getCampaign().addPersonWithoutId(p, true);
                }

                // Clear some values we no longer should have set in case this
                // has transferred campaigns or things in the campaign have
                // changed...
                p.setUnitId(null);
                p.clearTechUnitIDs();
            }
            MekHQ.logMessage("Finished load of personnel file");
        }

        refreshPersonnelList();
        refreshPatientList();
        refreshTechsList();
        refreshDoctorsList();
        refreshReport();
        refreshFinancialTransactions();
        refreshOverview();
    }

    private void savePersonFile() throws IOException {
        JFileChooser savePersonnel = new JFileChooser(".");
        savePersonnel.setDialogTitle("Save Personnel");
        savePersonnel.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".prsx");
            }

            @Override
            public String getDescription() {
                return "Personnel file";
            }
        });
        savePersonnel.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString()
                + "_ExportedPersonnel" + ".prsx")); //$NON-NLS-1$
        int returnVal = savePersonnel.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (savePersonnel.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = savePersonnel.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".prsx")) {
            path += ".prsx";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            int row = personnelTable.getSelectedRow();
            if (row < 0) {
                MekHQ.logMessage("ERROR: Cannot export person if no one is selected! Ignoring.");
                return;
            }
            Person selectedPerson = personModel.getPerson(personnelTable
                    .convertRowIndexToModel(row));
            int[] rows = personnelTable.getSelectedRows();
            Person[] people = new Person[rows.length];
            for (int i = 0; i < rows.length; i++) {
                people[i] = personModel.getPerson(personnelTable
                        .convertRowIndexToModel(rows[i]));
            }
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            ResourceBundle resourceMap = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // Start the XML root.
            pw.println("<personnel version=\""
                    + resourceMap.getString("Application.version") + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    people[i].writeToXml(pw, 1);
                }
            } else {
                selectedPerson.writeToXml(pw, 1);
            }
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.println("</personnel>");
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.logMessage("Personnel saved to " + file);
        } catch (Exception ex) {
            MekHQ.logError(ex);
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your personnel. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.",
                            "Could not export personnel",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    private void saveOptionsFile() throws IOException {
        JFileChooser saveOptions = new JFileChooser(".");
        saveOptions.setDialogTitle("Save Campaign Options");
        saveOptions.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "Campaign options file";
            }
        });
        saveOptions.setSelectedFile(new File("campaignOptions.xml")); //$NON-NLS-1$
        int returnVal = saveOptions.showSaveDialog(mainPanel);

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

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            ResourceBundle resourceMap = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<options version=\""
                    + resourceMap.getString("Application.version") + "\">");
            // Start the XML root.
            getCampaign().getCampaignOptions().writeToXml(pw, 1);
            pw.println("\t<skillTypes>");
            for (String name : SkillType.skillList) {
                SkillType type = SkillType.getType(name);
                if (null != type) {
                    type.writeToXml(pw, 2);
                }
            }
            pw.println("\t</skillTypes>");
            pw.println("\t<specialAbilities>");
            for (String key : SpecialAbility.getAllSpecialAbilities().keySet()) {
                SpecialAbility.getAbility(key).writeToXml(pw, 2);
            }
            pw.println("\t</specialAbilities>");
            getCampaign().getRandomSkillPreferences().writeToXml(pw, 1);
            pw.println("</options>");
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.logMessage("Campaign Options saved saved to " + file);
        } catch (Exception ex) {
            MekHQ.logError(ex);
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your campaign options. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.",
                            "Could not export campaign options",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    protected void loadPartsFile() throws IOException {
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Parts");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".parts");
            }

            @Override
            public String getDescription() {
                return "Parts file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File partsFile = loadList.getSelectedFile();

        if (partsFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(partsFile);

            MekHQ.logMessage("Starting load of parts file from XML...");
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.logError(ex);
            }

            Element partsEle = xmlDoc.getDocumentElement();
            NodeList nl = partsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            partsEle.normalize();

            Version version = new Version(partsEle.getAttribute("version"));

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                    // Error condition of sorts!
                    // Errr, what should we do here?
                    MekHQ.logMessage("Unknown node type not loaded in Parts nodes: "
                            + wn2.getNodeName());

                    continue;
                }

                Part p = Part.generateInstanceFromXML(wn2, version);
                if (p != null) {
                    p.setCampaign(getCampaign());
                    getCampaign().addPartWithoutId(p);
                }
            }
            MekHQ.logMessage("Finished load of parts file");
        }

        refreshPartsList();
        refreshReport();
        refreshFinancialTransactions();
    }

    protected void loadOptionsFile() throws IOException {
        JFileChooser loadList = new JFileChooser(".");
        loadList.setDialogTitle("Load Campaign Options");

        loadList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".xml");
            }

            @Override
            public String getDescription() {
                return "Campaign options file";
            }
        });

        int returnVal = loadList.showOpenDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File optionsFile = loadList.getSelectedFile();

        if (optionsFile != null) {
            // Open up the file.
            InputStream fis = new FileInputStream(optionsFile);

            MekHQ.logMessage("Starting load of options file from XML...");
            // Initialize variables.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document xmlDoc = null;

            try {
                // Using factory get an instance of document builder
                DocumentBuilder db = dbf.newDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(fis);
            } catch (Exception ex) {
                MekHQ.logError(ex);
            }

            Element partsEle = xmlDoc.getDocumentElement();
            NodeList nl = partsEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            partsEle.normalize();

            Version version = new Version(partsEle.getAttribute("version"));

            CampaignOptions options = null;
            RandomSkillPreferences rsp = null;

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String xn = wn.getNodeName();

                if (xn.equalsIgnoreCase("campaignOptions")) {
                    options = CampaignOptions
                            .generateCampaignOptionsFromXml(wn);
                } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                    rsp = RandomSkillPreferences
                            .generateRandomSkillPreferencesFromXml(wn);
                } else if (xn.equalsIgnoreCase("skillTypes")) {
                    NodeList wList = wn.getChildNodes();

                    // Okay, lets iterate through the children, eh?
                    for (int x2 = 0; x2 < wList.getLength(); x2++) {
                        Node wn2 = wList.item(x2);

                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (wn2.getNodeName().startsWith("ability-")) {
                            continue;
                        } else if (!wn2.getNodeName().equalsIgnoreCase(
                                "skillType")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in Skill Type nodes: "
                                    + wn2.getNodeName());

                            continue;
                        }
                        SkillType.generateInstanceFromXML(wn2, version);
                    }
                } else if (xn.equalsIgnoreCase("specialAbilities")) {
                    PilotOptions poptions = new PilotOptions();
                    SpecialAbility.clearSPA();

                    NodeList wList = wn.getChildNodes();

                    // Okay, lets iterate through the children, eh?
                    for (int x2 = 0; x2 < wList.getLength(); x2++) {
                        Node wn2 = wList.item(x2);

                        // If it's not an element node, we ignore it.
                        if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn2.getNodeName().equalsIgnoreCase("ability")) {
                            // Error condition of sorts!
                            // Errr, what should we do here?
                            MekHQ.logMessage("Unknown node type not loaded in Special Ability nodes: "
                                    + wn2.getNodeName());

                            continue;
                        }

                        SpecialAbility.generateInstanceFromXML(wn2, poptions,
                                null);
                    }
                }

            }

            if (null != options) {
                this.getCampaign().setCampaignOptions(options);
            }
            if (null != rsp) {
                this.getCampaign().setRandomSkillPreferences(rsp);
            }

            MekHQ.logMessage("Finished load of campaign options file");
        }

        refreshCalendar();
        getCampaign().reloadNews();
        changePersonnelView();
        refreshPersonnelList();
        panMap.repaint();
    }

    private void savePartsFile() throws IOException {
        JFileChooser saveParts = new JFileChooser(".");
        saveParts.setDialogTitle("Save Parts");
        saveParts.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".parts");
            }

            @Override
            public String getDescription() {
                return "Parts file";
            }
        });
        saveParts.setSelectedFile(new File(getCampaign().getName()
                + getCampaign().getShortDateAsString()
                + "_ExportedParts" + ".parts")); //$NON-NLS-1$
        int returnVal = saveParts.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveParts.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = saveParts.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".parts")) {
            path += ".parts";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos = null;
        PrintWriter pw = null;

        try {
            int row = partsTable.getSelectedRow();
            if (row < 0) {
                MekHQ.logMessage("ERROR: Cannot export parts if none are selected! Ignoring.");
                return;
            }
            Part selectedPart = partsModel.getPartAt(partsTable
                    .convertRowIndexToModel(row));
            int[] rows = partsTable.getSelectedRows();
            Part[] parts = new Part[rows.length];
            for (int i = 0; i < rows.length; i++) {
                parts[i] = partsModel.getPartAt(partsTable
                        .convertRowIndexToModel(rows[i]));
            }
            fos = new FileOutputStream(file);
            pw = new PrintWriter(new OutputStreamWriter(fos, "UTF-8"));

            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            ResourceBundle resourceMap = ResourceBundle
                    .getBundle("mekhq.resources.MekHQ");
            // Start the XML root.
            pw.println("<parts version=\""
                    + resourceMap.getString("Application.version") + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    parts[i].writeToXml(pw, 1);
                }
            } else {
                selectedPart.writeToXml(pw, 1);
            }
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.println("</parts>");
            pw.flush();
            pw.close();
            fos.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.logMessage("Parts saved to " + file);
        } catch (Exception ex) {
            MekHQ.logError(ex);
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your parts. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.", "Could not export parts",
                            JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    private void exportTable(JTable table, String suggestedName) {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setDialogTitle("Save Table");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "comma-separated text file";
            }
        });
        fileChooser.setSelectedFile(new File(suggestedName)); //$NON-NLS-1$
        int returnVal = fileChooser.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (fileChooser.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File file = fileChooser.getSelectedFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".csv")) {
            path += ".csv";
            file = new File(path);
        }

        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        Utilities.exportTabletoCSV(table, file);
    }

    protected void clearAssignedUnits() {
        if (0 == JOptionPane.showConfirmDialog(null,
                "Do you really want to remove all units from this scenario?",
                "Clear Units?", JOptionPane.YES_NO_OPTION)) {
            int row = scenarioTable.getSelectedRow();
            Scenario scenario = scenarioModel.getScenario(scenarioTable
                    .convertRowIndexToModel(row));
            if (null == scenario) {
                return;
            }
            scenario.clearAllForcesAndPersonnel(getCampaign());
            refreshPersonnelList();
            refreshServicedUnitList();
            refreshUnitList();
            refreshOrganization();
            refreshTaskList();
            refreshUnitView();
            refreshPartsList();
            refreshAcquireList();
            refreshReport();
            refreshPatientList();
            refreshPersonnelList();
            refreshScenarioList();
            refreshOverview();
            filterTasks();
        }
    }

    protected void resolveScenario() {
        int row = scenarioTable.getSelectedRow();
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        if (null == scenario) {
            return;
        }
        boolean control = JOptionPane
                .showConfirmDialog(
                        getFrame(),
                        "Did your side control the battlefield at the end of the scenario?",
                        "Control of Battlefield?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
        ResolveScenarioTracker tracker = new ResolveScenarioTracker(scenario,
                getCampaign(), control);
        ChooseMulFilesDialog chooseFilesDialog = new ChooseMulFilesDialog(
                getFrame(), true, tracker, control);
        chooseFilesDialog.setVisible(true);
        if (chooseFilesDialog.wasCancelled()) {
            return;
        }
        // tracker.postProcessEntities(control);
        ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(
                getFrame(), true, tracker);
        resolveDialog.setVisible(true);
        if (getCampaign().getCampaignOptions().getUseAtB()
                && getCampaign().getMission(scenario.getMissionId()) instanceof AtBContract
                && getCampaign().getRetirementDefectionTracker().getRetirees()
                        .size() > 0) {
            RetirementDefectionDialog rdd = new RetirementDefectionDialog(
                    getCampaignGUI(), (AtBContract) getCampaign().getMission(
                            scenario.getMissionId()), false);
            rdd.setVisible(true);
            if (!rdd.wasAborted()) {
                getCampaign().applyRetirement(rdd.totalPayout(),
                        rdd.getUnitAssignments());
            }
        }

        refreshScenarioList();
        refreshOrganization();
        refreshServicedUnitList();
        refreshUnitList();
        refreshPersonnelList();
        filterPersonnel();
        refreshDoctorsList();
        refreshPatientList();
        refreshReport();
        changeMission();
        refreshFinancialTransactions();
        refreshOverview();
    }

    protected void printRecordSheets() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        Vector<Entity> chosen = new Vector<Entity>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (u.isUnmanned()) {
                continue;
            }
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    chosen.add(u.getEntity());
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (")
                            .append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(
                    getFrame(),
                    "The following units could not be deployed:"
                            + undeployed.toString(),
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }

        if (chosen.size() > 0) {
            UnitPrintManager.printAllUnits(chosen, true);
        }
    }

    protected void loadScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        if (null != scenario) {
            ((MekHQ) getApplication()).startHost(scenario, true, null);
        }
    }

    protected void startScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Unit> chosen = new ArrayList<Unit>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (u.isUnmanned()) {
                continue;
            }
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    chosen.add(u);
                    u.getEntity().getCrew().setSize(u.getActiveCrew().size()); // So
                                                                               // MegaMek
                                                                               // has
                                                                               // correct
                                                                               // crew
                                                                               // sizes
                                                                               // set
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (")
                            .append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(
                    getFrame(),
                    "The following units could not be deployed:"
                            + undeployed.toString(),
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }

        if (getCampaign().getCampaignOptions().getUseAtB()
                && scenario instanceof AtBScenario) {
            ((AtBScenario) scenario).refresh(getCampaign());

            /*
             * For standard battles, any deployed unit not part of the lance
             * assigned to the battle is assumed to be reinforcements.
             */
            if (null != ((AtBScenario) scenario).getLance(getCampaign())) {
                int assignedForceId = ((AtBScenario) scenario).getLance(
                        getCampaign()).getForceId();
                for (Force f : scenario.getForces(getCampaign()).getSubForces()) {
                    if (f.getId() != assignedForceId) {
                        Vector<UUID> units = f.getAllUnits();
                        int slowest = 12;
                        for (UUID id : units) {
                            if (chosen.contains(getCampaign().getUnit(id))) {
                            	int speed = getCampaign().getUnit(id).getEntity().getWalkMP();
                            	if (getCampaign().getUnit(id).getEntity().getJumpMP() > 0) {
                            		if (getCampaign().getUnit(id).getEntity() instanceof megamek.common.Infantry) {
                            			speed = getCampaign().getUnit(id).getEntity().getJumpMP();
                            		} else {
                            			speed++;
                            		}
                            	}
                                slowest = Math.min(slowest, speed);
                            }
                        }
                        int deployRound = 12 - slowest;
                        Person commander = getCampaign().getPerson(
                                Lance.findCommander(f.getId(), getCampaign()));
                        if (null != commander
                                && null != commander
                                        .getSkill(SkillType.S_STRATEGY)) {
                            deployRound -= commander.getSkill(
                                    SkillType.S_STRATEGY).getLevel();
                        }
                        deployRound = Math.max(deployRound, 0);

                        for (UUID id : units) {
                            if (chosen.contains(getCampaign().getUnit(id))) {
                                getCampaign().getUnit(id).getEntity()
                                        .setDeployRound(deployRound);
                            }
                        }
                    }
                }
            }
        }

        if (chosen.size() > 0) {
            // Ensure that the MegaMek year GameOption matches the campaign year
            GameOptions gameOpts = getCampaign().getGameOptions();
            int campaignYear = getCampaign().getCalendar().get(Calendar.YEAR);
            if (gameOpts.intOption("year") != campaignYear) {
                gameOpts.getOption("year").setValue(campaignYear);
            }
            ((MekHQ) getApplication()).startHost(scenario, false, chosen);
        }
    }

    protected void joinScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Unit> chosen = new ArrayList<Unit>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (u.isUnmanned()) {
                continue;
            }
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    chosen.add(u);
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (")
                            .append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(
                    getFrame(),
                    "The following units could not be deployed:"
                            + undeployed.toString(),
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }

        if (chosen.size() > 0) {
            ((MekHQ) getApplication()).joinGame(scenario, chosen);
        }
    }

    protected void deployListFile() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable
                .convertRowIndexToModel(row));
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Entity> chosen = new ArrayList<Entity>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            // TODO: Inoperable and Advanced Medical Checks
            if (u.isUnmanned() || !u.isFunctional()) {
                continue;
            }
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    chosen.add(u.getEntity());
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (")
                            .append(u.checkDeployment()).append(")");
                }
            }
        }

        JFileChooser saveList = new JFileChooser(".");
        saveList.setDialogTitle("Deploy Units");

        saveList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".mul");
            }

            @Override
            public String getDescription() {
                return "MUL file";
            }
        });

        saveList.setSelectedFile(new File(scenario.getName() + ".mul")); //$NON-NLS-1$
        int returnVal = saveList.showSaveDialog(mainPanel);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File unitFile = saveList.getSelectedFile();

        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(".mul") //$NON-NLS-1$
            || unitFile.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + ".mul"); //$NON-NLS-1$
                } catch (IOException ie) {
                    // nothing needs to be done here
                    return;
                }
            }

            try {
                // Save the player's entities to the file.
                // FIXME: this is not working
                EntityListFile.saveTo(unitFile, chosen);

            } catch (IOException excep) {
                excep.printStackTrace(System.err);
            }
        }

        refreshServicedUnitList();
        refreshUnitList();
        refreshPatientList();
        refreshPersonnelList();
        refreshOverview();

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(
                    getFrame(),
                    "The following units could not be deployed:"
                            + undeployed.toString(),
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refreshServicedUnitList() {
        int selected = servicedUnitTable.getSelectedRow();
        servicedUnitModel.setData(getCampaign().getServiceableUnits());
        if (selected == servicedUnitTable.getRowCount()) {
            selected--;
        }
        if ((selected > -1) && (selected < servicedUnitTable.getRowCount())) {
            servicedUnitTable.setRowSelectionInterval(selected, selected);
        }
        refreshRating();
    }

    public void refreshPersonnelList() {
        UUID selectedUUID = null;
        int selectedRow = personnelTable.getSelectedRow();
        if (selectedRow != -1) {
            Person p = personModel.getPerson(personnelTable
                    .convertRowIndexToModel(selectedRow));
            if (null != p) {
                selectedUUID = p.getId();
            }
        }
        personModel.refreshData();
        // try to put the focus back on same person if they are still available
        for (int row = 0; row < personnelTable.getRowCount(); row++) {
            Person p = personModel.getPerson(personnelTable
                    .convertRowIndexToModel(row));
            if (p.getId().equals(selectedUUID)) {
                personnelTable.setRowSelectionInterval(row, row);
                refreshPersonnelView();
                break;
            }
        }
        refreshRating();
    }

    public void changeMission() {
        int idx = choiceMission.getSelectedIndex();
        btnEditMission.setEnabled(false);
        btnCompleteMission.setEnabled(false);
        btnDeleteMission.setEnabled(false);
        btnAddScenario.setEnabled(false);
        if (idx >= 0 && idx < getCampaign().getSortedMissions().size()) {
            Mission m = getCampaign().getSortedMissions().get(idx);
            if (null != m) {
                selectedMission = m.getId();
                if (getCampaign().getCampaignOptions().getUseAtB()
                        && m instanceof AtBContract) {
                    scrollMissionView.setViewportView(new AtBContractViewPanel(
                            (AtBContract) m, getCampaign()));
                } else if (m instanceof Contract) {
                    scrollMissionView.setViewportView(new ContractViewPanel(
                            (Contract) m));
                } else {
                    scrollMissionView.setViewportView(new MissionViewPanel(m));
                }
                // This odd code is to make sure that the scrollbar stays at the
                // top
                // I can't just call it here, because it ends up getting reset
                // somewhere later
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        scrollMissionView.getVerticalScrollBar().setValue(0);
                    }
                });
                btnEditMission.setEnabled(true);
                btnCompleteMission.setEnabled(m.isActive());
                btnDeleteMission.setEnabled(true);
                btnAddScenario.setEnabled(m.isActive());
            }

        } else {
            selectedMission = -1;
            scrollMissionView.setViewportView(null);
        }
        refreshScenarioList();
    }

    public void refreshScenarioList() {
        Mission m = getCampaign().getMission(selectedMission);
        if (null != m) {
            scenarioModel.setData(m.getScenarios());
        } else {
            scenarioModel.setData(new ArrayList<Scenario>());
        }
    }

    public void refreshUnitList() {
        UUID selectedUUID = null;
        int selectedRow = getUnitTable().getSelectedRow();
        if (selectedRow != -1) {
            Unit u = getUnitModel().getUnit(getUnitTable()
                    .convertRowIndexToModel(selectedRow));
            if (null != u) {
                selectedUUID = u.getId();
            }
        }
        getUnitModel().setData(getCampaign().getUnits());
        // try to put the focus back on same person if they are still available
        for (int row = 0; row < getUnitTable().getRowCount(); row++) {
            Unit u = getUnitModel().getUnit(getUnitTable().convertRowIndexToModel(row));
            if (u.getId().equals(selectedUUID)) {
                getUnitTable().setRowSelectionInterval(row, row);
                refreshUnitView();
                break;
            }
        }
        acquireUnitsModel
                .setData(getCampaign().getShoppingList().getUnitList());
        refreshLab();
        refreshRating();
    }

    public void refreshTaskList() {
        UUID uuid = null;
        if (null != getSelectedServicedUnit()) {
            uuid = getSelectedServicedUnit().getId();
        }
        taskModel.setData(getCampaign().getPartsNeedingServiceFor(uuid));

        if (getSelectedServicedUnit() != null
                && getSelectedServicedUnit().getEntity() != null) {
            int index = choiceLocation.getSelectedIndex();
            int numLocations = choiceLocation.getModel().getSize();
            choiceLocation.removeAllItems();
            choiceLocation.addItem("All");
            for (String s : getSelectedServicedUnit().getEntity()
                    .getLocationAbbrs()) {
                choiceLocation.addItem(s);
            }
            choiceLocation.setSelectedIndex(0);
            if (index > -1 && choiceLocation.getModel().getSize() == numLocations) {
                choiceLocation.setSelectedIndex(index);
            }
            choiceLocation.setEnabled(true);
        } else {
            choiceLocation.removeAllItems();
            choiceLocation.setEnabled(false);
        }
    }

    public void refreshAcquireList() {
        UUID uuid = null;
        if (null != getSelectedServicedUnit()) {
            uuid = getSelectedServicedUnit().getId();
        }
        acquireModel.setData(getCampaign().getAcquisitionsForUnit(uuid));
    }

    public void refreshMissions() {
        choiceMission.removeAllItems();
        for (Mission m : getCampaign().getSortedMissions()) {
            String desc = m.getName();
            if (!m.isActive()) {
                desc += " (Complete)";
            }
            choiceMission.addItem(desc);
            if (m.getId() == selectedMission) {
                choiceMission.setSelectedItem(m.getName());
            }
        }
        if (choiceMission.getSelectedIndex() == -1
                && getCampaign().getSortedMissions().size() > 0) {
            selectedMission = getCampaign().getSortedMissions().get(0).getId();
            choiceMission.setSelectedIndex(0);
        }
        changeMission();
        refreshRating();
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            refreshLanceAssignments();
        }
    }

    public void refreshLab() {
        if (null == getPanMekLab()) {
            return;
        }
        Unit u = getPanMekLab().getUnit();
        if (null == u) {
            return;
        }
        if (null == getCampaign().getUnit(u.getId())) {
            // this unit has been removed so clear the mek lab
            getPanMekLab().clearUnit();
        } else {
            // put a try-catch here so that bugs in the meklab don't screw up
            // other stuff
            try {
                getPanMekLab().refreshSummary();
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public void refreshTechsList() {
        int selected = techTable.getSelectedRow();
        techsModel.setData(getCampaign().getTechs(true, null));
        if ((selected > -1)
                && (selected < getCampaign().getTechs(true, null).size())) {
            techTable.setRowSelectionInterval(selected, selected);
        }
        String astechString = "<html><b>Astech Pool Minutes:</> "
                + getCampaign().getAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            astechString += " [" + getCampaign().getAstechPoolOvertime()
                    + " overtime]";
        }
        astechString += " (" + getCampaign().getNumberAstechs()
                + " Astechs)</html>";
        astechPoolLabel.setText(astechString); // NOI18N
        astechPoolLabelWarehouse.setText(astechString); // NOI18N
    }

    public void refreshDoctorsList() {
        int selected = docTable.getSelectedRow();
        doctorsModel.setData(getCampaign().getDoctors());
        if ((selected > -1) && (selected < getCampaign().getDoctors().size())) {
            docTable.setRowSelectionInterval(selected, selected);
        }
    }

    public void refreshPatientList() {
        Person doctor = getSelectedDoctor();
        ArrayList<Person> assigned = new ArrayList<Person>();
        ArrayList<Person> unassigned = new ArrayList<Person>();
        for (Person patient : getCampaign().getPatients()) {
            // Knock out inactive doctors
            if (patient != null
                    && patient.getDoctorId() != null
                    && getCampaign().getPerson(patient.getDoctorId()) != null
                    && getCampaign().getPerson(patient.getDoctorId())
                            .isInActive()) {
                patient.setDoctorId(null, getCampaign().getCampaignOptions()
                        .getNaturalHealingWaitingPeriod());
            }
            if (patient.getDoctorId() == null) {
                unassigned.add(patient);
            } else if (doctor != null
                    && patient.getDoctorId().equals(doctor.getId())) {
                assigned.add(patient);
            }
        }
        int assignedIdx = listAssignedPatient.getSelectedIndex();
        int unassignedIdx = listUnassignedPatient.getSelectedIndex();
        assignedPatientModel.setData(assigned);
        unassignedPatientModel.setData(unassigned);
        if (assignedIdx < assignedPatientModel.getSize()) {
            listAssignedPatient.setSelectedIndex(assignedIdx);
        } else {
            listAssignedPatient.setSelectedIndex(-1);
        }
        if (unassignedIdx < unassignedPatientModel.getSize()) {
            listUnassignedPatient.setSelectedIndex(unassignedIdx);
        } else {
            listUnassignedPatient.setSelectedIndex(-1);
        }

    }

    public void refreshPartsList() {
        partsModel.setData(getCampaign().getSpareParts());
        getCampaign().getShoppingList().removeZeroQuantityFromList(); // To
                                                                      // prevent
                                                                      // zero
                                                                      // quantity
                                                                      // from
                                                                      // hanging
                                                                      // around
        acquirePartsModel
                .setData(getCampaign().getShoppingList().getPartList());
    }

    public void refreshFinancialTransactions() {
        financeModel.setData(getCampaign().getFinances().getAllTransactions());
        loanModel.setData(getCampaign().getFinances().getAllLoans());
        refreshFunds();
        refreshRating();
        refreshFinancialReport();
    }

    public void refreshFinancialReport() {
        areaNetWorth.setText(getCampaign().getFinancialReport());
        areaNetWorth.setCaretPosition(0);
    }

    public void refreshCalendar() {
        getFrame().setTitle(getCampaign().getTitle());
    }

    public void refreshReport() {
        panLog.refreshLog(getCampaign().getCurrentReportHTML());
        // scrLog.setViewportView(panLog);
        logDialog.refreshLog();
    }

    public void refreshOrganization() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                orgTree.updateUI();
                // This seems like bad juju since it makes it annoying as hell to
                // add multiple units to a force if it's de-selected every single time
                // So, commenting it out - ralgith
                // orgTree.setSelectionPath(null);
                refreshForceView();
                if (getCampaign().getCampaignOptions().getUseAtB()) {
                    refreshLanceAssignments();
                }
            }
        });
        refreshRating();
    }

    public void refreshFunds() {
        long funds = getCampaign().getFunds();
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        String inDebt = "";
        if (getCampaign().getFinances().isInDebt()) {
            inDebt = " <font color='red'>(in Debt)</font>";
        }
        String text = "<html><b>Funds:</b> " + numberFormat.format(funds)
                + " C-Bills" + inDebt + "</html>";
        lblFunds.setText(text);
    }

    protected void refreshRating() {
        if (getCampaign().getCampaignOptions().useDragoonRating()) {
            String text = "<html><b>Dragoons Rating:</b> "
                    + getCampaign().getUnitRating() + "</html>";
            lblRating.setText(text);
        } else {
            lblRating.setText("");
        }
    }

    protected void refreshTempAstechs() {
        String text = "<html><b>Temp Astechs:</b> "
                + getCampaign().getAstechPool() + "</html>";
        lblTempAstechs.setText(text);
    }

    protected void refreshTempMedics() {
        String text = "<html><b>Temp Medics:</b> "
                + getCampaign().getMedicPool() + "</html>";
        lblTempMedics.setText(text);
    }

    public void refreshLocation() {
        lblLocation.setText(getCampaign().getLocation().getReport(
                getCampaign().getCalendar().getTime()));
    }

    protected ArrayList<Person> getSelectedUnassignedPatients() {
        ArrayList<Person> patients = new ArrayList<Person>();
        int[] indices = listUnassignedPatient.getSelectedIndices();
        if (unassignedPatientModel.getSize() == 0) {
            return patients;
        }
        for (int idx : indices) {
            patients.add((Person) unassignedPatientModel.getElementAt(idx));
        }
        return patients;
    }

    protected ArrayList<Person> getSelectedAssignedPatients() {
        ArrayList<Person> patients = new ArrayList<Person>();
        int[] indices = listAssignedPatient.getSelectedIndices();
        if (assignedPatientModel.getSize() == 0) {
            return patients;
        }
        for (int idx : indices) {
            patients.add((Person) assignedPatientModel.getElementAt(idx));
        }
        return patients;
    }

    protected void updateAssignDoctorEnabled() {
        Person doctor = getSelectedDoctor();
        if (!getSelectedUnassignedPatients().isEmpty() && null != doctor
                && getCampaign().getPatientsFor(doctor) < 25) {
            btnAssignDoc.setEnabled(true);
        } else {
            btnAssignDoc.setEnabled(false);
        }
        btnUnassignDoc.setEnabled(!getSelectedAssignedPatients().isEmpty());
    }

    protected void updateTechTarget() {
        TargetRoll target = null;
        if (acquireSelected()) {
            IAcquisitionWork acquire = getSelectedAcquisition();
            if (null != acquire) {
                Person admin = getCampaign().getLogisticsPerson();
                target = getCampaign().getTargetForAcquisition(acquire, admin);
            }
        } else {
            Part part = getSelectedTask();
            if (null != part) {
                Unit u = part.getUnit();
                Person tech = getSelectedTech();
                if (null != u && u.isSelfCrewed()) {
                    tech = u.getEngineer();
                    if (null == tech) {
                        target = new TargetRoll(TargetRoll.IMPOSSIBLE,
                                "You must have a crew assigned to large vessels to attempt repairs.");
                    }
                }
                if (null != tech) {
                    boolean wasNull = false;
                    // Temporarily set the Team ID if it isn't already.
                    // This is needed for the Clan Tech flag
                    if (part.getAssignedTeamId() == null) {
                        part.setTeamId(tech.getId());
                        wasNull = true;
                    }
                    target = getCampaign().getTargetFor(part, tech);
                    if (wasNull) { // If it was null, make it null again
                        part.setTeamId(null);
                    }
                }
            }
            ((TechSorter) techSorter.getComparator(0)).clearPart();
        }
        JButton btn = btnDoTask;
        JTextArea text = textTarget;
        JLabel lbl = lblTargetNum;
        if (onWarehouseTab()) {
            btn = btnDoTaskWarehouse;
            text = textTargetWarehouse;
            lbl = lblTargetNumWarehouse;
        }
        if (null != target) {
            btn.setEnabled(target.getValue() != TargetRoll.IMPOSSIBLE);
            text.setText(target.getDesc());
            lbl.setText(target.getValueAsString());
        } else {
            btn.setEnabled(false);
            text.setText("");
            lbl.setText("-");
        }
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            int numBonusParts = 0;
            if (acquireSelected() && null != getSelectedAcquisition()) {
                AtBContract contract = getCampaign().getAttachedAtBContract(
                        getSelectedAcquisition().getUnit());
                if (null == contract) {
                    numBonusParts = getCampaign().totalBonusParts();
                } else {
                    numBonusParts = contract.getNumBonusParts();
                }
            }
            if (numBonusParts > 0) {
                btnUseBonusPart.setText("Use Bonus Part (" + numBonusParts
                        + ")");
                btnUseBonusPart.setVisible(true);
            } else {
                btnUseBonusPart.setVisible(false);
            }
        }
    }

    public void filterTasks() {
        RowFilter<TaskTableModel, Integer> taskLocationFilter = null;
        final String loc = (String) choiceLocation.getSelectedItem();
        taskLocationFilter = new RowFilter<TaskTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends TaskTableModel, ? extends Integer> entry) {
                TaskTableModel taskModel = entry.getModel();
                Part part = taskModel.getTaskAt(entry.getIdentifier());
                if (part == null) {
                    return false;
                }
                if (loc != null && !loc.isEmpty()) {
                    if (loc.equals("All")) {
                        return true;
                    } else if (part.getLocation() == part.getUnit().getEntity()
                            .getLocationFromAbbr(loc)) {
                        return true;
                    } else if ((part instanceof EnginePart || part instanceof MissingEnginePart)
                            && part.getUnit() != null
                            && part.getUnit().getEntity() != null
                            && part.getUnit().getEntity() instanceof Mech) {
                        if (part.getUnit().getEntity().getLocationFromAbbr(loc) == Mech.LOC_CT) {
                            return true;
                        }
                        boolean needsSideTorso = false;
                        if (part instanceof EnginePart) {
                            switch (((EnginePart) part).getEngine()
                                    .getEngineType()) {
                                case Engine.XL_ENGINE:
                                case Engine.LIGHT_ENGINE:
                                case Engine.XXL_ENGINE:
                                    needsSideTorso = true;
                                    break;
                            }
                        } else if (part instanceof MissingEnginePart) {
                            switch (((EnginePart) part).getEngine()
                                    .getEngineType()) {
                                case Engine.XL_ENGINE:
                                case Engine.LIGHT_ENGINE:
                                case Engine.XXL_ENGINE:
                                    needsSideTorso = true;
                                    break;
                            }
                        }
                        if (needsSideTorso
                                && (part.getUnit().getEntity()
                                        .getLocationFromAbbr(loc) == Mech.LOC_LT || part
                                        .getUnit().getEntity()
                                        .getLocationFromAbbr(loc) == Mech.LOC_RT)) {
                            return true;
                        }
                    } else if (part instanceof MekGyro
                            && part.getUnit().getEntity()
                                    .getLocationFromAbbr(loc) == Mech.LOC_CT) {
                        return true;
                    } else {
                        return false;
                    }
                }
                return true;
            }
        };
        taskSorter.setRowFilter(taskLocationFilter);
    }

    public void filterPersonnel() {
        RowFilter<PersonnelTableModel, Integer> personTypeFilter = null;
        final int nGroup = choicePerson.getSelectedIndex();
        personTypeFilter = new RowFilter<PersonnelTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                PersonnelTableModel personModel = entry.getModel();
                Person person = personModel.getPerson(entry.getIdentifier());
                int type = person.getPrimaryRole();
                if ((nGroup == PG_ACTIVE)
                        || (nGroup == PG_COMBAT && type <= Person.T_SPACE_GUNNER)
                        || (nGroup == PG_SUPPORT && type > Person.T_SPACE_GUNNER)
                        || (nGroup == PG_MW && type == Person.T_MECHWARRIOR)
                        || (nGroup == PG_CREW && (type == Person.T_GVEE_DRIVER
                                || type == Person.T_NVEE_DRIVER
                                || type == Person.T_VTOL_PILOT || type == Person.T_VEE_GUNNER))
                        || (nGroup == PG_PILOT && type == Person.T_AERO_PILOT)
                        || (nGroup == PG_CPILOT && type == Person.T_CONV_PILOT)
                        || (nGroup == PG_PROTO && type == Person.T_PROTO_PILOT)
                        || (nGroup == PG_BA && type == Person.T_BA)
                        || (nGroup == PG_SOLDIER && type == Person.T_INFANTRY)
                        || (nGroup == PG_VESSEL && (type == Person.T_SPACE_PILOT
                                || type == Person.T_SPACE_CREW
                                || type == Person.T_SPACE_GUNNER || type == Person.T_NAVIGATOR))
                        || (nGroup == PG_TECH && type >= Person.T_MECH_TECH && type < Person.T_DOCTOR)
                        || (nGroup == PG_DOC && ((type == Person.T_DOCTOR) || (type == Person.T_MEDIC)))
                        || (nGroup == PG_ADMIN && type > Person.T_MEDIC)) {
                    return person.isActive();
                } else if (nGroup == PG_RETIRE) {
                    return person.getStatus() == Person.S_RETIRED;
                } else if (nGroup == PG_MIA) {
                    return person.getStatus() == Person.S_MIA;
                } else if (nGroup == PG_KIA) {
                    return person.getStatus() == Person.S_KIA;
                }
                return false;
            }
        };
        personnelSorter.setRowFilter(personTypeFilter);
    }

    public void filterParts() {
        RowFilter<PartsTableModel, Integer> partsTypeFilter = null;
        final int nGroup = choiceParts.getSelectedIndex();
        final int nGroupView = choicePartsView.getSelectedIndex();
        partsTypeFilter = new RowFilter<PartsTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends PartsTableModel, ? extends Integer> entry) {
                PartsTableModel partsModel = entry.getModel();
                Part part = partsModel.getPartAt(entry.getIdentifier());
                boolean inGroup = false;
                boolean inView = false;

                // Check grouping
                if (nGroup == SG_ALL) {
                    inGroup = true;
                } else if (nGroup == SG_ARMOR) {
                    inGroup = (part instanceof Armor
                            || part instanceof ProtomekArmor || part instanceof BaArmor);
                } else if (nGroup == SG_SYSTEM) {
                    inGroup = part instanceof MekGyro
                            || part instanceof EnginePart
                            || part instanceof MekActuator
                            || part instanceof MekLifeSupport
                            || part instanceof MekSensor;
                } else if (nGroup == SG_EQUIP) {
                    inGroup = part instanceof EquipmentPart;
                } else if (nGroup == SG_LOC) {
                    inGroup = part instanceof MekLocation
                            || part instanceof TankLocation;
                } else if (nGroup == SG_WEAP) {
                    inGroup = part instanceof EquipmentPart
                            && ((EquipmentPart) part).getType() instanceof WeaponType;
                } else if (nGroup == SG_AMMO) {
                    inGroup = part instanceof EquipmentPart
                            && !(part instanceof AmmoBin)
                            && ((EquipmentPart) part).getType() instanceof AmmoType;
                } else if (nGroup == SG_AMMO_BIN) {
                    inGroup = part instanceof EquipmentPart
                            && (part instanceof AmmoBin)
                            && ((EquipmentPart) part).getType() instanceof AmmoType;
                } else if (nGroup == SG_MISC) {
                    inGroup = part instanceof EquipmentPart
                            && ((EquipmentPart) part).getType() instanceof MiscType;
                } else if (nGroup == SG_ENGINE) {
                    inGroup = part instanceof EnginePart;
                } else if (nGroup == SG_GYRO) {
                    inGroup = part instanceof MekGyro;
                } else if (nGroup == SG_ACT) {
                    inGroup = part instanceof MekActuator;
                }

                // Check view
                if (nGroupView == SV_ALL) {
                    inView = true;
                } else if (nGroupView == SV_IN_TRANSIT) {
                    inView = !part.isPresent();
                } else if (nGroupView == SV_RESERVED) {
                    inView = part.isReservedForRefit() || part.isReservedForReplacement();
                } else if (nGroupView == SV_UNDAMAGED) {
                    inView = !part.needsFixing();
                } else if (nGroupView == SV_DAMAGED) {
                    inView = part.needsFixing();
                }
                return (inGroup && inView);
            }
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    public void filterUnits() {
        RowFilter<UnitTableModel, Integer> unitTypeFilter = null;
        final int nGroup = choiceUnit.getSelectedIndex() - 1;
        unitTypeFilter = new RowFilter<UnitTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends UnitTableModel, ? extends Integer> entry) {
                if (nGroup < 0) {
                    return true;
                }
                UnitTableModel unitModel = entry.getModel();
                Unit unit = unitModel.getUnit(entry.getIdentifier());
                Entity en = unit.getEntity();
                int type = -1;
                if (null != en) {
                    type = UnitType.determineUnitTypeCode(en);
                }
                return type == nGroup;
            }
        };
        unitSorter.setRowFilter(unitTypeFilter);
    }

    public void filterTechs(boolean warehouse) {
        RowFilter<TechTableModel, Integer> techTypeFilter = null;
        final Part part = getSelectedTask();
        techTypeFilter = new RowFilter<TechTableModel, Integer>() {
            @Override
            public boolean include(
                    Entry<? extends TechTableModel, ? extends Integer> entry) {
                if (acquireSelected()) {
                    return false;
                }
                if (null == part) {
                    return false;
                }
                if (!part.needsFixing() && !part.isSalvaging()) {
                    return false;
                }
                TechTableModel techModel = entry.getModel();
                Person tech = techModel.getTechAt(entry.getIdentifier());
                if (!onWarehouseTab() && !tech.isRightTechTypeFor(part)
                        && !btnShowAllTechs.isSelected()) {
                    return false;
                }
                if (onWarehouseTab() && !tech.isRightTechTypeFor(part)
                        && !btnShowAllTechsWarehouse.isSelected()) {
                    return false;
                }
                Skill skill = tech.getSkillForWorkingOn(part);
                int modePenalty = Modes.getModeExperienceReduction(part
                        .getMode());
                if (skill == null) {
                    return false;
                }
                if (part.getSkillMin() > SkillType.EXP_ELITE) {
                    return false;
                }
                return (getCampaign().getCampaignOptions().isDestroyByMargin() || part
                        .getSkillMin() <= (skill.getExperienceLevel() - modePenalty));
            }
        };
        if (warehouse) {
            whTechSorter.setRowFilter(techTypeFilter);
        } else {
            if (getCampaign().getCampaignOptions().useAssignedTechFirst()) {
                ((TechSorter) techSorter.getComparator(0)).setPart(part);
            }
            techSorter.setRowFilter(techTypeFilter);
        }
    }

    private void changePersonnelView() {

        int view = choicePersonView.getSelectedIndex();
        XTableColumnModel columnModel = (XTableColumnModel) personnelTable
                .getColumnModel();
        personnelTable.setRowHeight(15);

        // set the renderer
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(personModel.getRenderer(
                    choicePersonView.getSelectedIndex() == PV_GRAPHIC,
                    getIconPackage()));
            if (i == PersonnelTableModel.COL_RANK) {
                if (view == PV_GRAPHIC) {
                    column.setPreferredWidth(125);
                    column.setHeaderValue("Person");
                } else {
                    column.setPreferredWidth(personModel.getColumnWidth(i));
                    column.setHeaderValue("Rank");
                }
            }
        }

        if (view == PV_GRAPHIC) {
            personnelTable.setRowHeight(80);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_GENERAL) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    getCampaign().getCampaignOptions().payForSalaries());
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), true);
        } else if (view == PV_PILOT) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_INF) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_TACTIC) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_TECH) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_ADMIN) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_FLUFF) {
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_CALL), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_AGE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GENDER),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SKILL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_FORCE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_MECH),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_AERO),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SPACE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NVEE),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_VTOL),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_ARTY),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TACTICS),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_ADMIN),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_TOUGH),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_EDGE),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_NABIL),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_NIMP),
                            false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_HITS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    false);
            columnModel
                    .setColumnVisible(
                            columnModel
                                    .getColumnByModelIndex(PersonnelTableModel.COL_KILLS),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        }
    }

    private void changeUnitView() {

        int view = choiceUnitView.getSelectedIndex();
        XTableColumnModel columnModel = (XTableColumnModel) getUnitTable()
                .getColumnModel();
        getUnitTable().setRowHeight(15);

        // set the renderer
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(getUnitModel().getRenderer(
                    choiceUnitView.getSelectedIndex() == UV_GRAPHIC,
                    getIconPackage()));
            if (i == UnitTableModel.COL_WCLASS) {
                if (view == UV_GRAPHIC) {
                    column.setPreferredWidth(125);
                    column.setHeaderValue("Unit");
                } else {
                    column.setPreferredWidth(personModel.getColumnWidth(i));
                    column.setHeaderValue("Weight Class");
                }
            }
        }

        if (view == UV_GRAPHIC) {
            getUnitTable().setRowHeight(80);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME),
                    false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_COST),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PILOT),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW),
                    false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_BV),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PARTS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE),
                    false);
        } else if (view == UV_GENERAL) {
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME),
                    true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_COST),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PILOT),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW),
                    false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_BV),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PARTS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE),
                    false);
        } else if (view == UV_DETAILS) {
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME),
                    true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WEIGHT), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_COST),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PILOT),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW),
                    false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_BV),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PARTS),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUIRKS), true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE),
                    false);
        } else if (view == UV_STATUS) {
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME),
                    true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE),
                    true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WCLASS), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_COST),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_MAINTAIN),
                    getCampaign().getCampaignOptions().payForMaintain());
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUALITY), true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PILOT),
                            false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW),
                    true);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_BV),
                    false);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_REPAIR), true);
            columnModel
                    .setColumnVisible(columnModel
                            .getColumnByModelIndex(UnitTableModel.COL_PARTS),
                            true);
            columnModel.setColumnVisible(columnModel
                    .getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(
                    columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE),
                    true);
        }
    }

    protected MekHQ getApplication() {
        return app;
    }

    public Campaign getCampaign() {
        return getApplication().getCampaign();
    }

    public IconPackage getIconPackage() {
        return getApplication().getIconPackage();
    }

    public JFrame getFrame() {
        return frame;
    }

    public CampaignGUI getCampaignGUI() {
        return this;
    }

    protected boolean repairsSelected() {
        return tabTasks.getSelectedIndex() == 0;
    }

    protected boolean acquireSelected() {
        return tabTasks.getSelectedIndex() == 1 && !onWarehouseTab();
    }

    protected boolean onWarehouseTab() {
        return tabMain.getTitleAt(tabMain.getSelectedIndex()).equals(
                resourceMap.getString("panSupplies.TabConstraints.tabTitle"));
    }

    public int getTabIndexByName(String tabTitle) {
        int retVal = -1;
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            if (tabMain.getTitleAt(i).equals(tabTitle)) {
                retVal = i;
                break;
            }
        }
        return retVal;
    }

    public class TaskTableMouseAdapter extends MouseInputAdapter implements
            ActionListener {

        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();
            Part part = taskModel.getTaskAt(taskTable
                    .convertRowIndexToModel(taskTable.getSelectedRow()));
            if (null == part) {
                return;
            }
            if (command.equalsIgnoreCase("SCRAP")) {
                if (null != part.checkScrappable()) {
                    JOptionPane.showMessageDialog(frame,
                            part.checkScrappable(), "Cannot scrap part",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Unit u = part.getUnit();
                getCampaign().addReport(part.scrap());
                if (null != u && !u.isRepairable()
                        && u.getSalvageableParts().size() == 0) {
                    getCampaign().removeUnit(u.getId());
                }
                refreshServicedUnitList();
                refreshUnitList();
                refreshTaskList();
                refreshUnitView();
                refreshPartsList();
                refreshAcquireList();
                refreshReport();
                refreshOverview();
                filterTasks();
            } else if (command.contains("SWAP_AMMO")) {
                /*
                 * WorkItem task =
                 * taskModel.getTaskAt(TaskTable.getSelectedRow()); if (task
                 * instanceof ReloadItem) { ReloadItem reload = (ReloadItem)
                 * task; Entity en = reload.getUnit().getEntity(); Mounted m =
                 * reload.getMounted(); if (null == m) { return; } AmmoType
                 * curType = (AmmoType) m.getType(); String sel =
                 * command.split(":")[1]; int selType = Integer.parseInt(sel);
                 * AmmoType newType = Utilities.getMunitionsFor(en, curType)
                 * .get(selType); reload.swapAmmo(newType); refreshTaskList();
                 * refreshUnitView(); refreshAcquireList(); }
                 */
            } else if (command.contains("CHANGE_MODE")) {
                String sel = command.split(":")[1];
                int selected = Integer.parseInt(sel);
                part.setMode(selected);
                refreshServicedUnitList();
                refreshUnitList();
                refreshTaskList();
                refreshUnitView();
                refreshAcquireList();
                refreshOverview();
            } else if (command.contains("UNASSIGN")) {
                /*
                 * for (WorkItem task : tasks) { task.resetTimeSpent();
                 * task.setTeam(null); refreshServicedUnitList();
                 * refreshUnitList(); refreshTaskList(); refreshAcquireList();
                 * refreshCargo(); refreshOverview(); }
                 */
            } else if (command.contains("FIX")) {
                /*
                 * for (WorkItem task : tasks) { if (task.checkFixable() ==
                 * null) { if ((task instanceof ReplacementItem) &&
                 * !((ReplacementItem) task).hasPart()) { ReplacementItem
                 * replace = (ReplacementItem) task; Part part =
                 * replace.partNeeded(); replace.setPart(part);
                 * getCampaign().addPart(part); } task.succeed(); if
                 * (task.isCompleted()) { getCampaign().removeTask(task); } }
                 * refreshServicedUnitList(); refreshUnitList();
                 * refreshTaskList(); refreshAcquireList(); refreshPartsList();
                 * refreshCargo(); refreshOverview(); }
                 */
                if (part.checkFixable() == null) {
                    getCampaign().addReport(part.succeed());

                    refreshServicedUnitList();
                    refreshUnitList();
                    refreshTaskList();
                    refreshAcquireList();
                    refreshPartsList();
                    refreshOverview();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                int row = taskTable.getSelectedRow();
                if (row < 0) {
                    return;
                }
                Part part = taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
                JMenuItem menuItem = null;
                JMenu menu = null;
                JCheckBoxMenuItem cbMenuItem = null;
                // Mode (extra time, rush job, ...
                // dont allow automatic success jobs to change mode
                if (part.getAllMods().getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    menu = new JMenu("Mode");
                    for (int i = 0; i < Modes.MODE_N; i++) {
                        cbMenuItem = new JCheckBoxMenuItem(Modes.getModeName(i));
                        if (part.getMode() == i) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("CHANGE_MODE:" + i);
                            cbMenuItem.addActionListener(this);
                        }
                        cbMenuItem.setEnabled(!part.isBeingWorkedOn());
                        menu.add(cbMenuItem);
                    }
                    popup.add(menu);
                }
                // Scrap component
                if (!part.canNeverScrap()) {
                    menuItem = new JMenuItem("Scrap component");
                    menuItem.setActionCommand("SCRAP");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(!part.isBeingWorkedOn());
                    popup.add(menuItem);
                }

                menu = new JMenu("GM Mode");
                popup.add(menu);
                // Auto complete task

                menuItem = new JMenuItem("Complete Task");
                menuItem.setActionCommand("FIX");
                menuItem.addActionListener(this);
                menuItem.setEnabled(getCampaign().isGM()
                        && (null == part.checkFixable()));
                menu.add(menuItem);

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    public class AcquisitionTableMouseAdapter extends MouseInputAdapter
            implements ActionListener {
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();
            IAcquisitionWork acquisitionWork = acquireModel
                    .getAcquisitionAt(acquisitionTable
                            .convertRowIndexToModel(acquisitionTable
                                    .getSelectedRow()));
            if (acquisitionWork instanceof AmmoBin) {
                acquisitionWork = ((AmmoBin) acquisitionWork)
                        .getAcquisitionWork();
            }
            if (null == acquisitionWork) {
                return;
            }
            if (command.contains("FIX")) {
                getCampaign().addReport(acquisitionWork.find(0));

                refreshServicedUnitList();
                refreshUnitList();
                refreshTaskList();
                refreshAcquireList();
                refreshPartsList();
                refreshOverview();
                filterTasks();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                int row = acquisitionTable.getSelectedRow();
                if (row < 0) {
                    return;
                }
                JMenuItem menuItem = null;
                JMenu menu = null;
                menu = new JMenu("GM Mode");
                popup.add(menu);
                // Auto complete task

                menuItem = new JMenuItem("Complete Task");
                menuItem.setActionCommand("FIX");
                menuItem.addActionListener(this);
                menuItem.setEnabled(getCampaign().isGM());
                menu.add(menuItem);

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    public void undeployUnit(Unit u) {
        Force f = getCampaign().getForce(u.getForceId());
        if (f != null) {
            undeployForce(f, false);
        }
        getCampaign().getScenario(u.getScenarioId()).removeUnit(u.getId());
        u.undeploy();
    }

    public void undeployForce(Force f) {
        undeployForce(f, true);
    }

    public void undeployForce(Force f, boolean killSubs) {
        int sid = f.getScenarioId();
        Scenario scenario = getCampaign().getScenario(sid);
        if (null != f && null != scenario) {
            f.clearScenarioIds(getCampaign(), killSubs);
            scenario.removeForce(f.getId());
            if (killSubs) {
                for (UUID uid : f.getAllUnits()) {
                    Unit u = getCampaign().getUnit(uid);
                    if (null != u) {
                        scenario.removeUnit(u.getId());
                        u.undeploy();
                    }
                }
            }

            // We have to clear out the parents as well.
            Force parent = f;
            int prevId = f.getId();
            while ((parent = parent.getParentForce()) != null) {
                if (parent.getScenarioId() == -1) {
                    break;
                }
                parent.clearScenarioIds(getCampaign(), false);
                scenario.removeForce(parent.getId());
                for (Force sub : parent.getSubForces()) {
                    if (sub.getId() == prevId) {
                        continue;
                    }
                    scenario.addForces(sub.getId());
                    sub.setScenarioId(scenario.getId());
                }
                prevId = parent.getId();
            }
        }
    }

    public Person[] getSelectedPeople() {
        Person[] selected = new Person[personnelTable.getSelectedRowCount()];
        int[] rows = personnelTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            Person person = personModel.getPerson(personnelTable
                    .convertRowIndexToModel(rows[i]));
            selected[i] = person;
        }
        return selected;
    }

    /**
     * @return the unitModel
     */
    public UnitTableModel getUnitModel() {
        return unitModel;
    }

    /**
     * @return the unitTable
     */
    public JTable getUnitTable() {
        return unitTable;
    }

    /**
     * @return the panMekLab
     */
    public MekLabPanel getPanMekLab() {
        return panMekLab;
    }

    /**
     * @return the splitUnit
     */
    public JSplitPane getSplitUnit() {
        return splitUnit;
    }

    public JTabbedPane getTabMain() {
        return tabMain;
    }

    /**
     * @return the resourceMap
     */
    public ResourceBundle getResourceMap() {
        return resourceMap;
    }

    /**
     * @return the loanTable
     */
    public JTable getLoanTable() {
        return loanTable;
    }

    /**
     * @return the loanModel
     */
    public LoanTableModel getLoanModel() {
        return loanModel;
    }

    /**
     * @return the financeTable
     */
    public JTable getFinanceTable() {
        return financeTable;
    }

    /**
     * @return the scenarioModel
     */
    public ScenarioTableModel getScenarioModel() {
        return scenarioModel;
    }

    /**
     * @return the scenarioTable
     */
    public JTable getScenarioTable() {
        return scenarioTable;
    }

    /**
     * @return the selectedMission
     */
    public int getSelectedMission() {
        return selectedMission;
    }

    /**
     * @return the personnelTable
     */
    public JTable getPersonnelTable() {
        return personnelTable;
    }

    /**
     * @return the personModel
     */
    public PersonnelTableModel getPersonnelModel() {
        return personModel;
    }

    /**
     * @return the splitPersonnel
     */
    public JSplitPane getSplitPersonnel() {
        return splitPersonnel;
    }

    /**
     * @return the partsTable
     */
    public JTable getPartsTable() {
        return partsTable;
    }

    /**
     * @return the partsModel
     */
    public PartsTableModel getPartsModel() {
        return partsModel;
    }

    /**
     * @return the servicedUnitTable
     */
    public JTable getServicedUnitTable() {
        return servicedUnitTable;
    }

    /**
     * @return the servicedUnitModel
     */
    public UnitTableModel getServicedUnitModel() {
        return servicedUnitModel;
    }
}
