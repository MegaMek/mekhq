/*
 * CampaignGUI.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.preferences.*;
import megamek.client.ui.swing.GameOptionsDialog;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import mekhq.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.event.*;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.death.AgeRangeRandomDeath;
import mekhq.campaign.personnel.death.ExponentialRandomDeath;
import mekhq.campaign.personnel.death.PercentageRandomDeath;
import mekhq.campaign.personnel.divorce.PercentageRandomDivorce;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.marriage.PercentageRandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.PercentageRandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.NewsItem;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.CampaignExportWizard.CampaignExportWizardState;
import mekhq.gui.dialog.nagDialogs.*;
import mekhq.gui.dialog.reportDialogs.*;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.PartsTableModel;
import mekhq.io.FileType;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.xml.parsers.DocumentBuilder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {
    //region Variable Declarations
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;
    // the max quantity when mass purchasing parts, hiring, etc. using the JSpinner
    public static final int MAX_QUANTITY_SPINNER = 10000;

    private JFrame frame;

    private MekHQ app;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
            MekHQ.getMHQOptions().getLocale());

    /* for the main panel */
    private JTabbedPane tabMain;

    /* For the menu bar */
    private JMenuBar menuBar;
    private JMenu menuThemes;
    private JMenuItem miPersonnelMarket;
    private JMenuItem miContractMarket;
    private JMenuItem miUnitMarket;
    private JMenuItem miShipSearch;
    private JMenuItem miRetirementDefectionDialog;
    private JMenuItem miCompanyGenerator;

    private EnumMap<MHQTabType, CampaignGuiTab> standardTabs;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblLocation;
    private JLabel lblFunds;
    private JLabel lblTempAstechs;
    private JLabel lblTempMedics;
    private JLabel lblPartsAvailabilityRating;

    /* for the top button panel */
    private JPanel btnPanel;
    private JToggleButton btnGMMode;
    private JToggleButton btnOvertime;

    ReportHyperlinkListener reportHLL;

    private boolean logNagActive = false;

    private transient StandardForceIcon copyForceIcon = null;
    //endregion Variable Declarations

    //region Constructors
    public CampaignGUI(MekHQ app) {
        this.app = app;
        reportHLL = new ReportHyperlinkListener(this);
        standardTabs = new EnumMap<>(MHQTabType.class);
        initComponents();
        MekHQ.registerHandler(this);
        setUserPreferences();
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    protected MekHQ getApplication() {
        return app;
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

    public ResourceBundle getResourceMap() {
        return resourceMap;
    }

    public JTabbedPane getTabMain() {
        return tabMain;
    }

    public ReportHyperlinkListener getReportHLL() {
        return reportHLL;
    }

    /**
     * @return the force icon to paste
     */
    public @Nullable StandardForceIcon getCopyForceIcon() {
        return copyForceIcon;
    }

    public void setCopyForceIcon(final @Nullable StandardForceIcon copyForceIcon) {
        this.copyForceIcon = copyForceIcon;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initComponents() {
        frame = new JFrame("MekHQ");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabMain = new JTabbedPane();
        tabMain.setToolTipText("");
        tabMain.setMinimumSize(new Dimension(600, 200));
        tabMain.setPreferredSize(new Dimension(900, 300));

        addStandardTab(MHQTabType.COMMAND_CENTER);
        addStandardTab(MHQTabType.TOE);
        addStandardTab(MHQTabType.BRIEFING_ROOM);
        if (getCampaign().getCampaignOptions().isUseStratCon()) {
            addStandardTab(MHQTabType.STRAT_CON);
        }
        addStandardTab(MHQTabType.INTERSTELLAR_MAP);
        addStandardTab(MHQTabType.PERSONNEL);
        addStandardTab(MHQTabType.HANGAR);
        addStandardTab(MHQTabType.WAREHOUSE);
        addStandardTab(MHQTabType.REPAIR_BAY);
        addStandardTab(MHQTabType.INFIRMARY);
        addStandardTab(MHQTabType.MEK_LAB);
        addStandardTab(MHQTabType.FINANCES);

        // check to see if we just selected the command center tab
        // and if so change its color to standard
        tabMain.addChangeListener(evt -> {
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
            public void windowClosing(WindowEvent evt) {
                getApplication().exit();
            }
        });

    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CampaignGUI.class);
            frame.setName("mainWindow");
            preferences.manage(new JWindowPreference(frame));
            UIUtil.keepOnScreen(frame);
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    /**
     * This is used to initialize the top menu bar.
     * All the top level menu bar and {@link MHQTabType} mnemonics must be unique, as they are both
     * accessed through the same GUI page.
     * The following mnemonic keys are being used as of 30-MAR-2020:
     * A, B, C, E, F, H, I, L, M, N, O, P, R, S, T, V, W, /
     *
     * Note 1: the slash is used for the help, as it is normally the same key as the ?
     * Note 2: the A mnemonic is used for the Advance Day button
     */
    private void initMenu() {
        // TODO : Implement "Export All" versions for Personnel and Parts
        // See the JavaDoc comment for used mnemonic keys
        menuBar = new JMenuBar();
        menuBar.getAccessibleContext().setAccessibleName("Main Menu");

        //region File Menu
        // The File menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, E, H, I, L, M, N, R, S, T, U, X
        JMenu menuFile = new JMenu(resourceMap.getString("fileMenu.text"));
        menuFile.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuLoad = new JMenuItem(resourceMap.getString("menuLoad.text"));
        menuLoad.setMnemonic(KeyEvent.VK_L);
        menuLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK));
        menuLoad.addActionListener(evt -> {
            final File file = FileDialogs.openCampaign(frame).orElse(null);
            if (file == null) {
                return;
            }
            new DataLoadingDialog(getFrame(), getApplication(), file).setVisible(true);
            // Unregister event handlers for CampaignGUI and tabs
            for (int i = 0; i < tabMain.getTabCount(); i++) {
                if (tabMain.getComponentAt(i) instanceof CampaignGuiTab) {
                    ((CampaignGuiTab) tabMain.getComponentAt(i)).disposeTab();
                }
            }
            MekHQ.unregisterHandler(this);
            // check for a loaded story arc and unregister that handler as well
            if (null != getCampaign().getStoryArc()) {
                MekHQ.unregisterHandler(getCampaign().getStoryArc());
            }
        });
        menuFile.add(menuLoad);

        JMenuItem menuSave = new JMenuItem(resourceMap.getString("menuSave.text"));
        menuSave.setMnemonic(KeyEvent.VK_S);
        menuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        menuSave.addActionListener(this::saveCampaign);
        menuFile.add(menuSave);

        JMenuItem menuNew = new JMenuItem(resourceMap.getString("menuNew.text"));
        menuNew.setMnemonic(KeyEvent.VK_N);
        menuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        menuNew.addActionListener(evt -> {
            int decision = new NewCampaignConfirmationDialog().YesNoOption();
            if (decision == JOptionPane.YES_OPTION) {
                new DataLoadingDialog(frame, app, null).setVisible(true);
            }
        });
        menuFile.add(menuNew);

        //region menuImport
        // The Import menu uses the following Mnemonic keys as of 25-MAR-2022:
        // A, C, F, I, P
        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text"));
        menuImport.setMnemonic(KeyEvent.VK_I);

        final JMenuItem miImportCampaignPreset = new JMenuItem(resourceMap.getString("miImportCampaignPreset.text"));
        miImportCampaignPreset.setToolTipText(resourceMap.getString("miImportCampaignPreset.toolTipText"));
        miImportCampaignPreset.setName("miImportCampaignPreset");
        miImportCampaignPreset.setMnemonic(KeyEvent.VK_C);
        miImportCampaignPreset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miImportCampaignPreset.addActionListener(evt -> {
            final CampaignPresetSelectionDialog campaignPresetSelectionDialog = new CampaignPresetSelectionDialog(getFrame());
            if (!campaignPresetSelectionDialog.showDialog().isConfirmed()) {
                return;
            }
            final CampaignPreset preset = campaignPresetSelectionDialog.getSelectedPreset();
            if (preset == null) {
                return;
            }
            preset.applyContinuousToCampaign(getCampaign());
        });
        menuImport.add(miImportCampaignPreset);

        JMenuItem miImportPerson = new JMenuItem(resourceMap.getString("miImportPerson.text"));
        miImportPerson.setMnemonic(KeyEvent.VK_P);
        miImportPerson.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miImportPerson.addActionListener(evt -> loadPersonFile());
        menuImport.add(miImportPerson);

        JMenuItem miImportIndividualRankSystem = new JMenuItem(resourceMap.getString("miImportIndividualRankSystem.text"));
        miImportIndividualRankSystem.setToolTipText(resourceMap.getString("miImportIndividualRankSystem.toolTipText"));
        miImportIndividualRankSystem.setName("miImportIndividualRankSystem");
        miImportIndividualRankSystem.setMnemonic(KeyEvent.VK_I);
        miImportIndividualRankSystem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        miImportIndividualRankSystem.addActionListener(evt -> getCampaign().setRankSystem(RankSystem
                .generateIndividualInstanceFromXML(FileDialogs.openIndividualRankSystem(getFrame()).orElse(null))));
        menuImport.add(miImportIndividualRankSystem);

        JMenuItem miImportParts = new JMenuItem(resourceMap.getString("miImportParts.text"));
        miImportParts.setMnemonic(KeyEvent.VK_A);
        miImportParts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        miImportParts.addActionListener(evt -> loadPartsFile());
        menuImport.add(miImportParts);

        JMenuItem miLoadForces = new JMenuItem(resourceMap.getString("miLoadForces.text"));
        miLoadForces.setMnemonic(KeyEvent.VK_F);
        miLoadForces.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
        miLoadForces.addActionListener(evt -> loadListFile(true));
        menuImport.add(miLoadForces);

        menuFile.add(menuImport);
        //endregion menuImport

        //region menuExport
        // The Export menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, X, S
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text"));
        menuExport.setMnemonic(KeyEvent.VK_X);

        //region CSV Export
        // The CSV menu uses the following Mnemonic keys as of 25-MAR-2022:
        // F, P, U
        JMenu miExportCSVFile = new JMenu(resourceMap.getString("menuExportCSV.text"));
        miExportCSVFile.setMnemonic(KeyEvent.VK_C);

        JMenuItem miExportPersonCSV = new JMenuItem(resourceMap.getString("miExportPersonnel.text"));
        miExportPersonCSV.setMnemonic(KeyEvent.VK_P);
        miExportPersonCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miExportPersonCSV.addActionListener(evt -> {
            try {
                exportPersonnel(FileType.CSV, resourceMap.getString("dlgSavePersonnelCSV.text"),
                        getCampaign().getLocalDate().format(
                                DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                        .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                                + "_ExportedPersonnel");
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        });
        miExportCSVFile.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(resourceMap.getString("miExportUnit.text"));
        miExportUnitCSV.setMnemonic(KeyEvent.VK_U);
        miExportUnitCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miExportUnitCSV.addActionListener(evt -> {
            try {
                exportUnits(FileType.CSV, resourceMap.getString("dlgSaveUnitsCSV.text"),
                        getCampaign().getName() + getCampaign().getLocalDate().format(
                                DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                        .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                                + "_ExportedUnits");
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        });
        miExportCSVFile.add(miExportUnitCSV);

        JMenuItem miExportFinancesCSV = new JMenuItem(resourceMap.getString("miExportFinances.text"));
        miExportFinancesCSV.setMnemonic(KeyEvent.VK_F);
        miExportFinancesCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
        miExportFinancesCSV.addActionListener(evt -> {
            try {
                exportFinances(FileType.CSV, resourceMap.getString("dlgSaveFinancesCSV.text"),
                        getCampaign().getName() + getCampaign().getLocalDate().format(
                                DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                        .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                                + "_ExportedFinances");
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        });
        miExportCSVFile.add(miExportFinancesCSV);

        menuExport.add(miExportCSVFile);
        //endregion CSV Export

        //region XML Export
        // The XML menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, I, P, R
        JMenu miExportXMLFile = new JMenu(resourceMap.getString("menuExportXML.text"));
        miExportXMLFile.setMnemonic(KeyEvent.VK_X);

        final JMenuItem miExportCampaignPreset = new JMenuItem(resourceMap.getString("miExportCampaignPreset.text"));
        miExportCampaignPreset.setName("miExportCampaignPreset");
        miExportCampaignPreset.setMnemonic(KeyEvent.VK_C);
        miExportCampaignPreset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miExportCampaignPreset.addActionListener(evt -> {
            final CreateCampaignPresetDialog createCampaignPresetDialog
                    = new CreateCampaignPresetDialog(getFrame(), getCampaign(), null);
            if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
                return;
            }
            final CampaignPreset preset = createCampaignPresetDialog.getPreset();
            if (preset == null) {
                return;
            }
            preset.writeToFile(getFrame(),
                    FileDialogs.saveCampaignPreset(getFrame(), preset).orElse(null));
        });
        miExportXMLFile.add(miExportCampaignPreset);

        JMenuItem miExportRankSystems = new JMenuItem(resourceMap.getString("miExportRankSystems.text"));
        miExportRankSystems.setName("miExportRankSystems");
        miExportRankSystems.setMnemonic(KeyEvent.VK_R);
        miExportRankSystems.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miExportRankSystems.addActionListener(evt -> Ranks.exportRankSystemsToFile(FileDialogs
                .saveRankSystems(getFrame()).orElse(null), getCampaign().getRankSystem()));
        miExportXMLFile.add(miExportRankSystems);

        JMenuItem miExportIndividualRankSystem = new JMenuItem(resourceMap.getString("miExportIndividualRankSystem.text"));
        miExportIndividualRankSystem.setName("miExportIndividualRankSystem");
        miExportIndividualRankSystem.setMnemonic(KeyEvent.VK_I);
        miExportIndividualRankSystem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        miExportIndividualRankSystem.addActionListener(evt -> getCampaign().getRankSystem()
                .writeToFile(FileDialogs.saveIndividualRankSystem(getFrame()).orElse(null)));
        miExportXMLFile.add(miExportIndividualRankSystem);

        JMenuItem miExportPlanetsXML = new JMenuItem(resourceMap.getString("miExportPlanets.text"));
        miExportPlanetsXML.setMnemonic(KeyEvent.VK_P);
        miExportPlanetsXML.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miExportPlanetsXML.addActionListener(evt -> {
            try {
                exportPlanets(FileType.XML, resourceMap.getString("dlgSavePlanetsXML.text"),
                        getCampaign().getName() + getCampaign().getLocalDate().format(
                                DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                        .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                                + "_ExportedPlanets");
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        });
        miExportXMLFile.add(miExportPlanetsXML);

        menuExport.add(miExportXMLFile);
        //endregion XML Export

        JMenuItem miExportCampaignSubset = new JMenuItem(resourceMap.getString("miExportCampaignSubset.text"));
        miExportCampaignSubset.setMnemonic(KeyEvent.VK_S);
        miExportCampaignSubset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        miExportCampaignSubset.addActionListener(evt -> {
            CampaignExportWizard cew = new CampaignExportWizard(getCampaign());
            cew.display(CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        menuFile.add(menuExport);
        //endregion menuExport

        //region Menu Refresh
        // The Refresh menu uses the following Mnemonic keys as of 12-APR-2022:
        // A, C, D, F, P, R, U
        JMenu menuRefresh = new JMenu(resourceMap.getString("menuRefresh.text"));
        menuRefresh.setMnemonic(KeyEvent.VK_R);

        JMenuItem miRefreshUnitCache = new JMenuItem(resourceMap.getString("miRefreshUnitCache.text"));
        miRefreshUnitCache.setName("miRefreshUnitCache");
        miRefreshUnitCache.setMnemonic(KeyEvent.VK_U);
        miRefreshUnitCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miRefreshUnitCache.addActionListener(evt -> MechSummaryCache.refreshUnitData(false));
        menuRefresh.add(miRefreshUnitCache);

        JMenuItem miRefreshCamouflage = new JMenuItem(resourceMap.getString("miRefreshCamouflage.text"));
        miRefreshCamouflage.setName("miRefreshCamouflage");
        miRefreshCamouflage.setMnemonic(KeyEvent.VK_C);
        miRefreshCamouflage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miRefreshCamouflage.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshCamouflageDirectory();
            refreshAllTabs();
        });
        menuRefresh.add(miRefreshCamouflage);

        JMenuItem miRefreshPortraits = new JMenuItem(resourceMap.getString("miRefreshPortraits.text"));
        miRefreshPortraits.setName("miRefreshPortraits");
        miRefreshPortraits.setMnemonic(KeyEvent.VK_P);
        miRefreshPortraits.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miRefreshPortraits.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshPortraitDirectory();
            refreshAllTabs();
        });
        menuRefresh.add(miRefreshPortraits);

        JMenuItem miRefreshForceIcons = new JMenuItem(resourceMap.getString("miRefreshForceIcons.text"));
        miRefreshForceIcons.setName("miRefreshForceIcons");
        miRefreshForceIcons.setMnemonic(KeyEvent.VK_F);
        miRefreshForceIcons.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
        miRefreshForceIcons.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshForceIcons();
            refreshAllTabs();
        });
        menuRefresh.add(miRefreshForceIcons);

        JMenuItem miRefreshAwards = new JMenuItem(resourceMap.getString("miRefreshAwards.text"));
        miRefreshAwards.setName("miRefreshAwards");
        miRefreshAwards.setMnemonic(KeyEvent.VK_A);
        miRefreshAwards.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        miRefreshAwards.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshAwardIcons();
            refreshAllTabs();
        });
        menuRefresh.add(miRefreshAwards);

        JMenuItem miRefreshStoryIcons = new JMenuItem(resourceMap.getString("miRefreshStoryIcons.text"));
        miRefreshStoryIcons.setName("miRefreshAwards");
        miRefreshStoryIcons.setMnemonic(KeyEvent.VK_A);
        miRefreshStoryIcons.addActionListener(evt -> {
            MHQStaticDirectoryManager.refreshStorySplash();
            refreshAllTabs();
        });
        menuRefresh.add(miRefreshStoryIcons);

        JMenuItem miRefreshRanks = new JMenuItem(resourceMap.getString("miRefreshRanks.text"));
        miRefreshRanks.setName("miRefreshRanks");
        miRefreshRanks.setMnemonic(KeyEvent.VK_R);
        miRefreshRanks.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miRefreshRanks.addActionListener(evt -> Ranks.reinitializeRankSystems(getCampaign()));
        menuRefresh.add(miRefreshRanks);

        JMenuItem miRefreshRandomDeathCauses = new JMenuItem(resourceMap.getString("miRefreshRandomDeathCauses.text"));
        miRefreshRandomDeathCauses.setToolTipText(resourceMap.getString("miRefreshRandomDeathCauses.toolTipText"));
        miRefreshRandomDeathCauses.setName("miRefreshRandomDeathCauses");
        miRefreshRandomDeathCauses.setMnemonic(KeyEvent.VK_D);
        miRefreshRandomDeathCauses.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
        miRefreshRandomDeathCauses.addActionListener(evt -> getCampaign().setDeath(
                getCampaign().getCampaignOptions().getRandomDeathMethod().getMethod(getCampaign().getCampaignOptions())));
        menuRefresh.add(miRefreshRandomDeathCauses);

        JMenuItem miRefreshFinancialInstitutions = new JMenuItem(resourceMap.getString("miRefreshFinancialInstitutions.text"));
        miRefreshFinancialInstitutions.setToolTipText(resourceMap.getString("miRefreshFinancialInstitutions.toolTipText"));
        miRefreshFinancialInstitutions.setName("miRefreshFinancialInstitutions");
        miRefreshFinancialInstitutions.addActionListener(evt -> FinancialInstitutions.initializeFinancialInstitutions());
        menuRefresh.add(miRefreshFinancialInstitutions);

        menuFile.add(menuRefresh);
        //endregion Menu Refresh

        JMenuItem miMercRoster = new JMenuItem(resourceMap.getString("miMercRoster.text"));
        miMercRoster.setMnemonic(KeyEvent.VK_U);
        miMercRoster.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miMercRoster.addActionListener(evt -> new MercRosterDialog(getFrame(), true, getCampaign()).setVisible(true));
        menuFile.add(miMercRoster);

        JMenuItem menuOptions = new JMenuItem(resourceMap.getString("menuOptions.text"));
        menuOptions.setMnemonic(KeyEvent.VK_C);
        menuOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        menuOptions.addActionListener(this::menuOptionsActionPerformed);
        menuFile.add(menuOptions);

        final JMenuItem miGameOptions = new JMenuItem(resourceMap.getString("miGameOptions.text"));
        miGameOptions.setToolTipText(resourceMap.getString("miGameOptions.toolTipText"));
        miGameOptions.setName("miGameOptions");
        miGameOptions.setMnemonic(KeyEvent.VK_M);
        miGameOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        miGameOptions.addActionListener(evt -> {
            final GameOptionsDialog god = new GameOptionsDialog(getFrame(), getCampaign().getGameOptions(), false);
            god.setEditable(true);
            if (god.showDialog().isConfirmed()) {
                getCampaign().setGameOptions(god.getOptions());
                refreshCalendar();
            }
        });
        menuFile.add(miGameOptions);

        final JMenuItem miMHQOptions = new JMenuItem(resourceMap.getString("miMHQOptions.text"));
        miMHQOptions.setToolTipText(resourceMap.getString("miMHQOptions.toolTipText"));
        miMHQOptions.setName("miMHQOptions");
        miMHQOptions.setMnemonic(KeyEvent.VK_H);
        miMHQOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miMHQOptions.addActionListener(evt -> new MHQOptionsDialog(getFrame()).setVisible(true));
        menuFile.add(miMHQOptions);

        menuThemes = new JMenu(resourceMap.getString("menuThemes.text"));
        menuThemes.setMnemonic(KeyEvent.VK_T);
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem(resourceMap.getString("menuExit.text"));
        menuExitItem.setMnemonic(KeyEvent.VK_E);
        menuExitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
        menuExitItem.addActionListener(evt -> getApplication().exit());
        menuFile.add(menuExitItem);

        menuBar.add(menuFile);
        //endregion File Menu

        //region Marketplace Menu
        // The Marketplace menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, C, H, M, N, P, R, S, U
        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text"));
        menuMarket.setMnemonic(KeyEvent.VK_M);

        miPersonnelMarket = new JMenuItem(resourceMap.getString("miPersonnelMarket.text"));
        miPersonnelMarket.setMnemonic(KeyEvent.VK_P);
        miPersonnelMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miPersonnelMarket.addActionListener(evt -> hirePersonMarket());
        miPersonnelMarket.setVisible(!getCampaign().getPersonnelMarket().isNone());
        menuMarket.add(miPersonnelMarket);

        miContractMarket = new JMenuItem(resourceMap.getString("miContractMarket.text"));
        miContractMarket.setMnemonic(KeyEvent.VK_C);
        miContractMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miContractMarket.addActionListener(evt -> showContractMarket());
        miContractMarket.setVisible(getCampaign().getCampaignOptions().isUseAtB());
        menuMarket.add(miContractMarket);

        miUnitMarket = new JMenuItem(resourceMap.getString("miUnitMarket.text"));
        miUnitMarket.setMnemonic(KeyEvent.VK_U);
        miUnitMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miUnitMarket.addActionListener(evt -> showUnitMarket());
        miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        menuMarket.add(miUnitMarket);

        miShipSearch = new JMenuItem(resourceMap.getString("miShipSearch.text"));
        miShipSearch.setMnemonic(KeyEvent.VK_S);
        miShipSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        miShipSearch.addActionListener(evt -> new ShipSearchDialog(getFrame(), this).setVisible(true));
        miShipSearch.setVisible(getCampaign().getCampaignOptions().isUseAtB());
        menuMarket.add(miShipSearch);

        JMenuItem miPurchaseUnit = new JMenuItem(resourceMap.getString("miPurchaseUnit.text"));
        miPurchaseUnit.setMnemonic(KeyEvent.VK_N);
        miPurchaseUnit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        miPurchaseUnit.addActionListener(evt -> {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
            if (!MechSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(getFrame(), unitLoadingDialog,
                    getCampaign(), true);
            usd.setVisible(true);
        });
        menuMarket.add(miPurchaseUnit);

        JMenuItem miBuyParts = new JMenuItem(resourceMap.getString("miBuyParts.text"));
        miBuyParts.setMnemonic(KeyEvent.VK_R);
        miBuyParts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miBuyParts.addActionListener(evt -> new PartsStoreDialog(true, this).setVisible(true));
        menuMarket.add(miBuyParts);

        JMenuItem miHireBulk = new JMenuItem(resourceMap.getString("miHireBulk.text"));
        miHireBulk.setMnemonic(KeyEvent.VK_B);
        miHireBulk.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miHireBulk.addActionListener(evt -> hireBulkPersonnel());
        menuMarket.add(miHireBulk);

        JMenu menuHire = new JMenu(resourceMap.getString("menuHire.text"));
        menuHire.setMnemonic(KeyEvent.VK_H);
        for (PersonnelRole role : PersonnelRole.getPrimaryRoles()) {
            JMenuItem miHire = new JMenuItem(role.getName(getCampaign().getFaction().isClan()));
            if (role.getMnemonic() != KeyEvent.VK_UNDEFINED) {
                miHire.setMnemonic(role.getMnemonic());
                miHire.setAccelerator(KeyStroke.getKeyStroke(role.getMnemonic(), InputEvent.ALT_DOWN_MASK));
            }
            miHire.setActionCommand(role.name());
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
        miHireAstechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
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
        miFireAstechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
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
        miFullStrengthAstechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miFullStrengthAstechs.addActionListener(evt -> getCampaign().fillAstechPool());
        menuAstechPool.add(miFullStrengthAstechs);

        JMenuItem miFireAllAstechs = new JMenuItem(resourceMap.getString("miFireAllAstechs.text"));
        miFireAllAstechs.setMnemonic(KeyEvent.VK_R);
        miFireAllAstechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
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
        miHireMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
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
        miFireMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
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
        miFullStrengthMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miFullStrengthMedics.addActionListener(evt -> getCampaign().fillMedicPool());
        menuMedicPool.add(miFullStrengthMedics);

        JMenuItem miFireAllMedics = new JMenuItem(resourceMap.getString("miFireAllMedics.text"));
        miFireAllMedics.setMnemonic(KeyEvent.VK_R);
        miFireAllMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miFireAllMedics.addActionListener(evt -> getCampaign().decreaseMedicPool(getCampaign().getMedicPool()));
        menuMedicPool.add(miFireAllMedics);
        menuMarket.add(menuMedicPool);
        //endregion Medic Pool

        menuBar.add(menuMarket);
        //endregion Marketplace Menu

        //region Reports Menu
        // The Reports menu uses the following Mnemonic keys as of 19-March-2020:
        // C, H, P, T, U
        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text"));
        menuReports.setMnemonic(KeyEvent.VK_E);

        JMenuItem miDragoonsRating = new JMenuItem(resourceMap.getString("miDragoonsRating.text"));
        miDragoonsRating.setMnemonic(KeyEvent.VK_U);
        miDragoonsRating.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miDragoonsRating.addActionListener(evt ->
                new UnitRatingReportDialog(getFrame(), getCampaign()).setVisible(true));
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(resourceMap.getString("miPersonnelReport.text"));
        miPersonnelReport.setMnemonic(KeyEvent.VK_P);
        miPersonnelReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miPersonnelReport.addActionListener(evt ->
                new PersonnelReportDialog(getFrame(), new PersonnelReport(getCampaign())).setVisible(true));
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(resourceMap.getString("miHangarBreakdown.text"));
        miHangarBreakdown.setMnemonic(KeyEvent.VK_H);
        miHangarBreakdown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miHangarBreakdown.addActionListener(evt ->
                new HangarReportDialog(getFrame(), new HangarReport(getCampaign())).setVisible(true));
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(resourceMap.getString("miTransportReport.text"));
        miTransportReport.setMnemonic(KeyEvent.VK_T);
        miTransportReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK));
        miTransportReport.addActionListener(evt ->
                new TransportReportDialog(getFrame(), new TransportReport(getCampaign())).setVisible(true));
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(resourceMap.getString("miCargoReport.text"));
        miCargoReport.setMnemonic(KeyEvent.VK_C);
        miCargoReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miCargoReport.addActionListener(evt ->
                new CargoReportDialog(getFrame(), new CargoReport(getCampaign())).setVisible(true));
        menuReports.add(miCargoReport);

        menuBar.add(menuReports);
        //endregion Reports Menu

        //region View Menu
        // The View menu uses the following Mnemonic keys as of 02-June-2020:
        // H, R
        JMenu menuView = new JMenu(resourceMap.getString("menuView.text"));
        menuView.setMnemonic(KeyEvent.VK_V);

        JMenuItem miHistoricalDailyReportDialog = new JMenuItem(resourceMap.getString("miShowHistoricalReportLog.text"));
        miHistoricalDailyReportDialog.setMnemonic(KeyEvent.VK_H);
        miHistoricalDailyReportDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miHistoricalDailyReportDialog.addActionListener(evt -> {
            HistoricalDailyReportDialog histDailyReportDialog = new HistoricalDailyReportDialog(getFrame(), this);
            histDailyReportDialog.setModal(true);
            histDailyReportDialog.setVisible(true);
            histDailyReportDialog.dispose();
        });
        menuView.add(miHistoricalDailyReportDialog);

        miRetirementDefectionDialog = new JMenuItem(resourceMap.getString("miRetirementDefectionDialog.text"));
        miRetirementDefectionDialog.setMnemonic(KeyEvent.VK_R);
        miRetirementDefectionDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miRetirementDefectionDialog.setVisible(!getCampaign().getCampaignOptions().getRandomRetirementMethod().isNone());
        miRetirementDefectionDialog.addActionListener(evt -> showRetirementDefectionDialog());
        menuView.add(miRetirementDefectionDialog);

        menuBar.add(menuView);
        //endregion View Menu

        //region Manage Campaign Menu
        // The Manage Campaign menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, C, G, M, S
        JMenu menuManage = new JMenu(resourceMap.getString("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_C);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = new JMenuItem(resourceMap.getString("miGMToolsDialog.text"));
        miGMToolsDialog.setMnemonic(KeyEvent.VK_G);
        miGMToolsDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.ALT_DOWN_MASK));
        miGMToolsDialog.addActionListener(evt -> new GMToolsDialog(getFrame(), this, null).setVisible(true));
        menuManage.add(miGMToolsDialog);

        JMenuItem miAdvanceMultipleDays = new JMenuItem(resourceMap.getString("miAdvanceMultipleDays.text"));
        miAdvanceMultipleDays.setMnemonic(KeyEvent.VK_A);
        miAdvanceMultipleDays.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        miAdvanceMultipleDays.addActionListener(evt -> new AdvanceDaysDialog(getFrame(), this).setVisible(true));
        miAdvanceMultipleDays.setVisible(getCampaignController().isHost());
        menuManage.add(miAdvanceMultipleDays);

        JMenuItem miBloodnames = new JMenuItem(resourceMap.getString("miRandomBloodnames.text"));
        miBloodnames.setMnemonic(KeyEvent.VK_B);
        miBloodnames.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miBloodnames.addActionListener(evt -> {
            for (final Person person : getCampaign().getPersonnel()) {
                getCampaign().checkBloodnameAdd(person, false);
            }
        });
        menuManage.add(miBloodnames);

        final JMenuItem miMassPersonnelTraining = new JMenuItem(resourceMap.getString("miMassPersonnelTraining.text"));
        miMassPersonnelTraining.setToolTipText(resourceMap.getString("miMassPersonnelTraining.toolTipText"));
        miMassPersonnelTraining.setName("miMassPersonnelTraining");
        miMassPersonnelTraining.setMnemonic(KeyEvent.VK_M);
        miMassPersonnelTraining.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        miMassPersonnelTraining.addActionListener(evt -> new BatchXPDialog(getFrame(), getCampaign()).setVisible(true));
        menuManage.add(miMassPersonnelTraining);

        JMenuItem miScenarioEditor = new JMenuItem(resourceMap.getString("miScenarioEditor.text"));
        miScenarioEditor.setMnemonic(KeyEvent.VK_S);
        miScenarioEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        miScenarioEditor.addActionListener(evt -> new ScenarioTemplateEditorDialog(getFrame()).setVisible(true));
        menuManage.add(miScenarioEditor);

        miCompanyGenerator = new JMenuItem(resourceMap.getString("miCompanyGenerator.text"));
        miCompanyGenerator.setMnemonic(KeyEvent.VK_C);
        miCompanyGenerator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
        miCompanyGenerator.addActionListener(evt ->
                new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(true));
        menuManage.add(miCompanyGenerator);

        menuBar.add(menuManage);
        //endregion Manage Campaign Menu

        //region Help Menu
        // The Help menu uses the following Mnemonic keys as of 19-March-2020:
        // A
        JMenu menuHelp = new JMenu(resourceMap.getString("menuHelp.text"));
        menuHelp.setMnemonic(KeyEvent.VK_SLASH);
        menuHelp.setName("helpMenu");

        JMenuItem menuAboutItem = new JMenuItem(resourceMap.getString("menuAbout.text"));
        menuAboutItem.setMnemonic(KeyEvent.VK_A);
        menuAboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        menuAboutItem.setName("aboutMenuItem");
        menuAboutItem.addActionListener(evt -> {
            MekHQAboutBox aboutBox = new MekHQAboutBox(getFrame());
            aboutBox.setLocationRelativeTo(getFrame());
            aboutBox.setModal(true);
            aboutBox.setVisible(true);
            aboutBox.dispose();
        });
        menuHelp.add(menuAboutItem);

        menuBar.add(menuHelp);
        //endregion Help Menu
    }

    private void initStatusBar() {
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));
        statusPanel.getAccessibleContext().setAccessibleName("Status Bar");

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
        lblLocation = new JLabel(getCampaign().getLocation().getReport(getCampaign().getLocalDate()));

        btnPanel = new JPanel(new GridBagLayout());
        btnPanel.getAccessibleContext().setAccessibleName("Campaign Actions");

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(3, 10, 3, 3);
        btnPanel.add(lblLocation, gridBagConstraints);

        btnGMMode = new JToggleButton(resourceMap.getString("btnGMMode.text"));
        btnGMMode.setToolTipText(resourceMap.getString("btnGMMode.toolTipText"));
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(e -> getCampaign().setGMMode(btnGMMode.isSelected()));
        btnGMMode.setMinimumSize(new Dimension(150, 25));
        btnGMMode.setPreferredSize(new Dimension(150, 25));
        btnGMMode.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        btnPanel.add(btnGMMode, gridBagConstraints);

        btnOvertime = new JToggleButton(resourceMap.getString("btnOvertime.text"));
        btnOvertime.setToolTipText(resourceMap.getString("btnOvertime.toolTipText"));
        btnOvertime.addActionListener(evt -> getCampaign().setOvertime(btnOvertime.isSelected()));
        btnOvertime.setMinimumSize(new Dimension(150, 25));
        btnOvertime.setPreferredSize(new Dimension(150, 25));
        btnOvertime.setMaximumSize(new Dimension(150, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        btnPanel.add(btnOvertime, gridBagConstraints);

        // This button uses a mnemonic that is unique and listed in the initMenu JavaDoc
        JButton btnAdvanceDay = new JButton(resourceMap.getString("btnAdvanceDay.text"));
        btnAdvanceDay.setToolTipText(resourceMap.getString("btnAdvanceDay.toolTipText"));
        btnAdvanceDay.addActionListener(evt -> getCampaignController().advanceDay());
        btnAdvanceDay.setMnemonic(KeyEvent.VK_A);
        btnAdvanceDay.setPreferredSize(new Dimension(250, 50));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        btnPanel.add(btnAdvanceDay, gridBagConstraints);
    }
    //endregion Initialization

    public @Nullable CampaignGuiTab getTab(final MHQTabType tabType) {
        return standardTabs.get(tabType);
    }

    public @Nullable CommandCenterTab getCommandCenterTab() {
        return (CommandCenterTab) getTab(MHQTabType.COMMAND_CENTER);
    }

    public @Nullable TOETab getTOETab() {
        return (TOETab) getTab(MHQTabType.TOE);
    }

    public @Nullable MapTab getMapTab() {
        return (MapTab) getTab(MHQTabType.INTERSTELLAR_MAP);
    }

    public @Nullable PersonnelTab getPersonnelTab() {
        return (PersonnelTab) getTab(MHQTabType.PERSONNEL);
    }

    public @Nullable WarehouseTab getWarehouseTab() {
        return (WarehouseTab) getTab(MHQTabType.WAREHOUSE);
    }

    public boolean hasTab(MHQTabType tabType) {
        return standardTabs.containsKey(tabType);
    }

    /**
     * Sets the selected tab by its {@link MHQTabType}.
     * @param tabType The type of tab to select.
     */
    public void setSelectedTab(MHQTabType tabType) {
        if (standardTabs.containsKey(tabType)) {
            final CampaignGuiTab tab = standardTabs.get(tabType);
            IntStream.range(0, tabMain.getTabCount())
                    .filter(ii -> tabMain.getComponentAt(ii) == tab)
                    .findFirst()
                    .ifPresent(ii -> tabMain.setSelectedIndex(ii));
        }
    }

    /**
     * Adds one of the built-in tabs to the gui, if it is not already present.
     *
     * @param tab The type of tab to add
     */
    public void addStandardTab(MHQTabType tab) {
        if (!standardTabs.containsKey(tab)) {
            CampaignGuiTab t = tab.createTab(this);
            if (t != null) {
                standardTabs.put(tab, t);
                int index = IntStream.range(0, tabMain.getTabCount())
                        .filter(i -> ((CampaignGuiTab) tabMain.getComponentAt(i)).tabType().ordinal() > tab.ordinal())
                        .findFirst()
                        .orElse(tabMain.getTabCount());
                tabMain.insertTab(t.getTabName(), null, t, null, index);
                tabMain.setMnemonicAt(index, tab.getMnemonic());
            }
        }
    }

    /**
     * Removes one of the built-in tabs from the gui.
     *
     * @param tabType The tab to remove
     */
    public void removeStandardTab(MHQTabType tabType) {
        CampaignGuiTab tab = standardTabs.get(tabType);
        if (tab != null) {
            MekHQ.unregisterHandler(tab);
            removeTab(tab);
        }
    }

    /**
     * Removes a tab from the gui.
     *
     * @param tab The tab to remove
     */
    public void removeTab(CampaignGuiTab tab) {
        tab.disposeTab();
        removeTab(tab.getTabName());
    }

    /**
     * Removes a tab from the gui.
     *
     * @param tabName The name of the tab to remove
     */
    public void removeTab(String tabName) {
        int index = tabMain.indexOfTab(tabName);
        if (index >= 0) {
            CampaignGuiTab tab = (CampaignGuiTab) tabMain.getComponentAt(index);
            standardTabs.remove(tab.tabType());
            tabMain.removeTabAt(index);
        }
    }

    public void showRetirementDefectionDialog() {
        /*
         * if there are unresolved personnel, show the results view; otherwise,
         * present the retirement view to give the player a chance to follow a
         * custom schedule
         */
        RetirementDefectionDialog rdd = new RetirementDefectionDialog(this, null,
                getCampaign().getRetirementDefectionTracker().getRetirees().isEmpty());
        rdd.setVisible(true);
        if (!rdd.wasAborted()) {
            getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments());
        }
    }

    private static void enableFullScreenMode(Window window) {
        String className = "com.apple.eawt.FullScreenUtilities";
        String methodName = "setWindowCanFullScreen";

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, Window.class, boolean.class);
            method.invoke(null, window, true);
        } catch (Throwable t) {
            LogManager.getLogger().error("Full screen mode is not supported", t);
        }
    }

    private static boolean isMacOSX() {
        return System.getProperty("os.name").contains("Mac OS X");
    }

    private void changeTheme(ActionEvent evt) {
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

    public void focusOnUnit(UUID id) {
        HangarTab ht = (HangarTab) getTab(MHQTabType.HANGAR);
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
        if (getTab(MHQTabType.REPAIR_BAY) != null) {
            ((RepairTab) getTab(MHQTabType.REPAIR_BAY)).focusOnUnit(id);
            tabMain.setSelectedComponent(getTab(MHQTabType.REPAIR_BAY));
        }
    }

    public void focusOnPerson(Person person) {
        if (person != null) {
            focusOnPerson(person.getId());
        }
    }

    public void focusOnPerson(UUID id) {
        if (id == null) {
            return;
        }
        PersonnelTab pt = (PersonnelTab) getTab(MHQTabType.PERSONNEL);
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

    private void hirePerson(final ActionEvent evt) {
        final NewRecruitDialog npd = new NewRecruitDialog(this, true,
                getCampaign().newPerson(PersonnelRole.valueOf(evt.getActionCommand())));
        npd.setVisible(true);
    }

    public void hirePersonMarket() {
        PersonnelMarketDialog pmd = new PersonnelMarketDialog(getFrame(), this, getCampaign());
        pmd.setVisible(true);
    }

    private void hireBulkPersonnel() {
        HireBulkPersonnelDialog hbpd = new HireBulkPersonnelDialog(getFrame(), true, getCampaign());
        hbpd.setVisible(true);
    }

    public void showContractMarket() {
        ContractMarketDialog cmd = new ContractMarketDialog(getFrame(), getCampaign());
        cmd.setVisible(true);
    }

    public void showUnitMarket() {
        if (getCampaign().getUnitMarket().getMethod().isNone()) {
            LogManager.getLogger().error("Attempted to show the unit market while it is disabled");
        } else {
            new UnitMarketDialog(getFrame(), getCampaign()).showDialog();
        }
    }

    public boolean saveCampaign(ActionEvent evt) {
        LogManager.getLogger().info("Saving campaign...");
        // Choose a file...
        File file = FileDialogs.saveCampaign(frame, getCampaign()).orElse(null);
        if (file == null) {
            // I want a file, y'know!
            return false;
        }

        return saveCampaign(getFrame(), getCampaign(), file);
    }

    /**
     * Attempts to saves the given campaign to the given file.
     * @param frame The parent frame in which to display the error message. May be null.
     */
    public static boolean saveCampaign(JFrame frame, Campaign campaign, File file) {
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
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStream os = path.endsWith(".gz") ? new GZIPOutputStream(fos) : fos;
             BufferedOutputStream bos = new BufferedOutputStream(os);
             OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            campaign.writeToXML(pw);
            pw.flush();
            // delete the backup file because we didn't need it
            if (backupFile.exists() && !backupFile.delete()) {
                LogManager.getLogger().error("Backup file deletion failure. This means that the backup file of "
                        + backupFile.getPath() + " will be retained instead of being properly deleted.");
            }
            LogManager.getLogger().info("Campaign saved to " + file);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(frame,
                    "Oh no! The program was unable to correctly save your game. We know this\n"
                            + "is annoying and apologize. Please help us out and submit a bug with the\n"
                            + "mekhq.log file from this game so we can prevent this from happening in\n"
                            + "the future.", "Could not save game",
                    JOptionPane.ERROR_MESSAGE);

            // restore the backup file
            if (file.delete()) {
                if (backupFile.exists()) {
                    Utilities.copyfile(backupFile, file);
                    if (!backupFile.delete()) {
                        LogManager.getLogger().error("Backup file deletion failure after restoring the original file. This means that the backup file of "
                                + backupFile.getPath() + " will be retained instead of being properly deleted.");
                    }
                }
            } else {
                LogManager.getLogger().error(String.format(
                        "File deletion failure. This means that the file at %s will be retained instead of being properly deleted, with any backup at %s not being restored nor deleted.",
                        file.getPath(), backupFile.getPath()));
            }

            return false;
        }

        return true;
    }

    /**
     * @param evt the event triggering the opening of the Campaign Options Dialog
     */
    private void menuOptionsActionPerformed(final ActionEvent evt) {
        final CampaignOptions oldOptions = getCampaign().getCampaignOptions();
        // We need to handle it like this for now, as the options above get written to currently
        boolean atb = oldOptions.isUseAtB();
        boolean timeIn = oldOptions.isUseTimeInService();
        boolean rankIn = oldOptions.isUseTimeInRank();
        boolean staticRATs = oldOptions.isUseStaticRATs();
        boolean factionIntroDate = oldOptions.isFactionIntroDate();
        final RandomDeathMethod randomDeathMethod = oldOptions.getRandomDeathMethod();
        final boolean useRandomDeathSuicideCause = oldOptions.isUseRandomDeathSuicideCause();
        final RandomDivorceMethod randomDivorceMethod = oldOptions.getRandomDivorceMethod();
        final RandomMarriageMethod randomMarriageMethod = oldOptions.getRandomMarriageMethod();
        final RandomProcreationMethod randomProcreationMethod = oldOptions.getRandomProcreationMethod();
        final boolean retirementDateTracking = oldOptions.isUseRetirementDateTracking();
        final CampaignOptionsDialog cod = new CampaignOptionsDialog(getFrame(), getCampaign(), false);
        cod.setVisible(true);

        final CampaignOptions newOptions = getCampaign().getCampaignOptions();

        if (timeIn != newOptions.isUseTimeInService()) {
            if (newOptions.isUseTimeInService()) {
                getCampaign().initTimeInService();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setRecruitment(null);
                }
            }
        }

        if (rankIn != newOptions.isUseTimeInRank()) {
            if (newOptions.isUseTimeInRank()) {
                getCampaign().initTimeInRank();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setLastRankChangeDate(null);
                }
            }
        }

        if ((randomDeathMethod != newOptions.getRandomDeathMethod())
                || (useRandomDeathSuicideCause != newOptions.isUseRandomDeathSuicideCause())) {
            getCampaign().setDeath(newOptions.getRandomDeathMethod().getMethod(newOptions));
        } else {
            getCampaign().getDeath().setUseRandomClanPersonnelDeath(newOptions.isUseRandomClanPersonnelDeath());
            getCampaign().getDeath().setUseRandomPrisonerDeath(newOptions.isUseRandomPrisonerDeath());
            switch (getCampaign().getDeath().getMethod()) {
                case PERCENTAGE:
                    ((PercentageRandomDeath) getCampaign().getDeath()).setPercentage(
                            newOptions.getPercentageRandomDeathChance());
                    break;
                case EXPONENTIAL:
                    ((ExponentialRandomDeath) getCampaign().getDeath()).setMale(
                            newOptions.getExponentialRandomDeathMaleValues());
                    ((ExponentialRandomDeath) getCampaign().getDeath()).setFemale(
                            newOptions.getExponentialRandomDeathFemaleValues());
                    break;
                case AGE_RANGE:
                    ((AgeRangeRandomDeath) getCampaign().getDeath()).adjustRangeValues(newOptions);
                    break;
                default:
                    break;
            }
        }

        if (randomDivorceMethod != newOptions.getRandomDivorceMethod()) {
            getCampaign().setDivorce(newOptions.getRandomDivorceMethod().getMethod(newOptions));
        } else {
            getCampaign().getDivorce().setUseClanPersonnelDivorce(newOptions.isUseClanPersonnelDivorce());
            getCampaign().getDivorce().setUsePrisonerDivorce(newOptions.isUsePrisonerDivorce());
            getCampaign().getDivorce().setUseRandomOppositeSexDivorce(newOptions.isUseRandomOppositeSexDivorce());
            getCampaign().getDivorce().setUseRandomSameSexDivorce(newOptions.isUseRandomSameSexDivorce());
            getCampaign().getDivorce().setUseRandomClanPersonnelDivorce(newOptions.isUseRandomClanPersonnelDivorce());
            getCampaign().getDivorce().setUseRandomPrisonerDivorce(newOptions.isUseRandomPrisonerDivorce());
            if (getCampaign().getDivorce().getMethod().isPercentage()) {
                ((PercentageRandomDivorce) getCampaign().getDivorce()).setOppositeSexPercentage(
                        newOptions.getPercentageRandomDivorceOppositeSexChance());
                ((PercentageRandomDivorce) getCampaign().getDivorce()).setSameSexPercentage(
                        newOptions.getPercentageRandomDivorceSameSexChance());
            }
        }

        if (randomMarriageMethod != newOptions.getRandomMarriageMethod()) {
            getCampaign().setMarriage(newOptions.getRandomMarriageMethod().getMethod(newOptions));
        } else {
            getCampaign().getMarriage().setUseClanPersonnelMarriages(newOptions.isUseClanPersonnelMarriages());
            getCampaign().getMarriage().setUsePrisonerMarriages(newOptions.isUsePrisonerMarriages());
            getCampaign().getMarriage().setUseRandomSameSexMarriages(newOptions.isUseRandomSameSexMarriages());
            getCampaign().getMarriage().setUseRandomClanPersonnelMarriages(newOptions.isUseRandomClanPersonnelMarriages());
            getCampaign().getMarriage().setUseRandomPrisonerMarriages(newOptions.isUseRandomPrisonerMarriages());
            if (getCampaign().getMarriage().getMethod().isPercentage()) {
                ((PercentageRandomMarriage) getCampaign().getMarriage()).setOppositeSexPercentage(
                        newOptions.getPercentageRandomMarriageOppositeSexChance());
                ((PercentageRandomMarriage) getCampaign().getMarriage()).setSameSexPercentage(
                        newOptions.getPercentageRandomMarriageSameSexChance());
            }
        }

        if (randomProcreationMethod != newOptions.getRandomProcreationMethod()) {
            getCampaign().setProcreation(newOptions.getRandomProcreationMethod().getMethod(newOptions));
        } else {
            getCampaign().getProcreation().setUseClanPersonnelProcreation(newOptions.isUseClanPersonnelProcreation());
            getCampaign().getProcreation().setUsePrisonerProcreation(newOptions.isUsePrisonerProcreation());
            getCampaign().getProcreation().setUseRelationshiplessProcreation(newOptions.isUseRelationshiplessRandomProcreation());
            getCampaign().getProcreation().setUseRandomClanPersonnelProcreation(newOptions.isUseRandomClanPersonnelProcreation());
            getCampaign().getProcreation().setUseRandomPrisonerProcreation(newOptions.isUseRandomPrisonerProcreation());
            if (getCampaign().getProcreation().getMethod().isPercentage()) {
                ((PercentageRandomProcreation) getCampaign().getProcreation()).setPercentage(
                        newOptions.getPercentageRandomProcreationRelationshipChance());
                ((PercentageRandomProcreation) getCampaign().getProcreation()).setRelationshiplessPercentage(
                        newOptions.getPercentageRandomProcreationRelationshiplessChance());
            }
        }

        // Clear Procreation Data if Disabled
        if (!newOptions.isUseManualProcreation() && newOptions.getRandomProcreationMethod().isNone()) {
            getCampaign().getPersonnel().parallelStream().filter(Person::isPregnant)
                    .forEach(person -> getCampaign().getProcreation().removePregnancy(person));
        }

        if (retirementDateTracking != newOptions.isUseRetirementDateTracking()) {
            if (newOptions.isUseRetirementDateTracking()) {
                getCampaign().initRetirementDateTracking();
            } else {
                for (Person person : getCampaign().getPersonnel()) {
                    person.setRetirement(null);
                }
            }
        }

        miPersonnelMarket.setVisible(!getCampaign().getPersonnelMarket().isNone());

        final AbstractUnitMarket unitMarket = getCampaign().getUnitMarket();
        if (unitMarket.getMethod() != newOptions.getUnitMarketMethod()) {
            getCampaign().setUnitMarket(newOptions.getUnitMarketMethod().getUnitMarket());
            getCampaign().getUnitMarket().setOffers(unitMarket.getOffers());
            miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        }

        if (atb != newOptions.isUseAtB()) {
            if (newOptions.isUseAtB()) {
                getCampaign().initAtB(false);
                //refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign().getForces()));
            }
            miContractMarket.setVisible(newOptions.isUseAtB());
            miShipSearch.setVisible(newOptions.isUseAtB());
            if (newOptions.isUseAtB()) {
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

        if (staticRATs != newOptions.isUseStaticRATs()) {
            getCampaign().initUnitGenerator();
        }

        if (factionIntroDate != newOptions.isFactionIntroDate()) {
            getCampaign().updateTechFactionCode();
        }
        refreshCalendar();
        getCampaign().reloadNews();
    }

    public void refitUnit(Refit r, boolean selectModelName) {
        if (r.getOriginalEntity() instanceof Dropship || r.getOriginalEntity() instanceof Jumpship) {
            Person engineer = r.getOriginalUnit().getEngineer();
            if (engineer == null) {
                JOptionPane.showMessageDialog(frame,
                        "You cannot refit a ship that does not have an engineer. Assign a qualified vessel crew to this unit.",
                        "No Engineer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            r.setTech(engineer);
        } else if (getCampaign().getActivePersonnel().stream().anyMatch(Person::isTech)) {
            String name;
            Map<String, Person> techHash = new HashMap<>();
            List<String> techList = new ArrayList<>();

            List<Person> techs = getCampaign().getTechs(false, true);
            int lastRightTech = 0;

            for (Person tech : techs) {
                if (getCampaign().isWorkingOnRefit(tech) || tech.isEngineer()) {
                    continue;
                }
                name = tech.getFullName() + ", " + tech.getSkillLevel(getCampaign(), false) + " "
                        + tech.getPrimaryRoleDesc() + " ("
                        + getCampaign().getTargetFor(r, tech).getValueAsString() + "+), "
                        + tech.getMinutesLeft() + "/" + tech.getDailyAvailableTechTime() + " minutes";
                techHash.put(name, tech);
                if (tech.isRightTechTypeFor(r)) {
                    techList.add(lastRightTech++, name);
                } else {
                    techList.add(name);
                }
            }

            String s = (techList.isEmpty()) ? null : (String) JOptionPane.showInputDialog(frame,
                    "Which tech should work on the refit?", "Select Tech",
                    JOptionPane.PLAIN_MESSAGE, null, techList.toArray(), techList.get(0));

            if (null == s) {
                return;
            }

            Person selectedTech = techHash.get(s);

            if (!selectedTech.isRightTechTypeFor(r)) {
                if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(null,
                        "This tech is not appropriate for this unit. Would you like to continue?",
                        "Incorrect Tech Type", JOptionPane.YES_NO_OPTION)) {
                    return;
                }
            }

            r.setTech(selectedTech);
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
                r.setTech(null);
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
            JOptionPane.showMessageDialog(null,
                    "For some reason, the unit you are trying to customize cannot be loaded\n and so the customization was cancelled. Please report the bug with a description\nof the unit being customized.",
                    "Could not customize unit", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "IO Exception",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        getCampaign().refit(r);
        if (hasTab(MHQTabType.MEK_LAB)) {
            ((MekLabTab) getTab(MHQTabType.MEK_LAB)).clearUnit();
        }
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
        if (((PersonnelTab) getTab(MHQTabType.PERSONNEL)).getPersonnelTable().getRowCount() != 0) {
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
                            report = Utilities.exportTableToCSV(((PersonnelTab) getTab(MHQTabType.PERSONNEL)).getPersonnelTable(), file);
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
        if (((HangarTab) getTab(MHQTabType.HANGAR)).getUnitTable().getRowCount() != 0) {
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
                            report = Utilities.exportTableToCSV(((HangarTab) getTab(MHQTabType.HANGAR)).getUnitTable(), file);
                        } else {
                            report = "Unsupported FileType in Export Units";
                        }
                        JOptionPane.showMessageDialog(tabMain, report);
                    });
        } else {
            JOptionPane.showMessageDialog(tabMain, resourceMap.getString("dlgNoUnits.text"));
        }
    }

     /**
     * Exports Finances to a file (CSV, XML, etc.)
     * @param format        file format to export to
     * @param dialogTitle   title of the dialog frame
     * @param filename      file name to save to
     */
    protected void exportFinances(FileType format, String dialogTitle, String filename) {
        if (!getCampaign().getFinances().getTransactions().isEmpty()) {
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

    protected void loadListFile(final boolean allowNewPilots) {
        final File unitFile = FileDialogs.openUnits(getFrame()).orElse(null);
        if (unitFile != null) {
            try {
                for (Entity entity : new MULParser(unitFile, getCampaign().getGameOptions()).getEntities()) {
                    getCampaign().addNewUnit(entity, allowNewPilots, 0);
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
    }

    protected void loadPersonFile() {
        File personnelFile = FileDialogs.openPersonnel(frame).orElse(null);

        if (personnelFile != null) {
            LogManager.getLogger().info("Starting load of personnel file from XML...");
            // Initialize variables.
            Document xmlDoc;

            // Open the file
            try (InputStream is = new FileInputStream(personnelFile)) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(is);
            } catch (Exception ex) {
                LogManager.getLogger().error("Cannot load person XML", ex);
                return; // otherwise we NPE out in the next line
            }

            Element personnelEle = xmlDoc.getDocumentElement();
            NodeList nl = personnelEle.getChildNodes();

            // Get rid of empty text nodes and adjacent text nodes...
            // Stupid weird parsing of XML. At least this cleans it up.
            personnelEle.normalize();

            final Version version = new Version(personnelEle.getAttribute("version"));

            // we need to iterate through three times, the first time to collect
            // any custom units that might not be written yet
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                // If it's not an element node, we ignore it.
                if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (!wn2.getNodeName().equalsIgnoreCase("person")) {
                    LogManager.getLogger().error("Unknown node type not loaded in Personnel nodes: " + wn2.getNodeName());
                    continue;
                }

                Person p = Person.generateInstanceFromXML(wn2, getCampaign(), version);
                if ((p != null) && (getCampaign().getPerson(p.getId()) != null)) {
                    LogManager.getLogger().error("ERROR: Cannot load person who exists, ignoring. (Name: "
                            + p.getFullName() + ", Id " + p.getId() + ")");
                    p = null;
                }

                if (p != null) {
                    getCampaign().recruitPerson(p, true);

                    // Clear some values we no longer should have set in case this
                    // has transferred campaigns or things in the campaign have
                    // changed...
                    p.setUnit(null);
                    p.clearTechUnits();
                }
            }

            // Fix Spouse Id Information - This is required to fix spouse NPEs where one doesn't export
            // both members of the couple
            // TODO : make it so that exports will automatically include both spouses
            for (Person p : getCampaign().getActivePersonnel()) {
                if (p.getGenealogy().hasSpouse()
                        && !getCampaign().getPersonnel().contains(p.getGenealogy().getSpouse())) {
                    // If this happens, we need to clear the spouse
                    if (p.getMaidenName() != null) {
                        p.setSurname(p.getMaidenName());
                    }

                    p.getGenealogy().setSpouse(null);
                }

                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null)
                            && !getCampaign().getPersonnel().contains(getCampaign().getPerson(fatherId))) {
                        p.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, null);
                    }
                }
            }

            LogManager.getLogger().info("Finished load of personnel file");
        }
    }

    public void savePersonFile() {
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

        PersonnelTab pt = getPersonnelTab();
        if (pt == null) {
            LogManager.getLogger().error("Cannot export person if there's not a personnel tab");
            return;
        }
        int row = pt.getPersonnelTable().getSelectedRow();
        if (row < 0) {
            LogManager.getLogger().warn("Cannot export person if no one is selected! Ignoring.");
            return;
        }
        Person selectedPerson = pt.getPersonModel().getPerson(pt.getPersonnelTable()
                .convertRowIndexToModel(row));
        int[] rows = pt.getPersonnelTable().getSelectedRows();
        Person[] people = Arrays.stream(rows)
                .mapToObj(j -> pt.getPersonModel().getPerson(pt.getPersonnelTable().convertRowIndexToModel(j)))
                .toArray(Person[]::new);

        // Then save it out to that file.
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            // Start the XML root.
            pw.println("<personnel version=\"" + MHQConstants.VERSION + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    people[i].writeToXML(pw, 1, getCampaign());
                }
            } else {
                selectedPerson.writeToXML(pw, 1, getCampaign());
            }
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.println("</personnel>");
            pw.flush();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            LogManager.getLogger().info("Personnel saved to " + file);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(getFrame(),
                    "Oh no! The program was unable to correctly export your personnel. We know this\n"
                            + "is annoying and apologize. Please help us out and submit a bug with the\n"
                            + "mekhq.log file from this game so we can prevent this from happening in\n"
                            + "the future.",
                    "Could not export personnel", JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    protected void loadPartsFile() {
        Optional<File> maybeFile = FileDialogs.openParts(frame);

        if (maybeFile.isEmpty()) {
            return;
        }

        File partsFile = maybeFile.get();

        LogManager.getLogger().info("Starting load of parts file from XML...");
        // Initialize variables.
        Document xmlDoc;

        // Open up the file.
        try (InputStream is = new FileInputStream(partsFile)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return;
        }

        Element partsEle = xmlDoc.getDocumentElement();
        NodeList nl = partsEle.getChildNodes();

        // Get rid of empty text nodes and adjacent text nodes...
        // Stupid weird parsing of XML. At least this cleans it up.
        partsEle.normalize();

        final Version version = new Version(partsEle.getAttribute("version"));

        // we need to iterate through three times, the first time to collect
        // any custom units that might not be written yet
        List<Part> parts = new ArrayList<>();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("part")) {
                // Error condition of sorts!
                // Errr, what should we do here?
                LogManager.getLogger().error("Unknown node type not loaded in Parts nodes: " + wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                parts.add(p);
            }
        }

        getCampaign().importParts(parts);
        LogManager.getLogger().info("Finished load of parts file");
    }

    public void savePartsFile() {
        Optional<File> maybeFile = FileDialogs.saveParts(frame, getCampaign());

        if (maybeFile.isEmpty()) {
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

        // Get the information to export
        final WarehouseTab warehouseTab = getWarehouseTab();
        if (warehouseTab == null) {
            LogManager.getLogger().error("Cannot export parts for a null warehouse tab");
            return;
        }

        JTable partsTable = warehouseTab.getPartsTable();
        PartsTableModel partsModel = warehouseTab.getPartsModel();
        int row = partsTable.getSelectedRow();
        if (row < 0) {
            LogManager.getLogger().warn("Cannot export parts if none are selected! Ignoring.");
            return;
        }
        Part selectedPart = partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
        int[] rows = partsTable.getSelectedRows();
        Part[] parts = Arrays.stream(rows)
                .mapToObj(j -> partsModel.getPartAt(partsTable.convertRowIndexToModel(j)))
                .toArray(Part[]::new);

        // Then save it out to the file.
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            // File header
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

            // Start the XML root.
            pw.println("<parts version=\"" + MHQConstants.VERSION + "\">");

            if (rows.length > 1) {
                for (int i = 0; i < rows.length; i++) {
                    parts[i].writeToXML(pw, 1);
                }
            } else {
                selectedPart.writeToXML(pw, 1);
            }
            // Okay, we're done.
            // Close everything out and be done with it.
            pw.println("</parts>");
            pw.flush();
            // delete the backup file because we didn't need it
            if (backupFile.exists()) {
                backupFile.delete();
            }
            LogManager.getLogger().info("Parts saved to " + file);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(getFrame(),
                    "Oh no! The program was unable to correctly export your parts. We know this\n"
                            + "is annoying and apologize. Please help us out and submit a bug with the\n"
                            + "mekhq.log file from this game so we can prevent this from happening in\n"
                            + "the future.", "Could not export parts", JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
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
            ((CampaignGuiTab) tabMain.getComponentAt(i)).refreshAll();
        }
    }

    public void refreshLab() {
        MekLabTab lab = (MekLabTab) getTab(MHQTabType.MEK_LAB);
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
            // put a try-catch here so that bugs in the meklab don't screw up other stuff
            try {
                lab.refreshRefitSummary();
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
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
            // FIXME : Localize
            inDebt = " <font color='red'>(in Debt)</font>";
        }
        // FIXME : Localize
        String text = "<html><b>Funds</b>: " + funds.toAmountAndSymbolString() + inDebt + "</html>";
        lblFunds.setText(text);
    }

    private void refreshTempAstechs() {
        // FIXME : Localize
        String text = "<html><b>Temp Astechs</b>: " + getCampaign().getAstechPool() + "</html>";
        lblTempAstechs.setText(text);
    }

    private void refreshTempMedics() {
        // FIXME : Localize
        String text = "<html><b>Temp Medics</b>: " + getCampaign().getMedicPool() + "</html>";
        lblTempMedics.setText(text);
    }

    private void refreshPartsAvailability() {
        if (!getCampaign().getCampaignOptions().isUseAtB()
                || CampaignOptions.S_AUTO.equals(getCampaign().getCampaignOptions().getAcquisitionSkill())) {
            lblPartsAvailabilityRating.setText("");
        } else {
            StringBuilder report = new StringBuilder();
            int partsAvailability = getCampaign().findAtBPartsAvailabilityLevel(null, report);
            // FIXME : Localize
            lblPartsAvailabilityRating.setText("<html><b>Campaign Parts Availability</b>: " + partsAvailability + "</html>");
        }
    }

    private ActionScheduler fundsScheduler = new ActionScheduler(this::refreshFunds);

    public void refreshLocation() {
        lblLocation.setText(getCampaign().getLocation().getReport(getCampaign().getLocalDate()));
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

    //region Subscriptions
    @Subscribe
    public void handleDayEnding(DayEndingEvent evt) {
        // first check for overdue loan payments - don't allow advancement until
        // these are addressed
        if (getCampaign().checkOverDueLoans()) {
            refreshFunds();
            // FIXME : Localize
            JOptionPane.showMessageDialog(null, "You must resolve overdue loans before advancing the day",
                    "Overdue loans", JOptionPane.WARNING_MESSAGE);
            evt.cancel();
            return;
        }

        if (getCampaign().checkRetirementDefections()) {
            showRetirementDefectionDialog();
            evt.cancel();
            return;
        }

        if (getCampaign().checkYearlyRetirements()) {
            showRetirementDefectionDialog();
            evt.cancel();
            return;
        }

        if(getCampaign().checkScenariosDue()) {
            JOptionPane.showMessageDialog(null, getResourceMap().getString("dialogCheckDueScenarios.text"),
                    getResourceMap().getString("dialogCheckDueScenarios.title"), JOptionPane.WARNING_MESSAGE);
            evt.cancel();
            return;
        }

        if (new UnmaintainedUnitsNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new PregnantCombatantNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new PrisonersNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new UntreatedPersonnelNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new CargoCapacityNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new InsufficientAstechsNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new InsufficientAstechTimeNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (new InsufficientMedicsNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
            evt.cancel();
            return;
        }

        if (getCampaign().getCampaignOptions().isUseAtB()) {
            if (new ShortDeploymentNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
                evt.cancel();
                return;
            }

            if (new UnresolvedStratConContactsNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
                evt.cancel();
                return;
            }

            if (new OutstandingScenariosNagDialog(getFrame(), getCampaign()).showDialog().isCancelled()) {
                evt.cancel();
                return;
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
    public void handle(final OptionsChangedEvent evt) {
        if (!getCampaign().getCampaignOptions().isUseStratCon() && (getTab(MHQTabType.STRAT_CON) != null)) {
            removeStandardTab(MHQTabType.STRAT_CON);
        } else if (getCampaign().getCampaignOptions().isUseStratCon() && (getTab(MHQTabType.STRAT_CON) == null)) {
            addStandardTab(MHQTabType.STRAT_CON);
        }

        refreshAllTabs();
        fundsScheduler.schedule();
        refreshPartsAvailability();

        miRetirementDefectionDialog.setVisible(!evt.getOptions().getRandomRetirementMethod().isNone());
        miUnitMarket.setVisible(!evt.getOptions().getUnitMarketMethod().isNone());
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
        if (ev.getPerson().hasRole(PersonnelRole.ADMINISTRATOR_LOGISTICS)) {
            refreshPartsAvailability();
        }
    }

    @Subscribe
    public void handle(final MHQOptionsChangedEvent evt) {
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
    }
    //endregion Subscriptions
}
