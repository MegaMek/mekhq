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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import mekhq.MekHqConstants;
import mekhq.campaign.finances.Money;
import mekhq.gui.dialog.*;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import chat.ChatClient;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.swing.GameOptionsDialog;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.event.AssetEvent;
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.DayEndingEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.LoanEvent;
import mekhq.campaign.event.LocationChangedEvent;
import mekhq.campaign.event.MedicPoolChangedEvent;
import mekhq.campaign.event.MissionEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.TransactionEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.RatingReport;
import mekhq.campaign.report.Report;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.gui.model.PartsTableModel;
import mekhq.io.FileType;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {
    private static final long serialVersionUID = -687162569841072579L;

    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;
    // the max quantity when mass purchasing parts, hiring, etc. using the JSpinner
    public static final int MAX_QUANTITY_SPINNER = 1000;

    private JFrame frame;

    private MekHQ app;

    private ResourceBundle resourceMap;

    /* for the main panel */
    private JTabbedPane tabMain;

    /* For the menu bar */
    private JMenuBar menuBar;
    private JMenu menuThemes;
    private JMenuItem miContractMarket;
    private JMenuItem miUnitMarket;
    private JMenuItem miShipSearch;
    private JMenuItem miRetirementDefectionDialog;
    private JMenuItem miAdvanceMultipleDays;

    private EnumMap<GuiTabType, CampaignGuiTab> standardTabs;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblLocation;
    private JLabel lblFunds;
    private JLabel lblTempAstechs;
    private JLabel lblTempMedics;
    private JLabel lblPartsAvailabilityRating;
    @SuppressWarnings("unused")
    private JLabel lblCargo; // FIXME: Re-add this in an optionized form

    /* for the top button panel */
    private JPanel btnPanel;
    private JToggleButton btnGMMode;
    private JToggleButton btnOvertime;
    private JButton btnAdvanceDay;

    ReportHyperlinkListener reportHLL;

    private boolean logNagActive = false;

    public CampaignGUI(MekHQ app) {
        this.app = app;
        reportHLL = new ReportHyperlinkListener(this);
        standardTabs = new EnumMap<>(GuiTabType.class);
        initComponents();
        MekHQ.registerHandler(this);
        setUserPreferences();
    }

    public void showAboutBox() {
        MekHQAboutBox aboutBox = new MekHQAboutBox(getFrame());
        aboutBox.setLocationRelativeTo(getFrame());
        aboutBox.setModal(true);
        aboutBox.setVisible(true);
        aboutBox.dispose();
    }

    private void showHistoricalDailyReportDialog() {
        HistoricalDailyReportDialog histDailyReportDialog = new HistoricalDailyReportDialog(getFrame(), this);
        histDailyReportDialog.setModal(true);
        histDailyReportDialog.setVisible(true);
        histDailyReportDialog.dispose();
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
    }

    /**
     * Show a dialog indicating that the user must resolve overdue loans before advanching the day
     */
    public void showOverdueLoansDialog() {

        JOptionPane.showMessageDialog(null, "You must resolve overdue loans before advancing the day",
                "Overdue loans", JOptionPane.WARNING_MESSAGE);

    }

    public void showAdvanceMultipleDays(boolean isHost) {
        miAdvanceMultipleDays.setVisible(isHost);
    }

    public void showGMToolsDialog() {
        GMToolsDialog gmTools = new GMToolsDialog(getFrame(), this);
        gmTools.setVisible(true);
    }

    public void showMassMothballDialog(Unit[] units, boolean activate) {
        MassMothballDialog mothballDialog = new MassMothballDialog(getFrame(), units, getCampaign(), activate);
        mothballDialog.setVisible(true);
    }

    public void showAdvanceDaysDialog() {
        AdvanceDaysDialog advanceDaysDialog = new AdvanceDaysDialog(getFrame(), this);
        advanceDaysDialog.setModal(true);
        advanceDaysDialog.setVisible(true);
        advanceDaysDialog.dispose();
    }

    public void randomizeAllBloodnames() {
        for (Person p : getCampaign().getPersonnel()) {
            getCampaign().checkBloodnameAdd(p, false);
        }
    }

    public void spendBatchXP() {
        BatchXPDialog batchXPDialog = new BatchXPDialog(getFrame(), getCampaign());
        batchXPDialog.setVisible(true);
    }

    public void showBloodnameDialog() {
        final int year = getCampaign().getGameYear();
        BloodnameDialog bloodnameDialog = new BloodnameDialog(getFrame(),
                getCampaign().getFaction().getFullName(year), year);

        bloodnameDialog.setVisible(true);
    }

    private void initComponents() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", new EncodeControl()); //$NON-NLS-1$

        frame = new JFrame("MekHQ"); //$NON-NLS-1$
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabMain = new JTabbedPane();
        tabMain.setToolTipText(resourceMap.getString("tabMain.toolTipText")); // NOI18N
        tabMain.setMinimumSize(new java.awt.Dimension(600, 200));
        tabMain.setPreferredSize(new java.awt.Dimension(900, 300));

        addStandardTab(GuiTabType.COMMAND);
        addStandardTab(GuiTabType.TOE);
        addStandardTab(GuiTabType.BRIEFING);
        addStandardTab(GuiTabType.MAP);
        addStandardTab(GuiTabType.PERSONNEL);
        addStandardTab(GuiTabType.HANGAR);
        addStandardTab(GuiTabType.WAREHOUSE);
        addStandardTab(GuiTabType.REPAIR);
        addStandardTab(GuiTabType.INFIRMARY);
        addStandardTab(GuiTabType.MEKLAB);
        addStandardTab(GuiTabType.FINANCES);

        //check to see if we just selected the command center tab
        //and if so change its color to standard
        tabMain.addChangeListener(e -> {
            if (tabMain.getSelectedIndex() == 0) {
                tabMain.setBackgroundAt(0, null);
                logNagActive = false;
            }
        });

        initTopButtons();
        initStatusBar();

        setLayout(new BorderLayout());

        add(tabMain, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.PAGE_START);
        add(statusPanel, BorderLayout.PAGE_END);

        standardTabs.values().forEach(CampaignGuiTab::refreshAll);

        refreshCalendar();
        refreshFunds();
        refreshLocation();
        refreshTempAstechs();
        refreshTempMedics();
        refreshPartsAvailability();

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

    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CampaignGUI.class);

        frame.setName("mainWindow");
        preferences.manage(new JWindowPreference(frame));
    }

    public CampaignGuiTab getTab(GuiTabType tabType) {
        return standardTabs.get(tabType);
    }

    public TOETab getTOETab() {
        return (TOETab) getTab(GuiTabType.TOE);
    }

    public BriefingTab getBriefingTab() {
        return (BriefingTab) getTab(GuiTabType.BRIEFING);
    }

    public MapTab getMapTab() {
        return (MapTab) getTab(GuiTabType.MAP);
    }

    public PersonnelTab getPersonnelTab() {
        return (PersonnelTab) getTab(GuiTabType.PERSONNEL);
    }

    public HangarTab getHangarTab() {
        return (HangarTab) getTab(GuiTabType.HANGAR);
    }

    public WarehouseTab getWarehouseTab() {
        return (WarehouseTab) getTab(GuiTabType.WAREHOUSE);
    }

    public RepairTab getRepairTab() {
        return (RepairTab) getTab(GuiTabType.REPAIR);
    }

    public MekLabTab getMekLabTab() {
        return (MekLabTab) getTab(GuiTabType.MEKLAB);
    }

    public InfirmaryTab getInfirmaryTab() {
        return (InfirmaryTab) getTab(GuiTabType.INFIRMARY);
    }

    public boolean hasTab(GuiTabType tabType) {
        return standardTabs.containsKey(tabType);
    }

    /**
     * Sets the selected tab by its {@link GuiTabType}.
     * @param tabType The type of tab to select.
     */
    public void setSelectedTab(GuiTabType tabType) {
        if (standardTabs.containsKey(tabType)) {
            CampaignGuiTab tab = standardTabs.get(tabType);
            for (int ii = 0; ii < tabMain.getTabCount(); ++ii) {
                if (tabMain.getComponentAt(ii) == tab) {
                    tabMain.setSelectedIndex(ii);
                    break;
                }
            }
        }
    }

    /**
     * Adds one of the built-in tabs to the gui, if it is not already present.
     *
     * @param tab The type of tab to add
     */
    public void addStandardTab(GuiTabType tab) {
        if (tab.equals(GuiTabType.CUSTOM)) {
            throw new IllegalArgumentException("Attempted to add custom tab as standard");
        }
        if (!standardTabs.containsKey(tab)) {
            CampaignGuiTab t = tab.createTab(this);
            if (t != null) {
                standardTabs.put(tab, t);
                int index = tabMain.getTabCount();
                for (int i = 0; i < tabMain.getTabCount(); i++) {
                    if (((CampaignGuiTab) tabMain.getComponentAt(i)).tabType().getDefaultPos() > tab.getDefaultPos()) {
                        index = i;
                        break;
                    }
                }
                tabMain.insertTab(t.getTabName(), null, t, null, index);
                tabMain.setMnemonicAt(index, tab.getMnemonic());
            }
        }
    }

    /**
     * Adds a custom tab to the gui at the end
     *
     * @param tab The tab to add
     */
    public void addCustomTab(CampaignGuiTab tab) {
        if (tabMain.indexOfComponent(tab) >= 0) {
            return;
        }
        if (tab.tabType().equals(GuiTabType.CUSTOM)) {
            tabMain.addTab(tab.getTabName(), tab);
        } else {
            addStandardTab(tab.tabType());
        }
    }

    /**
     * Adds a custom tab to the gui in the specified position. If <code>tab</code> is a built-in
     * type it will be placed in its normal position if it does not already exist.
     *
     * @param tab	The tab to add
     * @param index	The position to place the tab
     */
    public void insertCustomTab(CampaignGuiTab tab, int index) {
        if (tabMain.indexOfComponent(tab) >= 0) {
            return;
        }
        if (tab.tabType().equals(GuiTabType.CUSTOM)) {
            tabMain.insertTab(tab.getTabName(), null, tab, null, Math.min(index, tabMain.getTabCount()));
        } else {
            addStandardTab(tab.tabType());
        }
    }

    /**
     * Adds a custom tab to the gui positioned after one of the built-in tabs
     *
     * @param tab		The tab to add
     * @param stdTab	The build-in tab after which to place the new one
     */
    public void insertCustomTabAfter(CampaignGuiTab tab, GuiTabType stdTab) {
        if (tabMain.indexOfComponent(tab) >= 0) {
            return;
        }
        if (tab.tabType().equals(GuiTabType.CUSTOM)) {
            int index = tabMain.indexOfTab(stdTab.getTabName());
            if (index < 0) {
                if (stdTab.getDefaultPos() == 0) {
                    index = tabMain.getTabCount();
                } else {
                    for (int i = stdTab.getDefaultPos() - 1; i >= 0; i--) {
                        index = tabMain.indexOfTab(GuiTabType.values()[i].getTabName());
                        if (index >= 0) {
                            break;
                        }
                    }
                }
            }
            insertCustomTab(tab, index);
        } else {
            addStandardTab(tab.tabType());
        }
    }

    /**
     * Adds a custom tab to the gui positioned before one of the built-in tabs
     *
     * @param tab		The tab to add
     * @param stdTab	The build-in tab before which to place the new one
     */
    public void insertCustomTabBefore(CampaignGuiTab tab, GuiTabType stdTab) {
        if (tabMain.indexOfComponent(tab) >= 0) {
            return;
        }
        if (tab.tabType().equals(GuiTabType.CUSTOM)) {
            int index = tabMain.indexOfTab(stdTab.getTabName());
            if (index < 0) {
                if (stdTab.getDefaultPos() == GuiTabType.values().length - 1) {
                    index = tabMain.getTabCount();
                } else {
                    for (int i = stdTab.getDefaultPos() + 1; i >= GuiTabType.values().length; i++) {
                        index = tabMain.indexOfTab(GuiTabType.values()[i].getTabName());
                        if (index >= 0) {
                            break;
                        }
                    }
                }
            }
            insertCustomTab(tab, Math.max(0, index - 1));
        } else {
            addStandardTab(tab.tabType());
        }
    }

    /**
     * Removes one of the built-in tabs from the gui.
     *
     * @param tabType	The tab to remove
     */
    public void removeStandardTab(GuiTabType tabType) {
        CampaignGuiTab tab = standardTabs.get(tabType);
        if (tab != null) {
            MekHQ.unregisterHandler(tab);
            removeTab(tab);
        }
    }

    /**
     * Removes a tab from the gui.
     *
     * @param tab	The tab to remove
     */
    public void removeTab(CampaignGuiTab tab) {
        tab.disposeTab();
        removeTab(tab.getTabName());
    }

    /**
     * Removes a tab from the gui.
     *
     * @param tabName	The name of the tab to remove
     */
    public void removeTab(String tabName) {
        int index = tabMain.indexOfTab(tabName);
        if (index >= 0) {
            CampaignGuiTab tab = (CampaignGuiTab) tabMain.getComponentAt(index);
            standardTabs.remove(tab.tabType());
            tabMain.removeTabAt(index);
        }
    }

    /**
     * This is used to initialize the top menu bar.
     * All the top level menu bar and {@link GuiTabType} mnemonics must be unique, as they are both
     * accessed through the same GUI page.
     * The following mnemonic keys are being used as of 30-MAR-2020:
     * A, B, C, E, F, H, I, L, M, N, O, P, R, S, T, V, W, /
     *
     * Note 1: the slash is used for the help, as it is normally the same key as the ?
     * Note 2: the A mnemonic is used for the Advance Day button
     */
    private void initMenu() {
        // TODO: Implement "Export All" versions for Personnel and Parts
        // See the JavaDoc comment for used mnemonic keys
        menuBar = new JMenuBar();

        //region File Menu
        // The File menu uses the following Mnemonic keys as of 19-MAR-2020:
        // C, E, H, I, L, M, N, S, T, U, X
        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text")); // NOI18N
        menuFile.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuLoad = new JMenuItem(resourceMap.getString("menuLoad.text")); // NOI18N
        menuLoad.setMnemonic(KeyEvent.VK_L);
        menuLoad.addActionListener(this::menuLoadXmlActionPerformed);
        menuFile.add(menuLoad);

        JMenuItem menuSave = new JMenuItem(resourceMap.getString("menuSave.text")); // NOI18N
        menuSave.setMnemonic(KeyEvent.VK_S);
        menuSave.addActionListener(this::menuSaveXmlActionPerformed);
        menuFile.add(menuSave);

        JMenuItem menuNew = new JMenuItem(resourceMap.getString("menuNew.text"));
        menuNew.setMnemonic(KeyEvent.VK_N);
        menuNew.addActionListener(this::menuNewCampaignActionPerformed);
        menuFile.add(menuNew);

        //region menuImport
        // The Import menu uses the following Mnemonic keys as of 19-MAR-2020:
        // A, C, F, P
        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text")); // NOI18N
        menuImport.setMnemonic(KeyEvent.VK_I);

        JMenuItem miImportOptions = new JMenuItem(resourceMap.getString("miImportOptions.text")); // NOI18N
        miImportOptions.setMnemonic(KeyEvent.VK_C);
        miImportOptions.addActionListener(this::miImportOptionsActionPerformed);
        menuImport.add(miImportOptions);

        JMenuItem miImportPerson = new JMenuItem(resourceMap.getString("miImportPerson.text")); // NOI18N
        miImportPerson.setMnemonic(KeyEvent.VK_P);
        miImportPerson.addActionListener(this::miImportPersonActionPerformed);
        menuImport.add(miImportPerson);

        JMenuItem miImportParts = new JMenuItem(resourceMap.getString("miImportParts.text")); // NOI18N
        miImportParts.setMnemonic(KeyEvent.VK_A);
        miImportParts.addActionListener(this::miImportPartsActionPerformed);
        menuImport.add(miImportParts);

        JMenuItem miLoadForces = new JMenuItem(resourceMap.getString("miLoadForces.text")); // NOI18N
        miLoadForces.setMnemonic(KeyEvent.VK_F);
        miLoadForces.addActionListener(this::miLoadForcesActionPerformed);
        menuImport.add(miLoadForces);

        menuFile.add(menuImport);
        //endregion menuImport

        //region menuExport
        // The Export menu uses the following Mnemonic keys as of 19-March-2020:
        // C, X, S
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text")); // NOI18N
        menuExport.setMnemonic(KeyEvent.VK_X);

        //region CSV Export
        // The CSV menu uses the following Mnemonic keys as of 19-March-2020:
        // F, P, U
        JMenu miExportCSVFile = new JMenu(resourceMap.getString("menuExportCSV.text")); // NOI18N
        miExportCSVFile.setMnemonic(KeyEvent.VK_C);

        JMenuItem miExportPersonCSV = new JMenuItem(resourceMap.getString("miExportPersonnel.text")); // NOI18N
        miExportPersonCSV.setMnemonic(KeyEvent.VK_P);
        miExportPersonCSV.addActionListener(this::miExportPersonnelCSVActionPerformed);
        miExportCSVFile.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(resourceMap.getString("miExportUnit.text")); // NOI18N
        miExportUnitCSV.setMnemonic(KeyEvent.VK_U);
        miExportUnitCSV.addActionListener(this::miExportUnitCSVActionPerformed);
        miExportCSVFile.add(miExportUnitCSV);

        JMenuItem miExportFinancesCSV = new JMenuItem(resourceMap.getString("miExportFinances.text")); // NOI18N
        miExportFinancesCSV.setMnemonic(KeyEvent.VK_F);
        miExportFinancesCSV.addActionListener(this::miExportFinancesCSVActionPerformed);
        miExportCSVFile.add(miExportFinancesCSV);

        menuExport.add(miExportCSVFile);
        //endregion CSV Export

        //region XML Export
        // The XML menu uses the following Mnemonic keys as of 19-March-2020:
        // C, P
        JMenu miExportXMLFile = new JMenu(resourceMap.getString("menuExportXML.text")); // NOI18N
        miExportXMLFile.setMnemonic(KeyEvent.VK_X);

        JMenuItem miExportOptions = new JMenuItem(resourceMap.getString("miExportOptions.text")); // NOI18N
        miExportOptions.setMnemonic(KeyEvent.VK_C);
        miExportOptions.addActionListener(this::miExportOptionsActionPerformed);
        miExportXMLFile.add(miExportOptions);

        JMenuItem miExportPlanetsXML = new JMenuItem(resourceMap.getString("miExportPlanets.text"));
        miExportPlanetsXML.setMnemonic(KeyEvent.VK_P);
        miExportPlanetsXML.addActionListener(this::miExportPlanetsXMLActionPerformed);
        miExportXMLFile.add(miExportPlanetsXML);

        menuExport.add(miExportXMLFile);
        //endregion XML Export

        JMenuItem miExportCampaignSubset = new JMenuItem(resourceMap.getString("miExportCampaignSubset.text"));
        miExportCampaignSubset.setMnemonic(KeyEvent.VK_S);
        miExportCampaignSubset.addActionListener(evt -> {
            CampaignExportWizard cew = new CampaignExportWizard(getCampaign());
            cew.display(CampaignExportWizard.CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        menuFile.add(menuExport);
        //endregion menuExport

        JMenuItem miMercRoster = new JMenuItem(resourceMap.getString("miMercRoster.text")); // NOI18N
        miMercRoster.setMnemonic(KeyEvent.VK_U);
        miMercRoster.addActionListener(evt -> showMercRosterDialog());
        menuFile.add(miMercRoster);

        JMenuItem menuOptions = new JMenuItem(resourceMap.getString("menuOptions.text")); // NOI18N
        menuOptions.setMnemonic(KeyEvent.VK_C);
        menuOptions.addActionListener(this::menuOptionsActionPerformed);
        menuFile.add(menuOptions);

        JMenuItem menuOptionsMM = new JMenuItem(resourceMap.getString("menuOptionsMM.text")); // NOI18N
        menuOptionsMM.setMnemonic(KeyEvent.VK_M);
        menuOptionsMM.addActionListener(this::menuOptionsMMActionPerformed);
        menuFile.add(menuOptionsMM);

        JMenuItem menuMekHqOptions = new JMenuItem(resourceMap.getString("menuMekHqOptions.text"));
        menuMekHqOptions.setMnemonic(KeyEvent.VK_H);
        menuMekHqOptions.addActionListener(this::menuMekHqOptionsActionPerformed);
        menuFile.add(menuMekHqOptions);

        menuThemes = new JMenu(resourceMap.getString("menuThemes.text"));
        menuThemes.setMnemonic(KeyEvent.VK_T);
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem(resourceMap.getString("menuExit.text"));
        menuExitItem.setMnemonic(KeyEvent.VK_E);
        menuExitItem.addActionListener(evt -> getApplication().exit());
        menuFile.add(menuExitItem);

        menuBar.add(menuFile);
        //endregion File Menu

        //region Marketplace Menu
        // The Marketplace menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, C, H, M, N, P, R, S, U
        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text")); // NOI18N
        menuMarket.setMnemonic(KeyEvent.VK_M);

        JMenuItem miPersonnelMarket = new JMenuItem(resourceMap.getString("miPersonnelMarket.text"));
        miPersonnelMarket.setMnemonic(KeyEvent.VK_P);
        miPersonnelMarket.addActionListener(evt -> hirePersonMarket());
        menuMarket.add(miPersonnelMarket);

        miContractMarket = new JMenuItem(resourceMap.getString("miContractMarket.text"));
        miContractMarket.setMnemonic(KeyEvent.VK_C);
        miContractMarket.addActionListener(evt -> showContractMarket());
        miContractMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());
        menuMarket.add(miContractMarket);

        miUnitMarket = new JMenuItem(resourceMap.getString("miUnitMarket.text"));
        miUnitMarket.setMnemonic(KeyEvent.VK_U);
        miUnitMarket.addActionListener(evt -> showUnitMarket());
        miUnitMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());
        menuMarket.add(miUnitMarket);

        miShipSearch = new JMenuItem(resourceMap.getString("miShipSearch.text"));
        miShipSearch.setMnemonic(KeyEvent.VK_S);
        miShipSearch.addActionListener(ev -> showShipSearch());
        miShipSearch.setVisible(getCampaign().getCampaignOptions().getUseAtB());
        menuMarket.add(miShipSearch);

        JMenuItem miPurchaseUnit = new JMenuItem(resourceMap.getString("miPurchaseUnit.text")); // NOI18N
        miPurchaseUnit.setMnemonic(KeyEvent.VK_N);
        miPurchaseUnit.addActionListener(this::miPurchaseUnitActionPerformed);
        menuMarket.add(miPurchaseUnit);

        JMenuItem miBuyParts = new JMenuItem(resourceMap.getString("miBuyParts.text")); // NOI18N
        miBuyParts.setMnemonic(KeyEvent.VK_R);
        miBuyParts.addActionListener(evt -> buyParts());
        menuMarket.add(miBuyParts);

        JMenuItem miHireBulk = new JMenuItem(resourceMap.getString("miHireBulk.text"));
        miHireBulk.setMnemonic(KeyEvent.VK_B);
        miHireBulk.addActionListener(evt -> hireBulkPersonnel());
        menuMarket.add(miHireBulk);

        JMenu menuHire = new JMenu(resourceMap.getString("menuHire.text")); // NOI18N
        menuHire.setMnemonic(KeyEvent.VK_H);
        for (int i = Person.T_MECHWARRIOR; i < Person.T_NUM; i++) {
            JMenuItem miHire = new JMenuItem(Person.getRoleDesc(i, getCampaign().getFaction().isClan()));
            int miHireMnemonic = Person.getRoleMnemonic(i);
            if (miHireMnemonic != KeyEvent.VK_UNDEFINED) {
                miHire.setMnemonic(miHireMnemonic);
            }
            miHire.setActionCommand(Integer.toString(i));
            miHire.addActionListener(this::hirePerson);
            menuHire.add(miHire);
        }
        menuMarket.add(menuHire);

        //region Astech Pool
        // The Astech Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, F, H
        JMenu menuAstechPool = new JMenu(resourceMap.getString("menuAstechPool.text"));
        menuAstechPool.setMnemonic(KeyEvent.VK_A);

        JMenuItem miHireAstechs = new JMenuItem(resourceMap.getString("miHireAstechs.text"));
        miHireAstechs.setMnemonic(KeyEvent.VK_H);
        miHireAstechs.addActionListener(evt -> {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    getFrame(), true, resourceMap.getString("popupHireAstechsNum.text"),
                    1, 0, CampaignGUI.MAX_QUANTITY_SPINNER);
            pvcd.setVisible(true);
            if (pvcd.getValue() >= 0) {
                getCampaign().increaseAstechPool(pvcd.getValue());
            }
        });
        menuAstechPool.add(miHireAstechs);

        JMenuItem miFireAstechs = new JMenuItem(resourceMap.getString("miFireAstechs.text"));
        miFireAstechs.setMnemonic(KeyEvent.VK_E);
        miFireAstechs.addActionListener(evt -> {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    getFrame(), true, resourceMap.getString("popupFireAstechsNum.text"),
                    1, 0, getCampaign().getAstechPool());
            pvcd.setVisible(true);
            if (pvcd.getValue() >= 0) {
                getCampaign().decreaseAstechPool(pvcd.getValue());
            }
        });
        menuAstechPool.add(miFireAstechs);

        JMenuItem miFullStrengthAstechs = new JMenuItem(resourceMap.getString("miFullStrengthAstechs.text"));
        miFullStrengthAstechs.setMnemonic(KeyEvent.VK_B);
        miFullStrengthAstechs.addActionListener(evt -> {
            int need = (getCampaign().getTechs().size() * 6) - getCampaign().getNumberAstechs();
            if (need > 0) {
                getCampaign().increaseAstechPool(need);
            }
        });
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = new JMenuItem(resourceMap.getString("miFireAllAstechs.text"));
        miFireAllAstechs.setMnemonic(KeyEvent.VK_R);
        miFireAllAstechs.addActionListener(evt -> getCampaign().decreaseAstechPool(getCampaign().getAstechPool()));
        menuAstechPool.add(miFireAllAstechs);
        menuMarket.add(menuAstechPool);
        //endregion Astech Pool

        //region Medic Pool
        // The Medic Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, H, R
        JMenu menuMedicPool = new JMenu(resourceMap.getString("menuMedicPool.text"));
        menuMedicPool.setMnemonic(KeyEvent.VK_M);

        JMenuItem miHireMedics = new JMenuItem(resourceMap.getString("miHireMedics.text"));
        miHireMedics.setMnemonic(KeyEvent.VK_H);
        miHireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    getFrame(), true, resourceMap.getString("popupHireMedicsNum.text"),
                    1, 0, CampaignGUI.MAX_QUANTITY_SPINNER);
            pvcd.setVisible(true);
            if (pvcd.getValue() >= 0) {
                getCampaign().increaseMedicPool(pvcd.getValue());
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = new JMenuItem(resourceMap.getString("miFireMedics.text"));
        miFireMedics.setMnemonic(KeyEvent.VK_E);
        miFireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog pvcd = new PopupValueChoiceDialog(
                    getFrame(), true, resourceMap.getString("popupFireMedicsNum.text"),
                    1, 0, getCampaign().getMedicPool());
            pvcd.setVisible(true);
            if (pvcd.getValue() >= 0) {
                getCampaign().decreaseMedicPool(pvcd.getValue());
            }
        });
        menuMedicPool.add(miFireMedics);

        JMenuItem miFullStrengthMedics = new JMenuItem(resourceMap.getString("miFullStrengthMedics.text"));
        miFullStrengthMedics.setMnemonic(KeyEvent.VK_B);
        miFullStrengthMedics.addActionListener(evt -> {
            int need = (getCampaign().getDoctors().size() * 4) - getCampaign().getNumberMedics();
            if (need > 0) {
                getCampaign().increaseMedicPool(need);
            }
        });
        menuMedicPool.add(miFullStrengthMedics);

        JMenuItem miFireAllMedics = new JMenuItem(resourceMap.getString("miFireAllMedics.text"));
        miFireAllMedics.setMnemonic(KeyEvent.VK_R);
        miFireAllMedics.addActionListener(evt -> getCampaign().decreaseMedicPool(getCampaign().getMedicPool()));
        menuMedicPool.add(miFireAllMedics);
        menuMarket.add(menuMedicPool);
        //endregion Medic Pool

        menuBar.add(menuMarket);
        //endregion Marketplace Menu

        //region Reports Menu
        // The Reports menu uses the following Mnemonic keys as of 19-March-2020:
        // C, H, P, T, U
        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text")); // NOI18N
        menuReports.setMnemonic(KeyEvent.VK_E);

        JMenuItem miDragoonsRating = new JMenuItem(resourceMap.getString("miDragoonsRating.text")); // NOI18N
        miDragoonsRating.setMnemonic(KeyEvent.VK_U);
        miDragoonsRating.addActionListener(evt -> showReport(new RatingReport(getCampaign())));
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(resourceMap.getString("miPersonnelReport.text")); // NOI18N
        miPersonnelReport.setMnemonic(KeyEvent.VK_P);
        miPersonnelReport.addActionListener(evt -> showReport(new PersonnelReport(getCampaign())));
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(resourceMap.getString("miHangarBreakdown.text")); // NOI18N
        miHangarBreakdown.setMnemonic(KeyEvent.VK_H);
        miHangarBreakdown.addActionListener(evt -> showReport(new HangarReport(getCampaign())));
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(resourceMap.getString("miTransportReport.text")); // NOI18N
        miTransportReport.setMnemonic(KeyEvent.VK_T);
        miTransportReport.addActionListener(evt -> showReport(new TransportReport(getCampaign())));
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(resourceMap.getString("miCargoReport.text")); // NOI18N
        miCargoReport.setMnemonic(KeyEvent.VK_C);
        miCargoReport.addActionListener(evt -> showReport(new CargoReport(getCampaign())));
        menuReports.add(miCargoReport);

        menuBar.add(menuReports);
        //endregion Reports Menu

        //region Community Menu
        // The Community menu uses the following Mnemonic keys as of 19-March-2020:
        // C
        JMenu menuCommunity = new JMenu(resourceMap.getString("menuCommunity.text")); // NOI18N
        //menuCommunity.setMnemonic(KeyEvent.VK_?); // This will need to be replaced with a unique mnemonic key if this menu is ever added

        JMenuItem miChat = new JMenuItem(resourceMap.getString("miChat.text")); // NOI18N
        miChat.setMnemonic(KeyEvent.VK_C);
        miChat.addActionListener(this::miChatActionPerformed);
        menuCommunity.add(miChat);

        // menuBar.add(menuCommunity);
        //endregion Community Menu

        //region View Menu
        // The View menu uses the following Mnemonic keys as of 02-June-2020:
        // B, H, R
        JMenu menuView = new JMenu(resourceMap.getString("menuView.text")); // NOI18N
        menuView.setMnemonic(KeyEvent.VK_V);

        JMenuItem miHistoricalDailyReportDialog = new JMenuItem(resourceMap.getString("miShowHistoricalReportLog.text")); // NOI18N
        miHistoricalDailyReportDialog.setMnemonic(KeyEvent.VK_H);
        miHistoricalDailyReportDialog.addActionListener(evt -> showHistoricalDailyReportDialog());
        menuView.add(miHistoricalDailyReportDialog);

        JMenuItem miBloodnameDialog = new JMenuItem(resourceMap.getString("miBloodnameDialog.text"));
        miBloodnameDialog.setMnemonic(KeyEvent.VK_B);
        miBloodnameDialog.addActionListener(evt -> showBloodnameDialog());
        menuView.add(miBloodnameDialog);

        miRetirementDefectionDialog = new JMenuItem(resourceMap.getString("miRetirementDefectionDialog.text"));
        miRetirementDefectionDialog.setMnemonic(KeyEvent.VK_R);
        miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().getUseAtB());
        miRetirementDefectionDialog.addActionListener(evt -> showRetirementDefectionDialog());
        menuView.add(miRetirementDefectionDialog);

        menuBar.add(menuView);
        //endregion View Menu

        //region Manage Campaign Menu
        // The Manage Campaign menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, G, M, S
        JMenu menuManage = new JMenu(resourceMap.getString("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_C);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = new JMenuItem(resourceMap.getString("miGMToolsDialog.text"));
        miGMToolsDialog.setMnemonic(KeyEvent.VK_G);
        miGMToolsDialog.addActionListener(evt -> showGMToolsDialog());
        menuManage.add(miGMToolsDialog);

        miAdvanceMultipleDays = new JMenuItem(resourceMap.getString("miAdvanceMultipleDays.text"));
        miAdvanceMultipleDays.setMnemonic(KeyEvent.VK_A);
        miAdvanceMultipleDays.addActionListener(evt -> showAdvanceDaysDialog());
        miAdvanceMultipleDays.setVisible(getCampaignController().isHost());
        menuManage.add(miAdvanceMultipleDays);

        JMenuItem miBloodnames = new JMenuItem(resourceMap.getString("miRandomBloodnames.text"));
        miBloodnames.setMnemonic(KeyEvent.VK_B);
        miBloodnames.addActionListener(evt -> randomizeAllBloodnames());
        menuManage.add(miBloodnames);

        JMenuItem miBatchXP = new JMenuItem(resourceMap.getString("miBatchXP.text"));
        miBatchXP.setMnemonic(KeyEvent.VK_M);
        miBatchXP.addActionListener(evt -> spendBatchXP());
        menuManage.add(miBatchXP);

        JMenuItem miScenarioEditor = new JMenuItem(resourceMap.getString("miScenarioEditor.text"));
        miScenarioEditor.setMnemonic(KeyEvent.VK_S);
        miScenarioEditor.addActionListener(evt -> {
            ScenarioTemplateEditorDialog sted = new ScenarioTemplateEditorDialog(getFrame());
            sted.setVisible(true);
        });
        menuManage.add(miScenarioEditor);

        menuBar.add(menuManage);
        //endregion Manage Campaign Menu

        //region Help Menu
        // The Help menu uses the following Mnemonic keys as of 19-March-2020:
        // A
        JMenu menuHelp = new JMenu(resourceMap.getString("menuHelp.text")); // NOI18N
        menuHelp.setMnemonic(KeyEvent.VK_SLASH);
        menuHelp.setName("helpMenu"); // NOI18N

        JMenuItem menuAboutItem = new JMenuItem(resourceMap.getString("menuAbout.text"));
        menuAboutItem.setMnemonic(KeyEvent.VK_A);
        menuAboutItem.setName("aboutMenuItem");
        menuAboutItem.addActionListener(evt -> showAboutBox());
        menuHelp.add(menuAboutItem);

        menuBar.add(menuHelp);
        //endregion Help Menu
    }

    private void initStatusBar() {
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));

        lblFunds = new JLabel();
        lblTempAstechs = new JLabel();
        lblTempMedics = new JLabel();
        lblPartsAvailabilityRating = new JLabel();

        statusPanel.add(lblFunds);
        statusPanel.add(lblTempAstechs);
        statusPanel.add(lblTempMedics);
        statusPanel.add(lblPartsAvailabilityRating);
    }

    private void initTopButtons() {
        GridBagConstraints gridBagConstraints;

        lblLocation = new JLabel(getCampaign().getLocation().getReport(getCampaign().getLocalDate())); // NOI18N

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
        btnGMMode.setToolTipText(resourceMap.getString("btnGMMode.toolTipText")); // NOI18N
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(e -> getCampaign().setGMMode(btnGMMode.isSelected()));
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

        btnOvertime = new JToggleButton(resourceMap.getString("btnOvertime.text")); // NOI18N
        btnOvertime.setToolTipText(resourceMap.getString("btnOvertime.toolTipText")); // NOI18N
        btnOvertime.addActionListener(this::btnOvertimeActionPerformed);
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

        // This button uses a mnemonic that is unique and listed in the initMenu JavaDoc
        btnAdvanceDay = new JButton(resourceMap.getString("btnAdvanceDay.text")); // NOI18N
        btnAdvanceDay.setToolTipText(resourceMap.getString("btnAdvanceDay.toolTipText")); // NOI18N
        btnAdvanceDay.addActionListener(evt -> getCampaignController().advanceDay());
        btnAdvanceDay.setMnemonic(KeyEvent.VK_A);
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
            Method method = clazz.getMethod(methodName, Window.class, boolean.class);
            method.invoke(null, window, true);
        } catch (Throwable t) {
            MekHQ.getLogger().error(CampaignGUI.class, "enableFullScreenMode",
                    "Full screen mode is not supported", t);
        }
    }

    private static boolean isMacOSX() {
        return System.getProperty("os.name").contains("Mac OS X");
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

    private void changeTheme(java.awt.event.ActionEvent evt) {
        MekHQ.getSelectedTheme().setValue(evt.getActionCommand());
        refreshThemeChoices();
    }

    private void refreshThemeChoices() {
        menuThemes.removeAll();
        JCheckBoxMenuItem miPlaf;
        for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            miPlaf = new JCheckBoxMenuItem(laf.getName());
            if (laf.getClassName().equalsIgnoreCase(MekHQ.getSelectedTheme().getValue())) {
                miPlaf.setSelected(true);
            }

            menuThemes.add(miPlaf);
            miPlaf.setActionCommand(laf.getClassName());
            miPlaf.addActionListener(this::changeTheme);
        }
    }
    //TODO: trigger from event
    public void filterTasks() {
        if (getTab(GuiTabType.REPAIR) != null) {
            ((RepairTab)getTab(GuiTabType.REPAIR)).filterTasks();
        }
    }

    public void focusOnUnit(UUID id) {
        HangarTab ht = (HangarTab)getTab(GuiTabType.HANGAR);
        if (null == id || null == ht) {
            return;
        }
        ht.focusOnUnit(id);
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap
                .getString("panHangar.TabConstraints.tabTitle")));
    }

    public void focusOnUnitInRepairBay(UUID id) {
        if (null == id) {
            return;
        }
        if (getTab(GuiTabType.REPAIR) != null) {
            ((RepairTab)getTab(GuiTabType.REPAIR)).focusOnUnit(id);
            tabMain.setSelectedComponent(getTab(GuiTabType.REPAIR));
        }
    }

    public void focusOnPerson(UUID id) {
        if (null == id) {
            return;
        }
        PersonnelTab pt = (PersonnelTab)getTab(GuiTabType.PERSONNEL);
        if (pt == null) {
            return;
        }
        pt.focusOnPerson(id);
        tabMain.setSelectedComponent(pt);
    }

    public void showNews(int id) {
        NewsItem news = getCampaign().getNews().getNewsItem(id);
        if (null != news) {
            NewsReportDialog nrd = new NewsReportDialog(frame, news);
            nrd.setVisible(true);
        }
    }

    public boolean nagShortMaintenance() {
        if (!getCampaign().getCampaignOptions().checkMaintenance()) {
            return false;
        }
        Vector<Unit> notMaintained = new Vector<>();
        int totalAstechMinutesNeeded = 0;
        for (Unit u : getCampaign().getUnits()) {
            if (u.requiresMaintenance() && (u.getTech() == null)) {
                notMaintained.add(u);
            } else if (u.isPresent() && (u.getEngineer() == null)) {
                // only add astech minutes for non-crewed units who are present
                totalAstechMinutesNeeded += (u.getMaintenanceTime() * 6);
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
            return JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
                    "You do not have enough astechs to provide for full maintenance. You need "
                            + needed + " more astech(s). Do you wish to proceed?",
                    "Astech shortage", JOptionPane.YES_NO_OPTION);
        }

        return false;
    }

    public boolean nagShortDeployments() {
        if (getCampaign().getLocalDate().getDayOfWeek() != DayOfWeek.SUNDAY) {
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
                if (getCampaign().getLocalDate().equals(s.getDate())) {
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

    private void hirePerson(java.awt.event.ActionEvent evt) {
        int type = Integer.parseInt(evt.getActionCommand());
        NewRecruitDialog npd = new NewRecruitDialog(this, true,
                getCampaign().newPerson(type));
        npd.setVisible(true);
    }

    public void hirePersonMarket() {
        PersonnelMarketDialog pmd = new PersonnelMarketDialog(getFrame(), this,
                getCampaign(), getIconPackage().getPortraits());
        pmd.setVisible(true);
    }

    private void hireBulkPersonnel() {
        HireBulkPersonnelDialog hbpd = new HireBulkPersonnelDialog(getFrame(),
                true, getCampaign());
        hbpd.setVisible(true);
    }

    public void showContractMarket() {
        ContractMarketDialog cmd = new ContractMarketDialog(getFrame(), getCampaign());
        cmd.setVisible(true);
    }

    public void showUnitMarket() {
        UnitMarketDialog umd = new UnitMarketDialog(getFrame(), getCampaign());
        umd.setVisible(true);
    }

    public void showShipSearch() {
        ShipSearchDialog ssd = new ShipSearchDialog(getFrame(), this);
        ssd.setVisible(true);
    }

    private void menuSaveXmlActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_menuSaveActionPerformed
        final String METHOD_NAME = "menuSaveXmlActionPerformed(ActionEvent)";

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Saving campaign..."); //$NON-NLS-1$
        // Choose a file...
        File file = selectSaveCampaignFile();
        if (file == null) {
            // I want a file, y'know!
            return;
        }

        saveCampaign(getFrame(), getCampaign(), file);
    }

    /**
     * Attempts to saves the given campaign to the given file.
     * @param frame The parent frame in which to display the error message. May be null.
     */
    public static boolean saveCampaign(JFrame frame, Campaign campaign, File file) {
        final String METHOD_NAME = "saveCampaign(Campaign campaign, File file)";
        String path = file.getPath();
        if (!path.endsWith(".cpnx") && !path.endsWith(".cpnx.gz")) {
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
        FileOutputStream fos;
        OutputStream os = null;
        PrintWriter pw;

        try {
            os = fos = new FileOutputStream(file);
            if (path.endsWith(".gz")) {
                os = new GZIPOutputStream(fos);
            }
            os = new BufferedOutputStream(os);
            pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            campaign.writeToXml(pw);
            pw.flush();
            pw.close();
            os.close();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(CampaignGUI.class, METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Campaign saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            if (os != null) {
                try { os.close(); } catch (Exception ignored) { }
            }
            MekHQ.getLogger().error(CampaignGUI.class, METHOD_NAME, ex); //$NON-NLS-1$
            JOptionPane
                    .showMessageDialog(
                            frame,
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

            return false;
        }

        return true;
    }

    private File selectSaveCampaignFile() {
        return FileDialogs.saveCampaign(frame, getCampaign()).orElse(null);
    }

    private String getExtensionForSaveFile(Campaign c) {
        return MekHQ.getMekHQOptions().getPreferGzippedOutput() ? ".cpnx.gz" : ".cpnx";
    }

    private void menuLoadXmlActionPerformed(java.awt.event.ActionEvent evt) {
        File f = selectLoadCampaignFile();
        if (null == f) {
            return;
        }
        boolean hadAtB = getCampaign().getCampaignOptions().getUseAtB();
        DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(
                getApplication(), getFrame(), f);
        // TODO: does this effectively deal with memory management issues?
        dataLoadingDialog.setVisible(true);
        if (hadAtB && !getCampaign().getCampaignOptions().getUseAtB()) {
            RandomFactionGenerator.getInstance().dispose();
            RandomUnitGenerator.getInstance().dispose();
        }
        //Unregister event handlers for CampaignGUI and tabs
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            if (tabMain.getComponentAt(i) instanceof CampaignGuiTab) {
                ((CampaignGuiTab)tabMain.getComponentAt(i)).disposeTab();
            }
        }
        MekHQ.unregisterHandler(this);
    }

    private File selectLoadCampaignFile() {
        return FileDialogs.openCampaign(frame).orElse(null);
    }

    private void menuNewCampaignActionPerformed(ActionEvent e) {
        DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(app, frame, null);
        dataLoadingDialog.setVisible(true);
    }

    private void btnOvertimeActionPerformed(java.awt.event.ActionEvent evt) {
        getCampaign().setOvertime(btnOvertime.isSelected());
    }

    private void menuOptionsActionPerformed(java.awt.event.ActionEvent evt) {
        boolean atb = getCampaign().getCampaignOptions().getUseAtB();
        boolean timeIn = getCampaign().getCampaignOptions().getUseTimeInService();
        boolean rankIn = getCampaign().getCampaignOptions().getUseTimeInRank();
        boolean retirementDateTracking = getCampaign().getCampaignOptions().useRetirementDateTracking();
        boolean staticRATs = getCampaign().getCampaignOptions().useStaticRATs();
        boolean factionIntroDate = getCampaign().getCampaignOptions().useFactionIntroDate();
        CampaignOptionsDialog cod = new CampaignOptionsDialog(getFrame(), true,
                getCampaign(), getIconPackage());
        cod.setVisible(true);
        if (timeIn != getCampaign().getCampaignOptions().getUseTimeInService()) {
            if (getCampaign().getCampaignOptions().getUseTimeInService()) {
                getCampaign().initTimeInService();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setRecruitment(null);
                }
            }
        }

        if (rankIn != getCampaign().getCampaignOptions().getUseTimeInRank()) {
            if (getCampaign().getCampaignOptions().getUseTimeInRank()) {
                getCampaign().initTimeInRank();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setLastRankChangeDate(null);
                }
            }
        }

        if (retirementDateTracking != getCampaign().getCampaignOptions().useRetirementDateTracking()) {
            if (getCampaign().getCampaignOptions().useRetirementDateTracking()) {
                getCampaign().initRetirementDateTracking();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setRetirement(null);
                }
            }
        }

        if (atb != getCampaign().getCampaignOptions().getUseAtB()) {
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                getCampaign().initAtB(false);
                //refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign().getForces()));
            }
            miContractMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());
            miUnitMarket.setVisible(getCampaign().getCampaignOptions().getUseAtB());
            miShipSearch.setVisible(getCampaign().getCampaignOptions().getUseAtB());
            miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().getUseAtB());
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                int loops = 0;
                while (!RandomUnitGenerator.getInstance().isInitialized()) {
                    try {
                        Thread.sleep(50);
                        if (++loops > 20) {
                            // Wait for up to a second
                            break;
                        }
                    } catch (InterruptedException ignore) {
                    }
                }
            } else {
                getCampaign().shutdownAtB();
            }
        }
        if (staticRATs != getCampaign().getCampaignOptions().useStaticRATs()) {
            getCampaign().initUnitGenerator();
        }
        if (factionIntroDate != getCampaign().getCampaignOptions().useFactionIntroDate()) {
            getCampaign().updateTechFactionCode();
        }
        refreshCalendar();
        getCampaign().reloadNews();
    }

    private void menuOptionsMMActionPerformed(java.awt.event.ActionEvent evt) {
        GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
        god.refreshOptions();
        god.setEditable(true);
        god.setVisible(true);
        if (!god.wasCancelled()) {
            getCampaign().setGameOptions(god.getOptions());
            setCampaignOptionsFromGameOptions();
            refreshCalendar();
        }
    }

    private void menuMekHqOptionsActionPerformed(ActionEvent evt) {
        MekHqOptionsDialog dialog = new MekHqOptionsDialog(getFrame());
        dialog.setVisible(true);
    }

    private void miLoadForcesActionPerformed(java.awt.event.ActionEvent evt) {
        loadListFile(true);
    }

    private void miImportPersonActionPerformed(java.awt.event.ActionEvent evt) {
        loadPersonFile();
    }

    public void miExportPersonActionPerformed(java.awt.event.ActionEvent evt) {
        savePersonFile();
    }

    private void miExportOptionsActionPerformed(java.awt.event.ActionEvent evt) {
        saveOptionsFile(FileType.XML, resourceMap.getString("dlgSaveCampaignXML.text"),
                getCampaign().getName() + getCampaign().getLocalDate().format(DateTimeFormatter.ofPattern(MekHqConstants.FILENAME_DATE_FORMAT)) + "_ExportedCampaignSettings");
    }

    private void miExportPlanetsXMLActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            exportPlanets(FileType.XML, resourceMap.getString("dlgSavePlanetsXML.text"),
                    getCampaign().getName() + getCampaign().getLocalDate().format(DateTimeFormatter.ofPattern(MekHqConstants.FILENAME_DATE_FORMAT)) + "_ExportedPlanets");
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), "miExportOptionsActionPerformed(ActionEvent)", ex);
        }
    }

    private void miExportFinancesCSVActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            exportFinances(FileType.CSV, resourceMap.getString("dlgSaveFinancesCSV.text"),
                    getCampaign().getName() + getCampaign().getLocalDate().format(DateTimeFormatter.ofPattern(MekHqConstants.FILENAME_DATE_FORMAT)) + "_ExportedFinances");
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), "miExportOptionsActionPerformed(ActionEvent)", ex);
        }
    }

    private void miExportPersonnelCSVActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            exportPersonnel(FileType.CSV, resourceMap.getString("dlgSavePersonnelCSV.text"),
                    getCampaign().getLocalDate().format(DateTimeFormatter.ofPattern(MekHqConstants.FILENAME_DATE_FORMAT)) + "_ExportedPersonnel");
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), "miExportOptionsActionPerformed(ActionEvent)", ex);
        }
    }

    private void miExportUnitCSVActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            exportUnits(FileType.CSV, resourceMap.getString("dlgSaveUnitsCSV.text"),
                    getCampaign().getName() + getCampaign().getLocalDate().format(DateTimeFormatter.ofPattern(MekHqConstants.FILENAME_DATE_FORMAT)) + "_ExportedUnits");
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), "miExportOptionsActionPerformed(ActionEvent)", ex);
        }
    }

    private void miImportOptionsActionPerformed(java.awt.event.ActionEvent evt) {
        loadOptionsFile();
    }

    private void miImportPartsActionPerformed(java.awt.event.ActionEvent evt) {
        loadPartsFile();
    }

    public void miExportPartsActionPerformed(java.awt.event.ActionEvent evt) {
        savePartsFile();
    }

    private void miPurchaseUnitActionPerformed(java.awt.event.ActionEvent evt) {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }
        AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(getFrame(), unitLoadingDialog,
                getCampaign(), true);
        usd.setVisible(true);
    }

    private void buyParts() {
        PartsStoreDialog psd = new PartsStoreDialog(true, this);
        psd.setVisible(true);
    }

    private void showMercRosterDialog() {
        MercRosterDialog mrd = new MercRosterDialog(getFrame(), true, getCampaign());
        mrd.setVisible(true);
    }

    public void refitUnit(Refit r, boolean selectModelName) {
        if (r.getOriginalEntity() instanceof Dropship || r.getOriginalEntity() instanceof Jumpship) {
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
            Map<String, Person> techHash = new HashMap<>();
            List<String> techList = new ArrayList<>();
            String skillLvl;
            int TimePerDay;

            List<Person> techs = getCampaign().getTechs();
            techs.sort(Comparator.comparingInt(Person::getPrimaryRole));
            for (Person tech : techs) {
                if (getCampaign().isWorkingOnRefit(tech) || tech.isEngineer()) {
                    continue;
                }
                if (tech.getSecondaryRole() == Person.T_MECH_TECH || tech.getSecondaryRole() == Person.T_MECHANIC || tech.getSecondaryRole() == Person.T_AERO_TECH) {
                    TimePerDay = 240 - tech.getMaintenanceTimeUsing();
                } else {
                    TimePerDay = 480 - tech.getMaintenanceTimeUsing();
                }
                skillLvl = SkillType.getExperienceLevelName(tech.getExperienceLevel(false));
                name = tech.getFullName()
                        + ", "
                        + skillLvl
                        + " "
                        + tech.getPrimaryRoleDesc()
                        + " ("
                        + getCampaign().getTargetFor(r, tech).getValueAsString()
                        + "+)"
                        + ", "
                        + tech.getMinutesLeft() + "/" + TimePerDay
                        + " minutes";
                techHash.put(name, tech);
                techList.add(name);
            }

            String s = (String) JOptionPane.showInputDialog(frame,
                    "Which tech should work on the refit?", "Select Tech",
                    JOptionPane.PLAIN_MESSAGE, null, techList.toArray(), techList.get(0));

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
        String RefitRefurbish;
        if (r.isBeingRefurbished()) {
            RefitRefurbish = "Refurbishment is a " + r.getRefitClassName() + " refit and must be done at a factory and costs 10% of the purchase price"
                    + ".\n Are you sure you want to refurbish ";
        } else {
            RefitRefurbish = "This is a " + r.getRefitClassName() + " refit. Are you sure you want to refit ";
        }
        if (0 != JOptionPane
                .showConfirmDialog(null, RefitRefurbish
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
        if (hasTab(GuiTabType.MEKLAB)) {
            ((MekLabTab)getTab(GuiTabType.MEKLAB)).clearUnit();
        }
    }

    public void showReport(Report report) {
        ReportDialog rd = new ReportDialog(getFrame(), report);
        rd.pack();
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

    public void showUnitCostReport(UUID id) {
        if (null == id) {
            return;
        }
        Unit u = getCampaign().getUnit(id);
        if (null == u) {
            return;
        }
        UnitCostReportDialog mrd = new UnitCostReportDialog(getFrame(), u);
        mrd.setVisible(true);
    }

    /**
     * Shows a dialog that lets the user select a tech for a task on a particular unit
     *
     * @param u                 The unit to be serviced, used to filter techs for skill on the unit.
     * @param desc              The description of the task
     * @param ignoreMaintenance If true, ignores the time required for maintenance tasks when displaying
     *                          the tech's time available.
     * @return                  The ID of the selected tech, or null if none is selected.
     */
    public @Nullable UUID selectTech(Unit u, String desc, boolean ignoreMaintenance) {
        String name;
        Map<String, Person> techHash = new LinkedHashMap<>();
        for (Person tech : getCampaign().getTechs()) {
            if (!tech.isMothballing() && tech.canTech(u.getEntity())) {
                int time = tech.getMinutesLeft();
                if (!ignoreMaintenance) {
                    time -= Math.max(0, tech.getMaintenanceTimeUsing());
                }
                name = tech.getFullTitle() + ", "
                        + SkillType.getExperienceLevelName(tech.getSkillForWorkingOn(u).getExperienceLevel())
                        + " (" + time + "min)";
                techHash.put(name, tech);
            }
        }
        if (techHash.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "You have no techs available.", "No Techs",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Object[] nameArray = techHash.keySet().toArray();

        String s = (String) JOptionPane.showInputDialog(frame,
                "Which tech should work on " + desc + "?", "Select Tech",
                JOptionPane.PLAIN_MESSAGE, null, nameArray, nameArray[0]);
        if (null == s) {
            return null;
        }
        return techHash.get(s).getId();
    }

    /**
     * Exports Planets to a file (CSV, XML, etc.)
     * @param format
     * @param dialogTitle
     * @param filename
     */
    protected void exportPlanets(FileType format, String dialogTitle, String filename) {
        //TODO: Fix this
        /*
        GUI.fileDialogSave(
                frame,
                dialogTitle,
                format,
                MekHQ.getPlanetsDirectory().getValue(),
                "planets." + format.getRecommendedExtension())
                .ifPresent(f -> {
                    MekHQ.getPlanetsDirectory().setValue(f.getParent());
                    File file = checkFileEnding(f, format.getRecommendedExtension());
                    checkToBackupFile(file, file.getPath());
                    String report = Planets.getInstance().exportPlanets(file.getPath(), format.getRecommendedExtension());
                    JOptionPane.showMessageDialog(mainPanel, report);
                });

        GUI.fileDialogSave(frame, dialogTitle, new File(".", "planets." + format.getRecommendedExtension()), format).ifPresent(f -> {
            File file = checkFileEnding(f, format.getRecommendedExtension());
            checkToBackupFile(file, file.getPath());
            String report = Planets.getInstance().exportPlanets(file.getPath(), format.getRecommendedExtension());
            JOptionPane.showMessageDialog(mainPanel, report);
        });
        */
    }

    /**
     * Exports Personnel to a file (CSV, XML, etc.)
     * @param format        file format to export to
     * @param dialogTitle   title of the dialog frame
     * @param filename      file name to save to
     */
    protected void exportPersonnel(FileType format, String dialogTitle, String filename) {
        if (((PersonnelTab) getTab(GuiTabType.PERSONNEL)).getPersonnelTable().getRowCount() != 0) {
            GUI.fileDialogSave(
                    frame,
                    dialogTitle,
                    format,
                    MekHQ.getPersonnelDirectory().getValue(),
                    filename + "." + format.getRecommendedExtension())
                    .ifPresent(f -> {
                        MekHQ.getPersonnelDirectory().setValue(f.getParent());
                        File file = checkFileEnding(f, format.getRecommendedExtension());
                        checkToBackupFile(file, file.getPath());
                        String report;
                        // TODO add support for xml and json export
                        if (format.equals(FileType.CSV)) {
                            report = Utilities.exportTableToCSV(((PersonnelTab) getTab(GuiTabType.PERSONNEL)).getPersonnelTable(), file);
                        } else {
                            report = "Unsupported FileType in Export Personnel";
                        }
                        JOptionPane.showMessageDialog(tabMain, report);
                    });
        } else {
            JOptionPane.showMessageDialog(tabMain, resourceMap.getString("dlgNoPersonnel.text"));
        }
    }

    /**
     * Exports Units to a file (CSV, XML, etc.)
     * @param format        file format to export to
     * @param dialogTitle   title of the dialog frame
     * @param filename      file name to save to
     */
    protected void exportUnits(FileType format, String dialogTitle, String filename) {
        if (((HangarTab) getTab(GuiTabType.HANGAR)).getUnitTable().getRowCount() != 0) {
            GUI.fileDialogSave(
                    frame,
                    dialogTitle,
                    format,
                    MekHQ.getUnitsDirectory().getValue(),
                    filename + "." + format.getRecommendedExtension())
                    .ifPresent(f -> {
                        MekHQ.getUnitsDirectory().setValue(f.getParent());
                        File file = checkFileEnding(f, format.getRecommendedExtension());
                        checkToBackupFile(file, file.getPath());
                        String report;
                        // TODO add support for xml and json export
                        if (format.equals(FileType.CSV)) {
                            report = Utilities.exportTableToCSV(((HangarTab) getTab(GuiTabType.HANGAR)).getUnitTable(), file);
                        } else {
                            report = "Unsupported FileType in Export Units";
                        }
                        JOptionPane.showMessageDialog(tabMain, report);
                    });
        } else {
            JOptionPane.showMessageDialog(tabMain, resourceMap.getString("dlgNoUnits"));
        }
    }

     /**
     * Exports Finances to a file (CSV, XML, etc.)
     * @param format        file format to export to
     * @param dialogTitle   title of the dialog frame
     * @param filename      file name to save to
     */
    protected void exportFinances(FileType format, String dialogTitle, String filename) {
        if (!getCampaign().getFinances().getAllTransactions().isEmpty()) {
            GUI.fileDialogSave(
                    frame,
                    dialogTitle,
                    format,
                    MekHQ.getFinancesDirectory().getValue(),
                    filename + "." + format.getRecommendedExtension())
                    .ifPresent(f -> {
                        MekHQ.getFinancesDirectory().setValue(f.getParent());
                        File file = checkFileEnding(f, format.getRecommendedExtension());
                        checkToBackupFile(file, file.getPath());
                        String report;
                        // TODO add support for xml and json export
                        if (format.equals(FileType.CSV)) {
                            report = getCampaign().getFinances().exportFinancesToCSV(file.getPath(),
                                    format.getRecommendedExtension());
                        } else {
                            report = "Unsupported FileType in Export Finances";
                        }
                        JOptionPane.showMessageDialog(tabMain, report);
                    });
        } else {
            JOptionPane.showMessageDialog(tabMain, resourceMap.getString("dlgNoFinances.text"));
        }
    }

    /**
     * Checks if a file already exists, if so it makes a backup copy.
     * @param file to determine if there is an existing file with that name
     * @param path path to the file
     */
    private void checkToBackupFile(File file, String path) {
        // check for existing file and make a back-up if found
        String path2 = path + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }
    }

    /**
     * Checks to make sure the file has the appropriate ending / extension.
     * @param file   the file to check
     * @param format proper format for the ending/extension
     * @return File  with the appropriate ending/ extension
     */
    private File checkFileEnding(File file, String format) {
        String path = file.getPath();
        if (!path.endsWith("." + format)) {
            path += "." + format;
            file = new File(path);
        }
        return file;
    }

    protected void loadListFile(boolean allowNewPilots) {
        final String METHOD_NAME = "loadListFile(boolean)";

        File unitFile = FileDialogs.openUnits(frame).orElse(null);

        if (unitFile != null) {
            // I need to get the parser myself, because I want to pull both
            // entities and pilots from it
            // Create an empty parser.
            MULParser parser = new MULParser();

            // Open up the file.
            try (InputStream is = new FileInputStream(unitFile)) {
                parser.parse(is);
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "loadListFile", e);
            }

            // Was there any error in parsing?
            if (parser.hasWarningMessage()) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
                        parser.getWarningMessage());
            }

            // Add the units from the file.
            for (Entity entity : parser.getEntities()) {
                getCampaign().addUnit(entity, allowNewPilots, 0);
            }

            // TODO : re-add any ejected pilots
            //for (Crew pilot : parser.getPilots()) {
            //    if (pilot.isEjected()) {
            //         getCampaign().addPilot(pilot, PilotPerson.T_MECHWARRIOR,
            //         false);
            //    }
            //}
        }
    }

    protected void loadPersonFile() {
        final String METHOD_NAME = "loadPersonFile()";

        File personnelFile = FileDialogs.openPersonnel(frame).orElse(null);

        if (personnelFile != null) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Starting load of personnel file from XML..."); //$NON-NLS-1$
            // Initialize variables.
            Document xmlDoc;

            // Open the file
            try (InputStream is = new FileInputStream(personnelFile)) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(is);
            } catch (Exception ex) {
                MekHQ.getLogger().error(getClass(), METHOD_NAME, "Cannot load person XML", ex);
                return; // otherwise we NPE out in the next line
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
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                            "Unknown node type not loaded in Personnel nodes: " //$NON-NLS-1$
                                    + wn2.getNodeName());

                    continue;
                }

                Person p = Person.generateInstanceFromXML(wn2, getCampaign(), version);
                if (getCampaign().getPerson(p.getId()) != null
                        && getCampaign().getPerson(p.getId()).getFullName().equals(p.getFullName())) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                            "ERROR: Cannot load person who exists, ignoring. (Name: " //$NON-NLS-1$
                                    + p.getFullName() + ")"); //$NON-NLS-1$
                    p = null;
                }

                if (p != null) {
                    getCampaign().recruitPerson(p, true);

                    // Clear some values we no longer should have set in case this
                    // has transferred campaigns or things in the campaign have
                    // changed...
                    p.setUnitId(null);
                    p.clearTechUnitIDs();
                }
            }

            // Fix Spouse Id Information - This is required to fix spouse NPEs where one doesn't export
            // both members of the couple
            // TODO : make it so that exports will automatically include both spouses
            for (Person p : getCampaign().getActivePersonnel()) {
                if (p.getGenealogy().hasSpouse()
                        && !getCampaign().getPersonnel().contains(p.getGenealogy().getSpouse(getCampaign()))) {
                    // If this happens, we need to clear the spouse
                    if (p.getMaidenName() != null) {
                        p.setSurname(p.getMaidenName());
                    }

                    p.getGenealogy().setSpouse(null);
                }

                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(Person.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null)
                            && !getCampaign().getPersonnel().contains(getCampaign().getPerson(fatherId))) {
                        p.getExtraData().set(Person.PREGNANCY_FATHER_DATA, null);
                    }
                }
            }

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Finished load of personnel file"); //$NON-NLS-1$
        }
    }

    //TODO: disable if not using personnel tab
    private void savePersonFile() {
        final String METHOD_NAME = "savePersonFile()";

        File file = FileDialogs.savePersonnel(frame, getCampaign()).orElse(null);
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
        try (OutputStream os = new FileOutputStream(file);
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            PersonnelTab pt = (PersonnelTab)getTab(GuiTabType.PERSONNEL);
            int row = pt.getPersonnelTable().getSelectedRow();
            if (row < 0) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
                        "ERROR: Cannot export person if no one is selected! Ignoring."); //$NON-NLS-1$
                return;
            }
            Person selectedPerson = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                    .convertRowIndexToModel(row));
            int[] rows = pt.getPersonnelTable().getSelectedRows();
            Person[] people = new Person[rows.length];
            for (int i = 0; i < rows.length; i++) {
                people[i] = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                        .convertRowIndexToModel(rows[i]));
            }

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
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Personnel saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
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

    private void saveOptionsFile(FileType format, String dialogTitle, String filename) {
        final String METHOD_NAME = "saveOptionsFile()";

        Optional<File> maybeFile = GUI.fileDialogSave(
                frame,
                dialogTitle,
                format,
                MekHQ.getCampaignOptionsDirectory().getValue(),
                filename + "." + format.getRecommendedExtension());

        if (!maybeFile.isPresent()) {
            return;
        }

        MekHQ.getCampaignOptionsDirectory().setValue(maybeFile.get().getParent());

        File file = checkFileEnding(maybeFile.get(), format.getRecommendedExtension());
        checkToBackupFile(file, file.getPath());

        // Then save it out to that file.


        try (OutputStream os = new FileOutputStream(file);
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");
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
            pw.flush();

            JOptionPane.showMessageDialog(tabMain, getResourceMap().getString("dlgCampaignSettingsSaved.text"));

            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                    "Campaign Options saved saved to " + file); //$NON-NLS-1$
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            JOptionPane
                    .showMessageDialog(
                            getFrame(),
                            "Oh no! The program was unable to correctly export your campaign options. We know this\n"
                                    + "is annoying and apologize. Please help us out and submit a bug with the\n"
                                    + "mekhqlog.txt file from this game so we can prevent this from happening in\n"
                                    + "the future.",
                            "Could not export campaign options",
                            JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void loadPartsFile() {
        final String METHOD_NAME = "loadPartsFile()";

        Optional<File> maybeFile = FileDialogs.openParts(frame);

        if (!maybeFile.isPresent()) {
            return;
        }

        File partsFile = maybeFile.get();

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Starting load of parts file from XML..."); //$NON-NLS-1$
        // Initialize variables.
        Document xmlDoc;

        // Open up the file.
        try (InputStream is = new FileInputStream(partsFile)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            return;
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
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                        "Unknown node type not loaded in Parts nodes: " //$NON-NLS-1$
                                + wn2.getNodeName());

                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                p.setCampaign(getCampaign());
                getCampaign().addPartWithoutId(p);
            }
        }
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Finished load of parts file"); //$NON-NLS-1$
    }

    protected void loadOptionsFile() {
        final String METHOD_NAME = "loadOptionsFile()";

        Optional<File> maybeFile = FileDialogs.openCampaignOptions(frame);

        if (!maybeFile.isPresent()) {
            return;
        }

        File optionsFile = maybeFile.get();

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Starting load of options file from XML..."); //$NON-NLS-1$
        // Initialize variables.
        Document xmlDoc;

        // Open up the file.
        try (InputStream is = new FileInputStream(optionsFile)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MekHqXmlUtil.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
            return;
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
                options = CampaignOptions.generateCampaignOptionsFromXml(wn);
            } else if (xn.equalsIgnoreCase("randomSkillPreferences")) {
                rsp = RandomSkillPreferences.generateRandomSkillPreferencesFromXml(wn);
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
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                                "Unknown node type not loaded in Skill Type nodes: " //$NON-NLS-1$
                                        + wn2.getNodeName());

                        continue;
                    }
                    SkillType.generateInstanceFromXML(wn2, version);
                }
            } else if (xn.equalsIgnoreCase("specialAbilities")) {
                PilotOptions pilotOptions = new PilotOptions();
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
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, //$NON-NLS-1$
                                "Unknown node type not loaded in Special Ability nodes: " //$NON-NLS-1$
                                        + wn2.getNodeName());

                        continue;
                    }

                    SpecialAbility.generateInstanceFromXML(wn2, pilotOptions, null);
                }
            }

        }

        if (null != options) {
            this.getCampaign().setCampaignOptions(options);
        }
        if (null != rsp) {
            this.getCampaign().setRandomSkillPreferences(rsp);
        }

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                "Finished load of campaign options file"); //$NON-NLS-1$
        MekHQ.triggerEvent(new OptionsChangedEvent(getCampaign(), options));

        refreshCalendar();
        getCampaign().reloadNews();
    }

    private void savePartsFile() {
        String METHOD_NAME = "savePartsFile()";

        Optional<File> maybeFile = FileDialogs.saveParts(frame, getCampaign());

        if (!maybeFile.isPresent()) {
            return;
        }

        File file = maybeFile.get();

        if (!file.getName().endsWith(".parts")) {
            file = new File(file.getAbsolutePath() + ".parts");
        }

        // check for existing file and make a back-up if found
        String path2 = file.getAbsolutePath() + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            Utilities.copyfile(file, backupFile);
        }

        // Then save it out to that file.
        FileOutputStream fos;
        PrintWriter pw;

        if (getTab(GuiTabType.WAREHOUSE) != null) {
            try {
                JTable partsTable = ((WarehouseTab)getTab(GuiTabType.WAREHOUSE)).getPartsTable();
                PartsTableModel partsModel = ((WarehouseTab)getTab(GuiTabType.WAREHOUSE)).getPartsModel();
                int row = partsTable.getSelectedRow();
                if (row < 0) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING, //$NON-NLS-1$
                            "ERROR: Cannot export parts if none are selected! Ignoring."); //$NON-NLS-1$
                    return;
                }
                Part selectedPart = partsModel.getPartAt(partsTable
                        .convertRowIndexToModel(row));
                int[] rows = partsTable.getSelectedRows();
                Part[] parts = new Part[rows.length];
                for (int i = 0; i < rows.length; i++) {
                    parts[i] = partsModel.getPartAt(partsTable.convertRowIndexToModel(rows[i]));
                }
                fos = new FileOutputStream(file);
                pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));

                // File header
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

                ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQ");
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
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, //$NON-NLS-1$
                        "Parts saved to " + file); //$NON-NLS-1$
            } catch (Exception ex) {
                MekHQ.getLogger().error(getClass(), METHOD_NAME, ex); //$NON-NLS-1$
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
    }

    /**
     * Check to see if the command center tab is currently active and if not, color the tab. Should be
     * called when items are added to daily report log panel and user is not on the command center tab
     * in order to draw attention to it
     */
    public void checkDailyLogNag() {
        if (!logNagActive) {
            if (tabMain.getSelectedIndex() != 0) {
                tabMain.setBackgroundAt(0, Color.RED);
                logNagActive = true;
            }
        }
    }

    public void refreshAllTabs() {
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            ((CampaignGuiTab)tabMain.getComponentAt(i)).refreshAll();
        }
    }

    public void refreshLab() {
        MekLabTab lab = (MekLabTab)getTab(GuiTabType.MEKLAB);
        if (null == lab) {
            return;
        }
        Unit u = lab.getUnit();
        if (null == u) {
            return;
        }
        if (null == getCampaign().getUnit(u.getId())) {
            // this unit has been removed so clear the mek lab
            lab.clearUnit();
        } else {
            // put a try-catch here so that bugs in the meklab don't screw up
            // other stuff
            try {
                lab.refreshRefitSummary();
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "refreshLab", e);
            }
        }
    }

    public void refreshCalendar() {
        getFrame().setTitle(getCampaign().getTitle());
    }

    private void refreshFunds() {
        Money funds = getCampaign().getFunds();
        String inDebt = "";
        if (getCampaign().getFinances().isInDebt()) {
            inDebt = " <font color='red'>(in Debt)</font>";
        }
        String text = "<html><b>Funds:</b> "
                + funds.toAmountAndSymbolString()
                + inDebt
                + "</html>";
        lblFunds.setText(text);
    }

    private void refreshTempAstechs() {
        String text = "<html><b>Temp Astechs:</b> " + getCampaign().getAstechPool() + "</html>";
        lblTempAstechs.setText(text);
    }

    private void refreshTempMedics() {
        String text = "<html><b>Temp Medics:</b> " + getCampaign().getMedicPool() + "</html>";
        lblTempMedics.setText(text);
    }

    private void refreshPartsAvailability() {
        if (!getCampaign().getCampaignOptions().getUseAtB()) {
            lblPartsAvailabilityRating.setText("");
        } else {
            StringBuilder report = new StringBuilder();
            int partsAvailability = getCampaign().findAtBPartsAvailabilityLevel(null, report);
            lblPartsAvailabilityRating.setText("<html><b>Campaign Parts Availability</b>:" + partsAvailability + "</html>");
        }
    }

    private ActionScheduler fundsScheduler = new ActionScheduler(this::refreshFunds);

    @Subscribe
    public void handleDayEnding(DayEndingEvent ev) {
        // first check for overdue loan payments - don't allow advancement until
        // these are addressed
        if (getCampaign().checkOverDueLoans()) {
            refreshFunds();
            showOverdueLoansDialog();
            ev.cancel();
        }
        if (getCampaign().checkRetirementDefections()) {
            showRetirementDefectionDialog();
            ev.cancel();
        }
        if (getCampaign().checkYearlyRetirements()) {
            showRetirementDefectionDialog();
            ev.cancel();
        }
        if (nagShortMaintenance()) {
            ev.cancel();
        }
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            if (nagShortDeployments()) {
                ev.cancel();
            }
            if (nagOutstandingScenarios()) {
                ev.cancel();
            }
        }
    }

    @Subscribe
    public void handleNewDay(NewDayEvent evt) {
        refreshCalendar();
        refreshLocation();
        refreshFunds();
        refreshPartsAvailability();

        refreshAllTabs();
    }

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        fundsScheduler.schedule();
        refreshPartsAvailability();
    }

    @Subscribe
    public void handle(TransactionEvent ev) {
        fundsScheduler.schedule();
        refreshPartsAvailability();
    }

    @Subscribe
    public void handle(LoanEvent ev) {
        fundsScheduler.schedule();
        refreshPartsAvailability();
    }

    @Subscribe
    public void handle(AssetEvent ev) {
        fundsScheduler.schedule();
    }

    @Subscribe
    public void handle(AstechPoolChangedEvent ev) {
        refreshTempAstechs();
    }

    @Subscribe
    public void handle(MedicPoolChangedEvent ev) {
        refreshTempMedics();
    }

    @Subscribe
    public void handleLocationChanged(LocationChangedEvent ev) {
        refreshLocation();
    }

    @Subscribe
    public void handleMissionChanged(MissionEvent ev) {
        refreshPartsAvailability();
    }

    @Subscribe
    public void handlePersonUpdate(PersonEvent ev) {
        // only bother recalculating AtB parts availability if a logistics admin has been changed
        // refreshPartsAvailability cuts out early with a "use AtB" check so it's not necessary here
        if (ev.getPerson().hasRole(Person.T_ADMIN_LOG)) {
            refreshPartsAvailability();
        }
    }

    public void refreshLocation() {
        lblLocation.setText(getCampaign().getLocation().getReport(getCampaign().getLocalDate()));
    }

    protected MekHQ getApplication() {
        return app;
    }

    public ReportHyperlinkListener getReportHLL() {
        return reportHLL;
    }

    public Campaign getCampaign() {
        return getApplication().getCampaign();
    }

    public CampaignController getCampaignController() {
        return getApplication().getCampaignController();
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

    public void undeployUnit(Unit u) {
        Force f = getCampaign().getForce(u.getForceId());
        if (f != null) {
            undeployForce(f, false);
        }
        Scenario s = getCampaign().getScenario(u.getScenarioId());
        s.removeUnit(u.getId());
        u.undeploy();
        MekHQ.triggerEvent(new DeploymentChangedEvent(u, s));
    }

    public void undeployForces(Vector<Force> forces) {
        for (Force force : forces) {
            undeployForce(force);
            undeployForces(force.getSubForces());
        }
    }

    public void undeployForce(Force f) {
        undeployForce(f, true);
    }

    public void undeployForce(Force f, boolean killSubs) {
        int sid = f.getScenarioId();
        Scenario scenario = getCampaign().getScenario(sid);
        if (null != scenario) {
            f.clearScenarioIds(getCampaign(), killSubs);
            scenario.removeForce(f.getId());
            if (killSubs) {
                for (UUID uid : f.getAllUnits(false)) {
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

        if (null != scenario) {
            MekHQ.triggerEvent(new DeploymentChangedEvent(f, scenario));
        }
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

    private void setCampaignOptionsFromGameOptions() {
        getCampaign().getCampaignOptions().setUseTactics(getCampaign().getGameOptions().getOption("command_init").booleanValue());
        getCampaign().getCampaignOptions().setInitBonus(getCampaign().getGameOptions().getOption("individual_initiative").booleanValue());
        getCampaign().getCampaignOptions().setToughness(getCampaign().getGameOptions().getOption("toughness").booleanValue());
        getCampaign().getCampaignOptions().setArtillery(getCampaign().getGameOptions().getOption("artillery_skill").booleanValue());
        getCampaign().getCampaignOptions().setAbilities(getCampaign().getGameOptions().getOption("pilot_advantages").booleanValue());
        getCampaign().getCampaignOptions().setEdge(getCampaign().getGameOptions().getOption("edge").booleanValue());
        getCampaign().getCampaignOptions().setImplants(getCampaign().getGameOptions().getOption("manei_domini").booleanValue());
        getCampaign().getCampaignOptions().setQuirks(getCampaign().getGameOptions().getOption("stratops_quirks").booleanValue());
        getCampaign().getCampaignOptions().setAllowCanonOnly(getCampaign().getGameOptions().getOption("canon_only").booleanValue());
        getCampaign().getCampaignOptions().setTechLevel(TechConstants.getSimpleLevel(getCampaign().getGameOptions().getOption("techlevel").stringValue()));
        MekHQ.triggerEvent(new OptionsChangedEvent(getCampaign()));
    }
}
