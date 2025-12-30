/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.force.Force.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.getExperienceLevelName;
import static mekhq.gui.dialog.nagDialogs.NagController.triggerDailyNags;
import static mekhq.gui.enums.MHQTabType.COMMAND_CENTER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.xml.parsers.DocumentBuilder;

import megamek.Version;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.logging.MMLogger;
import mekhq.IconPackage;
import mekhq.MHQConstants;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.AsTechPoolChangedEvent;
import mekhq.campaign.events.DayEndingEvent;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.events.MedicPoolChangedEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.events.assets.AssetEvent;
import mekhq.campaign.events.loans.LoanEvent;
import mekhq.campaign.events.missions.MissionEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.transactions.TransactionEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.divorce.RandomDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.campaign.personnel.marriage.RandomMarriage;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.GoingRogue;
import mekhq.campaign.utilities.AutomatedTechAssignments;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.baseComponents.roundedComponents.RoundedMMToggleButton;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.CampaignExportWizard.CampaignExportWizardState;
import mekhq.gui.dialog.glossary.NewGlossaryDialog;
import mekhq.gui.dialog.reportDialogs.CargoReportDialog;
import mekhq.gui.dialog.reportDialogs.HangarReportDialog;
import mekhq.gui.dialog.reportDialogs.PersonnelReportDialog;
import mekhq.gui.dialog.reportDialogs.ReputationReportDialog;
import mekhq.gui.dialog.reportDialogs.TransportReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.PartsTableModel;
import mekhq.io.FileType;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {
    private static final MMLogger logger = MMLogger.create(CampaignGUI.class);

    @Serial
    private static final long serialVersionUID = 3126634639249129512L;
    // region Variable Declarations
    public static final int MAX_START_WIDTH = 1400;
    public static final int MAX_START_HEIGHT = 900;
    // the max quantity when mass purchasing parts, hiring, etc. using the JSpinner
    public static final int MAX_QUANTITY_SPINNER = 10000;

    private JFrame frame;

    private final MekHQ app;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    /* for the main panel */
    private EnhancedTabbedPane tabMain;

    /* For the menu bar */
    private JMenuBar menuBar;
    private JMenu menuThemes;
    private JMenuItem miRetirementDefectionDialog;
    private JMenuItem miAwardEligibilityDialog;
    private JMenuItem miCompanyGenerator;

    private final EnumMap<MHQTabType, CampaignGuiTab> standardTabs;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblLocation;
    private JLabel lblFunds;
    private JLabel lblTempAsTechs;
    private JLabel lblTempMedics;
    private JLabel lblPartsAvailabilityRating;

    /* for the top button panel */
    private JPanel pnlTop;
    private final RoundedJButton btnAdvanceMultipleDays = new RoundedJButton(resourceMap.getString(
          "btnAdvanceMultipleDays.text"));
    private final RoundedJButton btnMassTraining = new RoundedJButton(resourceMap.getString("btnMassTraining.text"));
    private final RoundedMMToggleButton btnGMMode = new RoundedMMToggleButton(resourceMap.getString("btnGMMode.text"));
    private final RoundedMMToggleButton btnOvertime = new RoundedMMToggleButton(resourceMap.getString("btnOvertime.text"));
    private final RoundedJButton btnGoRogue = new RoundedJButton(resourceMap.getString("btnGoRogue.text"));
    private final RoundedJButton btnCompanyGenerator = new RoundedJButton(resourceMap.getString(
          "btnCompanyGenerator.text"));
    private final RoundedJButton btnGlossary = new RoundedJButton(resourceMap.getString("btnGlossary.text"));
    private final RoundedJButton btnBugReport = new RoundedJButton(resourceMap.getString("btnBugReport.text"));
    private final RoundedJButton btnContractMarket =
          new RoundedJButton(resourceMap.getString("btnContractMarket.market"));
    private final RoundedJButton btnPersonnelMarket =
          new RoundedJButton(resourceMap.getString("btnPersonnelMarket.market"));
    private final RoundedJButton btnUnitMarket = new RoundedJButton(resourceMap.getString("btnUnitMarket.market"));
    private final RoundedJButton btnPartsMarket = new RoundedJButton(resourceMap.getString("btnPartsMarket.manual"));

    ReportHyperlinkListener reportHLL;

    private boolean logNagActive = false;

    private transient StandardForceIcon copyForceIcon = null;
    // endregion Variable Declarations

    // region Constructors
    public CampaignGUI(MekHQ app) {
        this.app = app;
        reportHLL = new ReportHyperlinkListener(this);
        standardTabs = new EnumMap<>(MHQTabType.class);
        initComponents();
        MekHQ.registerHandler(this);
        setUserPreferences();
    }
    // endregion Constructors

    // region Getters/Setters
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

    public EnhancedTabbedPane getTabMain() {
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
    // endregion Getters/Setters

    // region Initialization
    private void initComponents() {
        frame = new JFrame("MekHQ");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        tabMain = new EnhancedTabbedPane(true, true);
        tabMain.setToolTipText("");
        tabMain.setMinimumSize(new Dimension(600, 200));
        tabMain.setPreferredSize(new Dimension(900, 300));

        addStandardTab(COMMAND_CENTER);
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

        boolean isMaplessMode = getCampaign().getCampaignOptions().isUseStratConMaplessMode();
        int stratConTabIndex = tabMain.indexOfTab(MHQTabType.STRAT_CON.toString());

        if (stratConTabIndex != -1) {
            tabMain.setEnabledAt(stratConTabIndex, !isMaplessMode);
        }

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
        add(pnlTop, BorderLayout.PAGE_START);
        add(statusPanel, BorderLayout.PAGE_END);

        standardTabs.values().forEach(CampaignGuiTab::refreshAll);

        refreshCalendar();
        refreshFunds();
        refreshLocation();
        refreshTempAsTechs();
        refreshTempMedics();
        refreshPartsAvailability();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(Math.min(MAX_START_WIDTH, dim.width), Math.min(MAX_START_HEIGHT, dim.height));

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

        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                getApplication().exit(true);
            }
        });

        CommandCenterTab commandCenter = getCommandCenterTab();
        for (DailyReportType type : DailyReportType.values()) {
            commandCenter.clearDailyReportNag(type.getTabIndex());
        }
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CampaignGUI.class);
            frame.setName("mainWindow");
            preferences.manage(new JWindowPreference(frame));
            UIUtil.keepOnScreen(frame);
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    /**
     * This is used to initialize the top menu bar. All the top level menu bar and {@link MHQTabType} mnemonics must be
     * unique, as they are both accessed through the same GUI page. The following mnemonic keys are being used as of
     * 30-MAR-2020: A, B, C, E, F, H, I, L, M, N, O, P, R, S, T, V, W, /
     * <p>
     * Note 1: the slash is used for the help, as it is normally the same key as the ? Note 2: the A mnemonic is used
     * for the Advance Day button
     */
    private void initMenu() {
        // TODO : Implement "Export All" versions for Personnel and Parts
        // See the JavaDoc comment for used mnemonic keys
        menuBar = new JMenuBar();
        menuBar.getAccessibleContext().setAccessibleName("Main Menu");

        // region File Menu
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
        menuNew.addActionListener(evt -> handleInAppNewCampaign());
        menuFile.add(menuNew);

        // region menuImport
        // The Import menu uses the following Mnemonic keys as of 25-MAR-2022:
        // A, C, F, I, P
        JMenu menuImport = new JMenu(resourceMap.getString("menuImport.text"));
        menuImport.setMnemonic(KeyEvent.VK_I);

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
        miImportIndividualRankSystem.addActionListener(evt -> getCampaign().setRankSystem(RankSystem.generateIndividualInstanceFromXML(
              FileDialogs.openIndividualRankSystem(getFrame()).orElse(null))));
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
        // endregion menuImport

        // region menuExport
        // The Export menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, X, S
        JMenu menuExport = new JMenu(resourceMap.getString("menuExport.text"));
        menuExport.setMnemonic(KeyEvent.VK_X);

        // region CSV Export
        // The CSV menu uses the following Mnemonic keys as of 25-MAR-2022:
        // F, P, U
        JMenu miExportCSVFile = new JMenu(resourceMap.getString("menuExportCSV.text"));
        miExportCSVFile.setMnemonic(KeyEvent.VK_C);

        JMenuItem miExportPersonCSV = new JMenuItem(resourceMap.getString("miExportPersonnel.text"));
        miExportPersonCSV.setMnemonic(KeyEvent.VK_P);
        miExportPersonCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miExportPersonCSV.addActionListener(evt -> {
            try {
                exportPersonnel(FileType.CSV,
                      resourceMap.getString("dlgSavePersonnelCSV.text"),
                      getCampaign().getLocalDate()
                            .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                          .withLocale(MekHQ.getMHQOptions().getDateLocale())) + "_ExportedPersonnel");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportPersonCSV);

        JMenuItem miExportUnitCSV = new JMenuItem(resourceMap.getString("miExportUnit.text"));
        miExportUnitCSV.setMnemonic(KeyEvent.VK_U);
        miExportUnitCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miExportUnitCSV.addActionListener(evt -> {
            try {
                exportUnits(FileType.CSV,
                      resourceMap.getString("dlgSaveUnitsCSV.text"),
                      getCampaign().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedUnits");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportUnitCSV);

        JMenuItem miExportFinancesCSV = new JMenuItem(resourceMap.getString("miExportFinances.text"));
        miExportFinancesCSV.setMnemonic(KeyEvent.VK_F);
        miExportFinancesCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
        miExportFinancesCSV.addActionListener(evt -> {
            try {
                exportFinances(FileType.CSV,
                      resourceMap.getString("dlgSaveFinancesCSV.text"),
                      getCampaign().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedFinances");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportCSVFile.add(miExportFinancesCSV);

        menuExport.add(miExportCSVFile);
        // endregion CSV Export

        // region XML Export
        // The XML menu uses the following Mnemonic keys as of 25-MAR-2022:
        // C, I, P, R
        JMenu miExportXMLFile = new JMenu(resourceMap.getString("menuExportXML.text"));
        miExportXMLFile.setMnemonic(KeyEvent.VK_X);

        JMenuItem miExportRankSystems = new JMenuItem(resourceMap.getString("miExportRankSystems.text"));
        miExportRankSystems.setName("miExportRankSystems");
        miExportRankSystems.setMnemonic(KeyEvent.VK_R);
        miExportRankSystems.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miExportRankSystems.addActionListener(evt -> Ranks.exportRankSystemsToFile(FileDialogs.saveRankSystems(getFrame())
                                                                                         .orElse(null),
              getCampaign().getRankSystem()));
        miExportXMLFile.add(miExportRankSystems);

        JMenuItem miExportIndividualRankSystem = new JMenuItem(resourceMap.getString("miExportIndividualRankSystem.text"));
        miExportIndividualRankSystem.setName("miExportIndividualRankSystem");
        miExportIndividualRankSystem.setMnemonic(KeyEvent.VK_I);
        miExportIndividualRankSystem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        miExportIndividualRankSystem.addActionListener(evt -> getCampaign().getRankSystem()
                                                                    .writeToFile(FileDialogs.saveIndividualRankSystem(
                                                                          getFrame()).orElse(null)));
        miExportXMLFile.add(miExportIndividualRankSystem);

        JMenuItem miExportPlanetsXML = new JMenuItem(resourceMap.getString("miExportPlanets.text"));
        miExportPlanetsXML.setMnemonic(KeyEvent.VK_P);
        miExportPlanetsXML.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miExportPlanetsXML.addActionListener(evt -> {
            try {
                exportPlanets(FileType.XML,
                      resourceMap.getString("dlgSavePlanetsXML.text"),
                      getCampaign().getName() +
                            getCampaign().getLocalDate()
                                  .format(DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                                                .withLocale(MekHQ.getMHQOptions().getDateLocale())) +
                            "_ExportedPlanets");
            } catch (Exception ex) {
                logger.error("", ex);
            }
        });
        miExportXMLFile.add(miExportPlanetsXML);

        menuExport.add(miExportXMLFile);
        // endregion XML Export

        JMenuItem miExportCampaignSubset = new JMenuItem(resourceMap.getString("miExportCampaignSubset.text"));
        miExportCampaignSubset.setMnemonic(KeyEvent.VK_S);
        miExportCampaignSubset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        miExportCampaignSubset.addActionListener(evt -> {
            CampaignExportWizard cew = new CampaignExportWizard(getCampaign());
            cew.display(CampaignExportWizardState.ForceSelection);
        });
        menuExport.add(miExportCampaignSubset);

        menuFile.add(menuExport);
        // endregion menuExport

        // region Menu Refresh
        // The Refresh menu uses the following Mnemonic keys as of 12-APR-2022:
        // A, C, D, F, P, R, U
        JMenu menuRefresh = new JMenu(resourceMap.getString("menuRefresh.text"));
        menuRefresh.setMnemonic(KeyEvent.VK_R);

        JMenuItem miRefreshUnitCache = new JMenuItem(resourceMap.getString("miRefreshUnitCache.text"));
        miRefreshUnitCache.setName("miRefreshUnitCache");
        miRefreshUnitCache.setMnemonic(KeyEvent.VK_U);
        miRefreshUnitCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miRefreshUnitCache.addActionListener(evt -> MekSummaryCache.refreshUnitData(false));
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

        JMenuItem miRefreshFinancialInstitutions = new JMenuItem(resourceMap.getString(
              "miRefreshFinancialInstitutions.text"));
        miRefreshFinancialInstitutions.setToolTipText(resourceMap.getString("miRefreshFinancialInstitutions.toolTipText"));
        miRefreshFinancialInstitutions.setName("miRefreshFinancialInstitutions");
        miRefreshFinancialInstitutions.addActionListener(evt -> FinancialInstitutions.initializeFinancialInstitutions());
        menuRefresh.add(miRefreshFinancialInstitutions);

        menuFile.add(menuRefresh);
        // endregion Menu Refresh

        JMenuItem menuOptions = new JMenuItem(resourceMap.getString("menuOptions.text"));
        menuOptions.setMnemonic(KeyEvent.VK_C);
        menuOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        menuOptions.addActionListener(this::menuOptionsActionPerformed);
        menuFile.add(menuOptions);

        final JMenuItem miMHQOptions = new JMenuItem(resourceMap.getString("miMHQOptions.text"));
        miMHQOptions.setToolTipText(resourceMap.getString("miMHQOptions.toolTipText"));
        miMHQOptions.setName("miMHQOptions");
        miMHQOptions.setMnemonic(KeyEvent.VK_H);
        miMHQOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miMHQOptions.addActionListener(evt -> new MHQOptionsDialog(getFrame()).setVisible(true));
        menuFile.add(miMHQOptions);

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

        final JMenuItem miMMClientOptions = new JMenuItem(resourceMap.getString("miMMClientOptions.text"));
        miMMClientOptions.setToolTipText(resourceMap.getString("miMMClientOptions.toolTipText"));
        miMMClientOptions.setName("miMMClientOptions");
        miMMClientOptions.setMnemonic(KeyEvent.VK_O);
        miMMClientOptions.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
        miMMClientOptions.addActionListener(evt -> new CommonSettingsDialog(frame, null).setVisible(true));
        menuFile.add(miMMClientOptions);

        menuThemes = new JMenu(resourceMap.getString("menuThemes.text"));
        menuThemes.setMnemonic(KeyEvent.VK_T);
        refreshThemeChoices();
        menuFile.add(menuThemes);

        JMenuItem menuExitItem = new JMenuItem(resourceMap.getString("menuExit.text"));
        menuExitItem.setMnemonic(KeyEvent.VK_E);
        menuExitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
        menuExitItem.addActionListener(evt -> getApplication().exit(true));
        menuFile.add(menuExitItem);

        menuBar.add(menuFile);
        // endregion File Menu

        // region Marketplace Menu
        // The Marketplace menu uses the following Mnemonic keys as of 19-March-2020:
        // A, B, C, H, M, N, P, R, S, U
        JMenu menuMarket = new JMenu(resourceMap.getString("menuMarket.text"));
        menuMarket.setMnemonic(KeyEvent.VK_M);

        JMenuItem miPersonnelMarket = new JMenuItem(resourceMap.getString("miPersonnelMarket.text"));
        miPersonnelMarket.setMnemonic(KeyEvent.VK_P);
        miPersonnelMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miPersonnelMarket.addActionListener(evt -> hirePersonMarket());
        miPersonnelMarket.setVisible(!getCampaign().getPersonnelMarket().isNone());
        menuMarket.add(miPersonnelMarket);

        JMenuItem miContractMarket = new JMenuItem(resourceMap.getString("miContractMarket.text"));
        miContractMarket.setMnemonic(KeyEvent.VK_C);
        miContractMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miContractMarket.addActionListener(evt -> showContractMarket());
        miContractMarket.setVisible(getCampaign().getCampaignOptions().isUseAtB());
        menuMarket.add(miContractMarket);

        JMenuItem miUnitMarket = new JMenuItem(resourceMap.getString("miUnitMarket.text"));
        miUnitMarket.setMnemonic(KeyEvent.VK_U);
        miUnitMarket.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miUnitMarket.addActionListener(evt -> showUnitMarket());
        miUnitMarket.setVisible(!getCampaign().getUnitMarket().getMethod().isNone());
        menuMarket.add(miUnitMarket);

        JMenuItem miPurchaseUnit = new JMenuItem(resourceMap.getString("miPurchaseUnit.text"));
        miPurchaseUnit.setMnemonic(KeyEvent.VK_N);
        miPurchaseUnit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        miPurchaseUnit.addActionListener(evt -> {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
            if (!MekSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(getFrame(),
                  unitLoadingDialog,
                  getCampaign(),
                  true);
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

        JMenu menuHireCombat = new JMenu(resourceMap.getString("menuHire.combat"));
        JMenu menuHireSupport = new JMenu(resourceMap.getString("menuHire.support"));
        JMenu menuHireCivilian = new JMenu(resourceMap.getString("menuHire.civilian"));

        PersonnelRole[] roles = PersonnelRole.getValuesSortedAlphabetically(getCampaign().isClanCampaign());
        for (PersonnelRole role : roles) {
            // Dependent is handled speciality so that it's always at the top of the civilian category
            if (role.isDependent()) {
                continue;
            }

            JMenuItem miHire = new JMenuItem(role.getLabel(getCampaign().getFaction().isClan()));
            if (role.getMnemonic() != KeyEvent.VK_UNDEFINED) {
                miHire.setMnemonic(role.getMnemonic());
                miHire.setAccelerator(KeyStroke.getKeyStroke(role.getMnemonic(), InputEvent.ALT_DOWN_MASK));
            }
            miHire.setToolTipText(role.getDescription(getCampaign().isClanCampaign()));
            miHire.setActionCommand(role.name());
            miHire.addActionListener(this::hirePerson);

            if (role.isCombat()) {
                menuHireCombat.add(miHire);
            } else if (role.isSupport(true)) {
                menuHireSupport.add(miHire);
            } else if (!role.isDependent()) {
                menuHireCivilian.add(miHire);
            }
        }

        JMenuItem miHire = new JMenuItem(PersonnelRole.DEPENDENT.getLabel(getCampaign().getFaction().isClan()));
        if (PersonnelRole.DEPENDENT.getMnemonic() != KeyEvent.VK_UNDEFINED) {
            miHire.setMnemonic(PersonnelRole.DEPENDENT.getMnemonic());
            miHire.setAccelerator(KeyStroke.getKeyStroke(PersonnelRole.DEPENDENT.getMnemonic(),
                  InputEvent.ALT_DOWN_MASK));
        }
        miHire.setActionCommand(PersonnelRole.DEPENDENT.name());
        miHire.addActionListener(this::hirePerson);
        menuHireCivilian.insert(miHire, 0);

        menuHire.add(menuHireCombat);
        menuHire.add(menuHireSupport);
        menuHire.add(menuHireCivilian);
        menuMarket.add(menuHire);

        // region Astech Pool
        // The Astech Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, F, H
        JMenu menuAsTechPool = new JMenu(resourceMap.getString("menuAstechPool.text"));
        menuAsTechPool.setMnemonic(KeyEvent.VK_A);

        JMenuItem miHireAsTechs = new JMenuItem(resourceMap.getString("miHireAstechs.text"));
        miHireAsTechs.setMnemonic(KeyEvent.VK_H);
        miHireAsTechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miHireAsTechs.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupHireAstechsNum.text"),
                  1,
                  0,
                  CampaignGUI.MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAsTechPool.add(miHireAsTechs);

        JMenuItem miFireAsTechs = new JMenuItem(resourceMap.getString("miFireAstechs.text"));
        miFireAsTechs.setMnemonic(KeyEvent.VK_E);
        miFireAsTechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
        miFireAsTechs.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupFireAstechsNum.text"),
                  1,
                  0,
                  getCampaign().getTemporaryAsTechPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseAsTechPool(popupValueChoiceDialog.getValue());
            }
        });
        menuAsTechPool.add(miFireAsTechs);

        JMenuItem miFullStrengthAsTechs = new JMenuItem(resourceMap.getString("miFullStrengthAstechs.text"));
        miFullStrengthAsTechs.setMnemonic(KeyEvent.VK_B);
        miFullStrengthAsTechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miFullStrengthAsTechs.addActionListener(evt -> getCampaign().resetAsTechPool());
        menuAsTechPool.add(miFullStrengthAsTechs);

        JMenuItem miFireAllAsTechs = new JMenuItem(resourceMap.getString("miFireAllAstechs.text"));
        miFireAllAsTechs.setMnemonic(KeyEvent.VK_R);
        miFireAllAsTechs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miFireAllAsTechs.addActionListener(evt -> getCampaign().emptyAsTechPool());
        menuAsTechPool.add(miFireAllAsTechs);
        menuMarket.add(menuAsTechPool);
        // endregion Astech Pool

        // region Medic Pool
        // The Medic Pool menu uses the following Mnemonic keys as of 19-March-2020:
        // B, E, H, R
        JMenu menuMedicPool = new JMenu(resourceMap.getString("menuMedicPool.text"));
        menuMedicPool.setMnemonic(KeyEvent.VK_M);

        JMenuItem miHireMedics = new JMenuItem(resourceMap.getString("miHireMedics.text"));
        miHireMedics.setMnemonic(KeyEvent.VK_H);
        miHireMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miHireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupHireMedicsNum.text"),
                  1,
                  0,
                  CampaignGUI.MAX_QUANTITY_SPINNER);
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().increaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miHireMedics);

        JMenuItem miFireMedics = new JMenuItem(resourceMap.getString("miFireMedics.text"));
        miFireMedics.setMnemonic(KeyEvent.VK_E);
        miFireMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
        miFireMedics.addActionListener(evt -> {
            PopupValueChoiceDialog popupValueChoiceDialog = new PopupValueChoiceDialog(getFrame(),
                  true,
                  resourceMap.getString("popupFireMedicsNum.text"),
                  1,
                  0,
                  getCampaign().getTemporaryMedicPool());
            popupValueChoiceDialog.setVisible(true);
            if (popupValueChoiceDialog.getValue() >= 0) {
                getCampaign().decreaseMedicPool(popupValueChoiceDialog.getValue());
            }
        });
        menuMedicPool.add(miFireMedics);

        JMenuItem miFullStrengthMedics = new JMenuItem(resourceMap.getString("miFullStrengthMedics.text"));
        miFullStrengthMedics.setMnemonic(KeyEvent.VK_B);
        miFullStrengthMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miFullStrengthMedics.addActionListener(evt -> getCampaign().resetMedicPool());
        menuMedicPool.add(miFullStrengthMedics);

        JMenuItem miFireAllMedics = new JMenuItem(resourceMap.getString("miFireAllMedics.text"));
        miFireAllMedics.setMnemonic(KeyEvent.VK_R);
        miFireAllMedics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miFireAllMedics.addActionListener(evt -> getCampaign().emptyMedicPool());
        menuMedicPool.add(miFireAllMedics);
        menuMarket.add(menuMedicPool);
        // endregion Medic Pool

        menuBar.add(menuMarket);
        // endregion Marketplace Menu

        // region Reports Menu
        // The Reports menu uses the following Mnemonic keys as of 19-March-2020:
        // C, H, P, T, U
        JMenu menuReports = new JMenu(resourceMap.getString("menuReports.text"));
        menuReports.setMnemonic(KeyEvent.VK_E);

        JMenuItem miDragoonsRating = new JMenuItem(resourceMap.getString("miDragoonsRating.text"));
        miDragoonsRating.setMnemonic(KeyEvent.VK_U);
        miDragoonsRating.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        miDragoonsRating.addActionListener(evt -> new ReputationReportDialog(getFrame(), getCampaign()).setVisible(
              true));
        menuReports.add(miDragoonsRating);

        JMenuItem miPersonnelReport = new JMenuItem(resourceMap.getString("miPersonnelReport.text"));
        miPersonnelReport.setMnemonic(KeyEvent.VK_P);
        miPersonnelReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK));
        miPersonnelReport.addActionListener(evt -> new PersonnelReportDialog(getFrame(),
              new PersonnelReport(getCampaign())).setVisible(true));
        menuReports.add(miPersonnelReport);

        JMenuItem miHangarBreakdown = new JMenuItem(resourceMap.getString("miHangarBreakdown.text"));
        miHangarBreakdown.setMnemonic(KeyEvent.VK_H);
        miHangarBreakdown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
        miHangarBreakdown.addActionListener(evt -> new HangarReportDialog(getFrame(),
              new HangarReport(getCampaign())).setVisible(true));
        menuReports.add(miHangarBreakdown);

        JMenuItem miTransportReport = new JMenuItem(resourceMap.getString("miTransportReport.text"));
        miTransportReport.setMnemonic(KeyEvent.VK_T);
        miTransportReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK));
        miTransportReport.addActionListener(evt -> new TransportReportDialog(getFrame(),
              new TransportReport(getCampaign())).setVisible(true));
        menuReports.add(miTransportReport);

        JMenuItem miCargoReport = new JMenuItem(resourceMap.getString("miCargoReport.text"));
        miCargoReport.setMnemonic(KeyEvent.VK_C);
        miCargoReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miCargoReport.addActionListener(evt -> new CargoReportDialog(getFrame(),
              new CargoReport(getCampaign())).setVisible(true));
        menuReports.add(miCargoReport);

        menuBar.add(menuReports);
        // endregion Reports Menu

        // region View Menu
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
        miRetirementDefectionDialog.setVisible(getCampaign().getCampaignOptions().isUseRandomRetirement());
        miRetirementDefectionDialog.addActionListener(evt -> showRetirementDefectionDialog());
        menuView.add(miRetirementDefectionDialog);

        miAwardEligibilityDialog = new JMenuItem(resourceMap.getString("miAwardEligibilityDialog.text"));
        miAwardEligibilityDialog.setMnemonic(KeyEvent.VK_R);
        miAwardEligibilityDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK));
        miAwardEligibilityDialog.setVisible(getCampaign().getCampaignOptions().isEnableAutoAwards());
        miAwardEligibilityDialog.addActionListener(evt -> showAwardEligibilityDialog());
        menuView.add(miAwardEligibilityDialog);

        menuBar.add(menuView);
        // endregion View Menu

        // region Manage Campaign Menu
        // The Manage Campaign menu uses the following Mnemonic keys as of
        // 19-March-2020:
        // A, B, C, G, M, S
        JMenu menuManage = new JMenu(resourceMap.getString("menuManageCampaign.text"));
        menuManage.setMnemonic(KeyEvent.VK_C);
        menuManage.setName("manageMenu");

        JMenuItem miGMToolsDialog = new JMenuItem(resourceMap.getString("miGMToolsDialog.text"));
        miGMToolsDialog.setMnemonic(KeyEvent.VK_G);
        miGMToolsDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.ALT_DOWN_MASK));
        miGMToolsDialog.addActionListener(evt -> new GMToolsDialog(getFrame(), this, null).setVisible(true));
        menuManage.add(miGMToolsDialog);

        JMenuItem miBloodnames = new JMenuItem(resourceMap.getString("miRandomBloodnames.text"));
        miBloodnames.setMnemonic(KeyEvent.VK_B);
        miBloodnames.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_DOWN_MASK));
        miBloodnames.addActionListener(evt -> {
            for (final Person person : getCampaign().getPersonnel()) {
                getCampaign().checkBloodnameAdd(person, false);
            }
        });
        menuManage.add(miBloodnames);

        JMenuItem miScenarioEditor = new JMenuItem(resourceMap.getString("miScenarioEditor.text"));
        miScenarioEditor.setMnemonic(KeyEvent.VK_S);
        miScenarioEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        miScenarioEditor.addActionListener(evt -> new ScenarioTemplateEditorDialog(getFrame()).setVisible(true));
        menuManage.add(miScenarioEditor);

        miCompanyGenerator = new JMenuItem(resourceMap.getString("miCompanyGenerator.text"));
        miCompanyGenerator.setMnemonic(KeyEvent.VK_C);
        miCompanyGenerator.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
        miCompanyGenerator.addActionListener(evt -> new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(
              true));
        menuManage.add(miCompanyGenerator);

        JMenuItem miAutoResolveBehaviorEditor = new JMenuItem(resourceMap.getString("miAutoResolveBehaviorSettings.text"));
        miAutoResolveBehaviorEditor.setMnemonic(KeyEvent.VK_T);
        miAutoResolveBehaviorEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK));
        miAutoResolveBehaviorEditor.addActionListener(evt -> {
            var autoResolveBehaviorSettingsDialog = new AutoResolveBehaviorSettingsDialog(getFrame(), getCampaign());
            autoResolveBehaviorSettingsDialog.setVisible(true);
            autoResolveBehaviorSettingsDialog.pack();
        });

        menuManage.add(miAutoResolveBehaviorEditor);

        menuBar.add(menuManage);
        // endregion Manage Campaign Menu

        // region Help Menu
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
        // endregion Help Menu
    }

    /**
     * Handles a new campaign event triggered from within an existing campaign.
     * <p>
     * This method performs the following actions in sequence:
     * <ul>
     * <li>Prompts the user to save any current progress through a confirmation
     * dialog.</li>
     * <li>If the user chooses to cancel or closes the dialog, the operation is
     * aborted.</li>
     * <li>If the user agrees to save and the save operation fails, the operation is
     * aborted.</li>
     * <li>Unregisters all event handlers associated with the current campaign,
     * including those
     * in the CampaignGUI and tabs.</li>
     * <li>Starts a new campaign by displaying a data loading dialog.</li>
     * </ul>
     * </p>
     */
    private void handleInAppNewCampaign() {
        int decision = new NewCampaignConfirmationDialog().YesNoOption();
        if (decision != JOptionPane.YES_OPTION) {
            return;
        }

        // Prompt the user to save
        int savePrompt = JOptionPane.showConfirmDialog(null,
              resourceMap.getString("savePrompt.text"),
              resourceMap.getString("savePrompt.title"),
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);

        // Abort if the user cancels, closes the dialog, or fails to save
        if (savePrompt == JOptionPane.CANCEL_OPTION ||
                  savePrompt == JOptionPane.CLOSED_OPTION ||
                  (savePrompt == JOptionPane.YES_OPTION && !app.getCampaigngui().saveCampaign(null))) {
            return;
        }

        // Unregister handlers for campaign tabs
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            Component tab = tabMain.getComponentAt(i);
            if (tab instanceof CampaignGuiTab) {
                ((CampaignGuiTab) tab).disposeTab();
            }
        }

        // Unregister other handlers
        MekHQ.unregisterHandler(this);

        if (getCampaign().getStoryArc() != null) {
            MekHQ.unregisterHandler(getCampaign().getStoryArc());
        }

        // Start a new campaign
        new DataLoadingDialog(frame, app, null, null, true).setVisible(true);
    }

    private void initStatusBar() {
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));
        statusPanel.getAccessibleContext().setAccessibleName("Status Bar");

        lblFunds = new JLabel();
        lblTempAsTechs = new JLabel();
        lblTempMedics = new JLabel();
        lblPartsAvailabilityRating = new JLabel();

        statusPanel.add(lblFunds);
        statusPanel.add(lblTempAsTechs);
        statusPanel.add(lblTempMedics);
        statusPanel.add(lblPartsAvailabilityRating);
    }

    /**
     * Initializes and arranges the top button panel and its components in the user interface.
     *
     * <p>This method sets up the location label with a travel status report, applies visual borders, and positions
     * the major controls at the top of the panel using a {@link GridBagLayout}:</p>
     *
     * <ul>
     *   <li>Adds the current location and travel status label.</li>
     *   <li>Places market-related controls beside the location label.</li>
     *   <li>Adds the main button panel for user actions.</li>
     * </ul>
     *
     * <p>Accessibility names and layout constraints are assigned to ensure that the UI is properly arranged and
     * accessible.</p>
     */
    private void initTopButtons() {
        boolean isUseCommandCircuit =
              FactionStandingUtilities.isUseCommandCircuit(getCampaign().isOverridingCommandCircuitRequirements(),
                    getCampaign().isGM(), getCampaign().getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
                    getCampaign().getFactionStandings(), getCampaign().getFutureAtBContracts());

        lblLocation = new JLabel(getCampaign().getLocation()
                                       .getReport(getCampaign().getLocalDate(),
                                             isUseCommandCircuit,
                                             getCampaign().getTransportCostCalculation(EXP_REGULAR)));
        lblLocation.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("currentLocation.title")));

        pnlTop = new JPanel(new GridBagLayout());
        pnlTop.getAccessibleContext().setAccessibleName(getText("currentLocation.title"));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 5);
        pnlTop.add(lblLocation, gridBagConstraints);


        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlTop.add(getMarketButtons(), gridBagConstraints);


        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlTop.add(getButtonPanel(), gridBagConstraints);
    }

    private JPanel getMarketButtons() {
        JPanel pnlButton = new JPanel(new GridBagLayout());
        pnlButton.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("lblMarkets.title")));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        btnContractMarket.addActionListener(e -> showContractMarket());
        btnContractMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnContractMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnContractMarket, gridBagConstraints);

        btnPersonnelMarket.addActionListener(e -> hirePersonMarket());
        btnPersonnelMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPersonnelMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnPersonnelMarket, gridBagConstraints);

        btnUnitMarket.addActionListener(e -> showUnitMarket());
        btnUnitMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnUnitMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnUnitMarket, gridBagConstraints);

        btnPartsMarket.addActionListener(e -> showPartsMarket());
        btnPartsMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPartsMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnPartsMarket, gridBagConstraints);

        refreshMarketButtonLabels();

        return pnlButton;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public void refreshDynamicButtons() {
        refreshMarketButtonLabels();
    }

    public void refreshMarketButtonLabels() {
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        String labelKey = campaignOptions.getContractMarketMethod().isNone()
                                ? "manual" : "market";
        String label = resourceMap.getString("btnContractMarket." + labelKey);

        btnContractMarket.setText(label);

        PersonnelMarketStyle marketStyle = campaignOptions.getPersonnelMarketStyle();
        labelKey = (marketStyle == PERSONNEL_MARKET_DISABLED && getCampaign().getPersonnelMarket().isNone())
                         ? "manual" : "market";
        label = resourceMap.getString("btnPersonnelMarket." + labelKey);
        btnPersonnelMarket.setText(label);

        labelKey = getCampaign().getUnitMarket().getMethod().isNone() ? "manual" : "market";
        label = resourceMap.getString("btnUnitMarket." + labelKey);
        btnUnitMarket.setText(label);
    }

    private JPanel getButtonPanel() {
        JPanel pnlButton = new JPanel(new GridBagLayout());
        pnlButton.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("campaignControls.title")));

        GridBagConstraints gridBagConstraints;
        btnGlossary.addActionListener(evt -> new NewGlossaryDialog(getFrame()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        pnlButton.add(btnGlossary, gridBagConstraints);

        btnGoRogue.addActionListener(e -> new GoingRogue(getCampaign(), getCampaign().getCommander(),
              getCampaign().getSecondInCommand()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        pnlButton.add(btnGoRogue, gridBagConstraints);

        btnCompanyGenerator.addActionListener(e -> new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(
              true));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        pnlButton.add(btnCompanyGenerator, gridBagConstraints);

        btnGMMode.setToolTipText(resourceMap.getString("btnGMMode.toolTipText"));
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(e -> getCampaign().setGMMode(btnGMMode.isSelected()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        pnlButton.add(btnGMMode, gridBagConstraints);

        btnOvertime.setToolTipText(resourceMap.getString("btnOvertime.toolTipText"));
        btnOvertime.setSelected(getCampaign().isOvertimeAllowed());
        btnOvertime.addActionListener(evt -> getCampaign().setOvertime(btnOvertime.isSelected()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 15);
        pnlButton.add(btnOvertime, gridBagConstraints);

        btnAdvanceMultipleDays.addActionListener(e -> new AdvanceDaysDialog(getFrame(), this).setVisible(true));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnAdvanceMultipleDays, gridBagConstraints);

        btnMassTraining.setToolTipText(resourceMap.getString("btnMassTraining.toolTipText"));
        btnMassTraining.addActionListener(e -> new BatchXPDialog(getFrame(), getCampaign()).setVisible(true));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        pnlButton.add(btnMassTraining, gridBagConstraints);

        // This button uses a mnemonic that is unique and listed in the initMenu JavaDoc
        String padding = "       ";
        RoundedJButton btnAdvanceDay = new RoundedJButton(padding +
                                                                resourceMap.getString("btnAdvanceDay.text") +
                                                                padding);
        btnAdvanceDay.setToolTipText(resourceMap.getString("btnAdvanceDay.toolTipText"));
        btnAdvanceDay.addActionListener(evt -> {
            // We disable the button here, as we don't want the user to be able to advance
            // day  again, until after Advance Day has completed.
            btnAdvanceDay.setEnabled(false);
            btnAdvanceMultipleDays.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    getCampaignController().advanceDay();
                } finally {
                    btnAdvanceDay.setEnabled(true);
                    btnAdvanceMultipleDays.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            });
        });
        btnAdvanceDay.setMnemonic(KeyEvent.VK_A);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(3, 15, 3, 0);
        pnlButton.add(btnAdvanceDay, gridBagConstraints);

        btnBugReport.addActionListener(evt -> new EasyBugReportDialog(getFrame(), getCampaign()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new Insets(3, 15, 3, 15);
        pnlButton.add(btnBugReport, gridBagConstraints);

        return pnlButton;
    }
    // endregion Initialization

    public @Nullable CampaignGuiTab getTab(final MHQTabType tabType) {
        return standardTabs.get(tabType);
    }

    public @Nullable CommandCenterTab getCommandCenterTab() {
        return (CommandCenterTab) getTab(COMMAND_CENTER);
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
     *
     * @param tabType The type of tab to select.
     */
    public void setSelectedTab(MHQTabType tabType) {
        if (standardTabs.containsKey(tabType)) {
            final CampaignGuiTab tab = standardTabs.get(tabType);
            IntStream.range(0, tabMain.getTabCount())
                  .filter(ii -> Objects.equals(tabMain.getComponentAt(ii), tab))
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
            CampaignGuiTab campaignGuiTab = tab.createTab(this);
            standardTabs.put(tab, campaignGuiTab);
            int index = IntStream.range(0, tabMain.getTabCount())
                              .filter(i -> ((CampaignGuiTab) tabMain.getComponentAt(i)).tabType().ordinal() >
                                                 tab.ordinal())
                              .findFirst()
                              .orElse(tabMain.getTabCount());
            tabMain.insertTab(campaignGuiTab.getTabName(), null, campaignGuiTab, null, index);
            tabMain.setMnemonicAt(index, tab.getMnemonic());
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

    public boolean showRetirementDefectionDialog() {
        /*
         * if there are unresolved personnel, show the results view; otherwise,
         * present the retirement view to give the player a chance to follow a
         * custom schedule
         */
        boolean doRetirement = getCampaign().getRetirementDefectionTracker().getRetirees().isEmpty();
        RetirementDefectionDialog dialog = new RetirementDefectionDialog(this, null, doRetirement);

        if (!dialog.wasAborted()) {
            getCampaign().applyRetirement(dialog.totalPayout(), dialog.getUnitAssignments());
            return true;
        } else {
            return false;
        }
    }

    public void showAwardEligibilityDialog() {
        AutoAwardsController autoAwardsController = new AutoAwardsController();

        autoAwardsController.ManualController(getCampaign(), true);
    }

    private void changeTheme(ActionEvent evt) {
        MekHQ.getSelectedTheme().setValue(evt.getActionCommand());
        refreshThemeChoices();
    }

    private void refreshThemeChoices() {
        menuThemes.removeAll();
        JCheckBoxMenuItem miPlaf;
        for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            // intentionally only limits the themes in the menu; as a last resort for GUI problems, other laf can be
            // used by hand-editing mhq.preferences
            if (GUIPreferences.isSupportedLookAndFeel(laf)) {
                miPlaf = new JCheckBoxMenuItem(laf.getName());
                if (laf.getClassName().equalsIgnoreCase(MekHQ.getSelectedTheme().getValue())) {
                    miPlaf.setSelected(true);
                }

                menuThemes.add(miPlaf);
                miPlaf.setActionCommand(laf.getClassName());
                miPlaf.addActionListener(this::changeTheme);
            }
        }
    }

    public void focusOnUnit(UUID id) {
        HangarTab ht = (HangarTab) getTab(MHQTabType.HANGAR);
        if (null == id || null == ht) {
            return;
        }
        ht.focusOnUnit(id);
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap.getString("panHangar.TabConstraints.tabTitle")));
    }

    /**
     * Focuses the UI on a specific scenario by its ID.
     *
     * <p>This method first retrieves the Briefing Room tab. If the tab exists, it:</p>
     *
     * <ol>
     *   <li>Delegates to the {@link BriefingTab}'s {@link BriefingTab#focusOnScenario} method to select the
     *   specific scenario</li>
     *   <li>Switches the main tab view to display the Briefing Room tab</li>
     * </ol>
     *
     * <p>If the Briefing Room tab cannot be found, no action is taken.</p>
     *
     * @param targetId The unique identifier of the scenario to focus on
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void focusOnScenario(int targetId) {
        BriefingTab briefingTab = (BriefingTab) getTab(MHQTabType.BRIEFING_ROOM);
        if (briefingTab == null) {
            return;
        }
        briefingTab.focusOnScenario(targetId);
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap.getString("panBriefing.TabConstraints.tabTitle")));
    }

    /**
     * Focuses the UI on a specific mission by its ID.
     *
     * <p>This method first retrieves the {@link BriefingTab} tab. If the tab exists, it:</p>
     *
     * <ol>
     *   <li>Delegates to the {@link BriefingTab}'s {@link BriefingTab#focusOnMission} method to select the specific
     *   mission</li>
     *   <li>Switches the main tab view to display the Briefing Room tab</li>
     * </ol>
     *
     * <p>If the Briefing Room tab cannot be found, no action is taken.</p>
     *
     * @param targetId The unique identifier of the mission to focus on
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void focusOnMission(int targetId) {
        BriefingTab briefingTab = (BriefingTab) getTab(MHQTabType.BRIEFING_ROOM);
        if (briefingTab == null) {
            return;
        }
        briefingTab.focusOnMission(targetId);
        tabMain.setSelectedIndex(getTabIndexByName(resourceMap.getString("panBriefing.TabConstraints.tabTitle")));
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
            new NewsDialog(getCampaign(), news.getFullDescription());
        }
    }

    private void hirePerson(final ActionEvent evt) {
        final NewRecruitDialog npd = new NewRecruitDialog(this,
              true,
              getCampaign().newPerson(PersonnelRole.valueOf(evt.getActionCommand())));
        npd.setVisible(true);
    }

    /**
     * Opens the personnel market dialog to hire a person, using the appropriate market style based on campaign
     * options.
     *
     * <p>If the personnel market is disabled in the campaign options, a deprecated {@link PersonnelMarketDialog} is
     * displayed. Otherwise, the new personnel market dialog is shown according to the campaign's current market
     * style.</p>
     *
     * <p>If no personnel market is enabled display the bulk hiring dialog, instead.</p>
     */
    public void hirePersonMarket() {
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        PersonnelMarketStyle marketStyle = campaignOptions.getPersonnelMarketStyle();

        if (marketStyle != PERSONNEL_MARKET_DISABLED || !getCampaign().getPersonnelMarket().isNone()) {
            if (marketStyle == PERSONNEL_MARKET_DISABLED) {
                PersonnelMarketDialog personnelMarketDialog =
                      new PersonnelMarketDialog(getFrame(), this, getCampaign());
                personnelMarketDialog.setVisible(true);
            } else {
                getCampaign().getNewPersonnelMarket().showPersonnelMarketDialog();
            }
        } else {
            hireBulkPersonnel();
        }
    }

    private void hireBulkPersonnel() {
        HireBulkPersonnelDialog hireBulkPersonnelDialog = new HireBulkPersonnelDialog(getFrame(), true, getCampaign());
        hireBulkPersonnelDialog.setVisible(true);
    }

    public void showContractMarket() {
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();

        if (campaignOptions.getContractMarketMethod().isNone()) {
            MissionTypeDialog missionTypeDialog = getMissionTypeDialog(campaignOptions);

            if (missionTypeDialog.isMission()) {
                CustomizeMissionDialog customizeMissionDialog =
                      new CustomizeMissionDialog(getFrame(), true, null, getCampaign());
                customizeMissionDialog.setVisible(true);
            }
        } else {
            ContractMarketDialog contractMarketDialog = new ContractMarketDialog(getFrame(), getCampaign());
            contractMarketDialog.setVisible(true);
        }
    }

    private MissionTypeDialog getMissionTypeDialog(CampaignOptions campaignOptions) {
        MissionTypeDialog missionTypeDialog = new MissionTypeDialog(getFrame(), true);
        missionTypeDialog.setVisible(true);

        if (missionTypeDialog.isContract()) {
            NewContractDialog newContractDialog = campaignOptions.isUseAtB() ?
                                                        new NewAtBContractDialog(getFrame(), true, getCampaign()) :
                                                        new NewContractDialog(getFrame(), true, getCampaign());
            newContractDialog.setVisible(true);
        }
        return missionTypeDialog;
    }

    public void showUnitMarket() {
        if (!getCampaign().getUnitMarket().getMethod().isNone()) {
            new UnitMarketDialog(getFrame(), getCampaign()).showDialog();
        } else {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(getFrame());
            if (!MekSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog mekHQUnitSelectorDialog = new MekHQUnitSelectorDialog(getFrame(),
                  unitLoadingDialog, getCampaign(), true);
            mekHQUnitSelectorDialog.setVisible(true);
        }
    }

    public void showPartsMarket() {
        PartsStoreDialog store = new PartsStoreDialog(true, this);
        store.setVisible(true);
    }

    public boolean saveCampaign(ActionEvent evt) {
        logger.info("Saving campaign...");
        // Choose a file...
        File file = FileDialogs.saveCampaign(frame, getCampaign()).orElse(null);
        if (file == null) {
            // I want a file, you know!
            return false;
        }

        return saveCampaign(getFrame(), getCampaign(), file, false);
    }

    public static boolean saveCampaign(JFrame frame, Campaign campaign, File file) {
        return saveCampaign(frame, campaign, file, false);
    }

    /**
     * Attempts to save the given campaign to the given file.
     *
     * @param frame The parent frame in which to display the error message. May be null.
     */
    public static boolean saveCampaign(JFrame frame, Campaign campaign, File file, boolean isForBugReport) {
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
            campaign.writeToXML(pw, isForBugReport);
            pw.flush();
            // delete the backup file because we didn't need it
            if (backupFile.exists() && !backupFile.delete()) {
                logger.error(
                      "Backup file deletion failure. This means that the backup file of {} will be retained instead of being properly deleted.",
                      backupFile.getPath());
            }
            logger.info("Campaign saved to {}", file);
        } catch (Exception ex) {
            logger.error("", ex);
            JOptionPane.showMessageDialog(frame, """
                  Oh no! The program was unable to correctly save your game. We know this
                  is annoying and apologize. Please help us out and submit a bug with the
                  mekhq.log file from this game so we can prevent this from happening in
                  the future.""", "Could not save game", JOptionPane.ERROR_MESSAGE);

            // restore the backup file
            if (file.delete()) {
                if (backupFile.exists()) {
                    Utilities.copyfile(backupFile, file);
                    if (!backupFile.delete()) {
                        logger.error(
                              "Backup file deletion failure after restoring the original file. This means that the " +
                                    "backup file of {} will be retained instead of being properly deleted.",
                              backupFile.getPath());
                    }
                }
            } else {
                logger.error("File deletion failure. This means that the file at {} will be retained instead of being " +
                                   "properly deleted, with any backup at {} not being restored nor deleted.",
                      file.getPath(),
                      backupFile.getPath());
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
        boolean factionIntroDate = oldOptions.isFactionIntroDate();
        final RandomDivorceMethod randomDivorceMethod = oldOptions.getRandomDivorceMethod();
        final RandomMarriageMethod randomMarriageMethod = oldOptions.getRandomMarriageMethod();
        final RandomProcreationMethod randomProcreationMethod = oldOptions.getRandomProcreationMethod();

        CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(getFrame(), getCampaign());
        optionsDialog.setVisible(true);

        final CampaignOptions newOptions = getCampaign().getCampaignOptions();

        if (randomDivorceMethod != newOptions.getRandomDivorceMethod()) {
            getCampaign().setDivorce(newOptions.getRandomDivorceMethod().getMethod(newOptions));
        } else {
            getCampaign().getDivorce().setUseClanPersonnelDivorce(newOptions.isUseClanPersonnelDivorce());
            getCampaign().getDivorce().setUsePrisonerDivorce(newOptions.isUsePrisonerDivorce());
            getCampaign().getDivorce().setUseRandomOppositeSexDivorce(newOptions.isUseRandomOppositeSexDivorce());
            getCampaign().getDivorce().setUseRandomSameSexDivorce(newOptions.isUseRandomSameSexDivorce());
            getCampaign().getDivorce().setUseRandomClanPersonnelDivorce(newOptions.isUseRandomClanPersonnelDivorce());
            getCampaign().getDivorce().setUseRandomPrisonerDivorce(newOptions.isUseRandomPrisonerDivorce());
            if (getCampaign().getDivorce().getMethod().isDiceRoll()) {
                ((RandomDivorce) getCampaign().getDivorce()).setDivorceDiceSize(newOptions.getRandomDivorceDiceSize());
            }
        }

        if (randomMarriageMethod != newOptions.getRandomMarriageMethod()) {
            getCampaign().setMarriage(newOptions.getRandomMarriageMethod().getMethod(newOptions));
        } else {
            getCampaign().getMarriage().setUseClanPersonnelMarriages(newOptions.isUseClanPersonnelMarriages());
            getCampaign().getMarriage().setUsePrisonerMarriages(newOptions.isUsePrisonerMarriages());
            getCampaign().getMarriage()
                  .setUseRandomClanPersonnelMarriages(newOptions.isUseRandomClanPersonnelMarriages());
            getCampaign().getMarriage().setUseRandomPrisonerMarriages(newOptions.isUseRandomPrisonerMarriages());
            if (getCampaign().getMarriage().getMethod().isDiceRoll()) {
                ((RandomMarriage) getCampaign().getMarriage()).setMarriageDiceSize(newOptions.getRandomMarriageDiceSize());
            }
        }

        if (randomProcreationMethod != newOptions.getRandomProcreationMethod()) {
            getCampaign().setProcreation(newOptions.getRandomProcreationMethod().getMethod(newOptions));
        } else {
            getCampaign().getProcreation().setUseClanPersonnelProcreation(newOptions.isUseClanPersonnelProcreation());
            getCampaign().getProcreation().setUsePrisonerProcreation(newOptions.isUsePrisonerProcreation());
            getCampaign().getProcreation()
                  .setUseRelationshiplessProcreation(newOptions.isUseRelationshiplessRandomProcreation());
            getCampaign().getProcreation()
                  .setUseRandomClanPersonnelProcreation(newOptions.isUseRandomClanPersonnelProcreation());
            getCampaign().getProcreation().setUseRandomPrisonerProcreation(newOptions.isUseRandomPrisonerProcreation());
            if (getCampaign().getProcreation().getMethod().isDiceRoll()) {
                ((RandomProcreation) getCampaign().getProcreation()).setRelationshipDieSize(newOptions.getRandomProcreationRelationshipDiceSize());
                ((RandomProcreation) getCampaign().getProcreation()).setRelationshiplessDieSize(newOptions.getRandomProcreationRelationshiplessDiceSize());
            }
        }

        // Clear Procreation Data if Disabled
        if (!newOptions.isUseManualProcreation() && newOptions.getRandomProcreationMethod().isNone()) {
            getCampaign().getPersonnel()
                  .parallelStream()
                  .filter(Person::isPregnant)
                  .forEach(person -> getCampaign().getProcreation().removePregnancy(person));
        }

        final AbstractUnitMarket unitMarket = getCampaign().getUnitMarket();
        if (unitMarket.getMethod() != newOptions.getUnitMarketMethod()) {
            getCampaign().setUnitMarket(newOptions.getUnitMarketMethod().getUnitMarket());
            getCampaign().getUnitMarket().setOffers(unitMarket.getOffers());
        }

        AbstractContractMarket contractMarket = getCampaign().getContractMarket();
        if (contractMarket.getMethod() != newOptions.getContractMarketMethod()) {
            getCampaign().setContractMarket(newOptions.getContractMarketMethod().getContractMarket());
        }

        if (atb != newOptions.isUseAtB()) {
            if (newOptions.isUseAtB()) {
                getCampaign().initAtB(false);
                // refresh lance assignment table
                MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCampaign().getForces()));
            }
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

        getCampaign().initTurnover();

        if (factionIntroDate != newOptions.isFactionIntroDate()) {
            getCampaign().updateTechFactionCode();
        }
        refreshCalendar();
        getCampaign().reloadNews();
    }

    public void refitUnit(Refit r, boolean selectModelName) {
        if (r.getOriginalEntity() instanceof Infantry && !(r.getOriginalEntity() instanceof BattleArmor)) {
            r.setTech(null);
        } else if (r.getOriginalEntity() instanceof Dropship || r.getOriginalEntity() instanceof Jumpship) {
            Person engineer = r.getOriginalUnit().getEngineer();
            if (engineer == null) {
                JOptionPane.showMessageDialog(frame,
                      "You cannot refit a ship that does not have an engineer. Assign a qualified vessel crew to this unit.",
                      "No Engineer",
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
            r.setTech(engineer);
        } else if (getCampaign().getActivePersonnel(false, false).stream().anyMatch(Person::isTech)) {
            String name;
            Map<String, Person> techHash = new HashMap<>();
            List<String> techList = new ArrayList<>();

            List<Person> techs = getCampaign().getTechs(false, true);
            int lastRightTech = 0;

            for (Person tech : techs) {
                if (getCampaign().isWorkingOnRefit(tech) || tech.isEngineer()) {
                    continue;
                }

                name = "<html>" +
                             tech.getFullName() +
                             ", <b>" +
                             SkillType.getColoredExperienceLevelName(tech.getSkillLevel(getCampaign(), false, true)) +
                             "</b> " +
                             tech.getPrimaryRoleDesc() +
                             " (" +
                             getCampaign().getTargetFor(r, tech).getValueAsString() +
                             "+), " +
                             tech.getMinutesLeft() +
                             '/' +
                             tech.getDailyAvailableTechTime(getCampaign().getCampaignOptions()
                                                                  .isTechsUseAdministration()) +
                             " minutes</html>";
                techHash.put(name, tech);
                if (tech.isRightTechTypeFor(r)) {
                    techList.add(lastRightTech++, name);
                } else {
                    techList.add(name);
                }
            }

            String s = (techList.isEmpty()) ?
                             null :
                             (String) JOptionPane.showInputDialog(frame,
                                   "Which tech should work on the refit?",
                                   "Select Tech",
                                   JOptionPane.PLAIN_MESSAGE,
                                   null,
                                   techList.toArray(),
                                   techList.get(0));

            if (null == s) {
                return;
            }

            Person selectedTech = techHash.get(s);

            if (!selectedTech.isRightTechTypeFor(r)) {
                if (JOptionPane.NO_OPTION ==
                          JOptionPane.showConfirmDialog(null,
                                "This tech is not appropriate for this unit. Would you like to continue?",
                                "Incorrect Tech Type",
                                JOptionPane.YES_NO_OPTION)) {
                    return;
                }
            }

            r.setTech(selectedTech);
        } else {
            JOptionPane.showMessageDialog(frame,
                  "You have no techs available to work on this refit.",
                  "No Techs",
                  JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectModelName) {
            // select a model name
            RefitNameDialog rnd = new RefitNameDialog(frame, true, r);
            rnd.setVisible(true);
            if (rnd.wasCancelled()) {
                // Set the tech team to null since we may want to change it when we re-do the
                // refit
                r.setTech(null);
                return;
            }
        }
        // TODO: allow overtime work?
        // check to see if user really wants to do it - give some info on what
        // will be done
        // TODO: better information
        String RefitRefurbish = getRefitRefurbish(r);
        if (0 !=
                  JOptionPane.showConfirmDialog(null,
                        RefitRefurbish + r.getUnit().getName() + '?',
                        "Proceed?",
                        JOptionPane.YES_NO_OPTION)) {
            return;
        }
        try {
            r.begin();
        } catch (EntityLoadingException ex) {
            JOptionPane.showMessageDialog(null,
                  "For some reason, the unit you are trying to customize cannot be loaded\n and so the customization was cancelled. Please report the bug with a description\nof the unit being customized.",
                  "Could not customize unit",
                  JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "IO Exception", JOptionPane.ERROR_MESSAGE);
            return;
        }
        getCampaign().refit(r);
        if (hasTab(MHQTabType.MEK_LAB)) {
            ((MekLabTab) getTab(MHQTabType.MEK_LAB)).clearUnit();
        }
    }

    private static String getRefitRefurbish(Refit r) {
        String RefitRefurbish;
        if (r.isBeingRefurbished()) {
            RefitRefurbish = "Refurbishment is a " +
                                   r.getRefitClassName() +
                                   " refit and must be done at a factory and costs 10% of the purchase price" +
                                   ".\n Are you sure you want to refurbish ";
        } else {
            RefitRefurbish = "This is a " + r.getRefitClassName() + " refit. Are you sure you want to refit ";
        }
        return RefitRefurbish;
    }

    /**
     * Shows a dialog that lets the user select a tech for a task on a particular unit
     *
     * @param unit              The unit to be serviced, used to filter techs for skill on the unit.
     * @param desc              The description of the task
     * @param ignoreMaintenance If true, ignores the time required for maintenance tasks when displaying the tech's time
     *                          available.
     *
     * @return The ID of the selected tech, or null if none is selected.
     */
    public @Nullable UUID selectTech(Unit unit, String desc, boolean ignoreMaintenance) {
        String name;
        Map<String, Person> techHash = new LinkedHashMap<>();
        for (Person tech : getCampaign().getTechsExpanded()) {
            if (tech.isTechLargeVessel()) {
                Entity entity = unit.getEntity();
                if (entity == null) {
                    logger.error("(selectTech) Unit {} has no entity", unit);
                    continue;
                }

                if (unit.getEntity().isLargeCraft()) {
                    Unit techUnit = tech.getUnit();
                    // This stops vessel crew from other vessels appearing in the list
                    if (techUnit != null && !techUnit.equals(unit)) {
                        continue;
                    }
                }
            }

            if (!tech.isMothballing() && tech.canTech(unit.getEntity())) {
                int time = tech.getMinutesLeft();
                if (!ignoreMaintenance) {
                    time -= Math.max(0, tech.getMaintenanceTimeUsing());
                }
                SkillModifierData skillModifierData = tech.getSkillModifierData(true);
                name = tech.getFullTitle() +
                             ", " +
                             getExperienceLevelName(tech.getSkillForWorkingOn(unit)
                                                          .getExperienceLevel(skillModifierData)) +
                             " (" +
                             time +
                             "min)";
                techHash.put(name, tech);
            }
        }
        if (techHash.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                  "You have no techs available.",
                  "No Techs",
                  JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Object[] nameArray = techHash.keySet().toArray();

        String s = (String) JOptionPane.showInputDialog(frame,
              "Which tech should work on " + desc + '?',
              "Select Tech",
              JOptionPane.PLAIN_MESSAGE,
              null,
              nameArray,
              nameArray[0]);
        if (null == s) {
            return null;
        }
        return techHash.get(s).getId();
    }

    /**
     * Exports Planets to a file (CSV, XML, etc.)
     *
     */
    protected void exportPlanets(FileType format, String dialogTitle, String filename) {
        // TODO: Fix this
        /*
         * GUI.fileDialogSave(
         * frame,
         * dialogTitle,
         * format,
         * MekHQ.getPlanetsDirectory().getValue(),
         * "planets." + format.getRecommendedExtension())
         * .ifPresent(f -> {
         * MekHQ.getPlanetsDirectory().setValue(f.getParent());
         * File file = checkFileEnding(f, format.getRecommendedExtension());
         * checkToBackupFile(file, file.getPath());
         * String report = Planets.getInstance().exportPlanets(file.getPath(),
         * format.getRecommendedExtension());
         * JOptionPane.showMessageDialog(mainPanel, report);
         * });
         *
         * GUI.fileDialogSave(frame, dialogTitle, new File(".", "planets." +
         * format.getRecommendedExtension()), format).ifPresent(f -> {
         * File file = checkFileEnding(f, format.getRecommendedExtension());
         * checkToBackupFile(file, file.getPath());
         * String report = Planets.getInstance().exportPlanets(file.getPath(),
         * format.getRecommendedExtension());
         * JOptionPane.showMessageDialog(mainPanel, report);
         * });
         */
    }

    /**
     * Exports Personnel to a file (CSV, XML, etc.)
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportPersonnel(FileType format, String dialogTitle, String filename) {
        if (((PersonnelTab) getTab(MHQTabType.PERSONNEL)).getPersonnelTable().getRowCount() != 0) {
            GUI.fileDialogSave(frame,
                  dialogTitle,
                  format,
                  MekHQ.getPersonnelDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
                MekHQ.getPersonnelDirectory().setValue(f.getParent());
                File file = checkFileEnding(f, format.getRecommendedExtension());
                checkToBackupFile(file, file.getPath());
                String report;
                // TODO add support for xml and json export
                if (format.equals(FileType.CSV)) {
                    report = Utilities.exportTableToCSV(((PersonnelTab) getTab(MHQTabType.PERSONNEL)).getPersonnelTable(),
                          file);
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
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportUnits(FileType format, String dialogTitle, String filename) {
        if (((HangarTab) getTab(MHQTabType.HANGAR)).getUnitTable().getRowCount() != 0) {
            GUI.fileDialogSave(frame,
                  dialogTitle,
                  format,
                  MekHQ.getUnitsDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
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
     *
     * @param format      file format to export to
     * @param dialogTitle title of the dialog frame
     * @param filename    file name to save to
     */
    protected void exportFinances(FileType format, String dialogTitle, String filename) {
        if (!getCampaign().getFinances().getTransactions().isEmpty()) {
            GUI.fileDialogSave(frame,
                  dialogTitle,
                  format,
                  MekHQ.getFinancesDirectory().getValue(),
                  filename + '.' + format.getRecommendedExtension()).ifPresent(f -> {
                MekHQ.getFinancesDirectory().setValue(f.getParent());
                File file = checkFileEnding(f, format.getRecommendedExtension());
                checkToBackupFile(file, file.getPath());
                String report;
                // TODO add support for xml and json export
                if (format.equals(FileType.CSV)) {
                    report = getCampaign().getFinances()
                                   .exportFinancesToCSV(file.getPath(), format.getRecommendedExtension());
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
     *
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
     *
     * @param file   the file to check
     * @param format proper format for the ending/extension
     *
     * @return File with the appropriate ending/ extension
     */
    private File checkFileEnding(File file, String format) {
        String path = file.getPath();
        if (!path.endsWith('.' + format)) {
            path += '.' + format;
            file = new File(path);
        }
        return file;
    }

    protected void loadListFile(final boolean allowNewPilots) {
        final File unitFile = FileDialogs.openUnits(getFrame()).orElse(null);

        PartQuality quality = PartQuality.QUALITY_D;

        if (getCampaign().getCampaignOptions().isUseRandomUnitQualities()) {
            quality = Unit.getRandomUnitQuality(0);
        }

        if (unitFile != null) {
            try {
                for (Entity entity : new MULParser(unitFile, getCampaign().getGameOptions()).getEntities()) {
                    getCampaign().addNewUnit(entity, allowNewPilots, 0, quality);
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    protected void loadPersonFile() {
        File personnelFile = FileDialogs.openPersonnel(frame).orElse(null);

        if (personnelFile != null) {
            logger.info("Starting load of personnel file from XML...");
            // Initialize variables.
            Document xmlDoc;

            // Open the file
            try (InputStream is = new FileInputStream(personnelFile)) {
                // Using factory get an instance of document builder
                DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

                // Parse using builder to get DOM representation of the XML file
                xmlDoc = db.parse(is);
            } catch (Exception ex) {
                logger.error("Cannot load person XML", ex);
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
                    logger.error("Unknown node type not loaded in Personnel nodes: {}", wn2.getNodeName());
                    continue;
                }

                Person person = Person.generateInstanceFromXML(wn2, getCampaign(), version);
                if ((person != null) && (getCampaign().getPerson(person.getId()) != null)) {
                    logger.error("ERROR: Cannot load person who exists, ignoring. (Name: {}, Id {})",
                          person.getFullName(),
                          person.getId());
                    person = null;
                }

                if (person != null) {
                    getCampaign().recruitPerson(person, person.getPrisonerStatus(), true, false, person.isEmployed(),
                          true);

                    // Clear some values we no longer should have set in case this
                    // has transferred campaigns or things in the campaign have
                    // changed...
                    person.setUnit(null);
                    person.clearTechUnits();
                }
            }

            // Fix Spouse Id Information - This is required to fix spouse NPEs where one doesn't export both members
            // of the couple
            // TODO : make it so that exports will automatically include both spouses
            for (Person p : getCampaign().getActivePersonnel(true, true)) {
                if (p.getGenealogy().hasSpouse() &&
                          !getCampaign().getPersonnel().contains(p.getGenealogy().getSpouse())) {
                    // If this happens, we need to clear the spouse
                    if (p.getMaidenName() != null) {
                        p.setSurname(p.getMaidenName());
                    }

                    p.getGenealogy().setSpouse(null);
                }

                if (p.isPregnant()) {
                    String fatherIdString = p.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
                    UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
                    if ((fatherId != null) &&
                              !getCampaign().getPersonnel().contains(getCampaign().getPerson(fatherId))) {
                        p.getExtraData().set(AbstractProcreation.PREGNANCY_FATHER_DATA, null);
                    }
                }
            }

            logger.info("Finished load of personnel file");
        }
    }

    public void savePersonFile() {
        File file = FileDialogs.savePersonnel(frame, getCampaign()).orElse(null);
        if (file == null) {
            // I want a file, you know!
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
            logger.error("Cannot export person if there's not a personnel tab");
            return;
        }
        int row = pt.getPersonnelTable().getSelectedRow();
        if (row < 0) {
            logger.warn("Cannot export person if no one is selected! Ignoring.");
            return;
        }
        Person selectedPerson = pt.getPersonModel().getPerson(pt.getPersonnelTable().convertRowIndexToModel(row));
        int[] rows = pt.getPersonnelTable().getSelectedRows();
        Person[] people = Arrays.stream(rows)
                                .mapToObj(j -> pt.getPersonModel()
                                                     .getPerson(pt.getPersonnelTable().convertRowIndexToModel(j)))
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
            logger.info("Personnel saved to {}", file);
        } catch (Exception ex) {
            logger.error("", ex);
            JOptionPane.showMessageDialog(getFrame(), """
                  Oh no! The program was unable to correctly export your personnel. We know this
                  is annoying and apologize. Please help us out and submit a bug with the
                  mekhq.log file from this game so we can prevent this from happening in
                  the future.""", "Could not export personnel", JOptionPane.ERROR_MESSAGE);
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

        logger.info("Starting load of parts file from XML...");
        // Initialize variables.
        Document xmlDoc;

        // Open up the file.
        try (InputStream is = new FileInputStream(partsFile)) {
            // Using factory get an instance of document builder
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            // Parse using builder to get DOM representation of the XML file
            xmlDoc = db.parse(is);
        } catch (Exception ex) {
            logger.error("", ex);
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
                // Err, what should we do here?
                logger.error("Unknown node type not loaded in Parts nodes: {}", wn2.getNodeName());
                continue;
            }

            Part p = Part.generateInstanceFromXML(wn2, version);
            if (p != null) {
                parts.add(p);
            }
        }

        getCampaign().importParts(parts);
        logger.info("Finished load of parts file");
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
            logger.error("Cannot export parts for a null warehouse tab");
            return;
        }

        JTable partsTable = warehouseTab.getPartsTable();
        PartsTableModel partsModel = warehouseTab.getPartsModel();
        int row = partsTable.getSelectedRow();
        if (row < 0) {
            logger.warn("Cannot export parts if none are selected! Ignoring.");
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
            logger.info("Parts saved to {}", file);
        } catch (Exception ex) {
            logger.error("", ex);
            JOptionPane.showMessageDialog(getFrame(), """
                  Oh no! The program was unable to correctly export your parts. We know this
                  is annoying and apologize. Please help us out and submit a bug with the
                  mekhq.log file from this game so we can prevent this from happening in
                  the future.""", "Could not export parts", JOptionPane.ERROR_MESSAGE);
            // restore the backup file
            file.delete();
            if (backupFile.exists()) {
                Utilities.copyfile(backupFile, file);
                backupFile.delete();
            }
        }
    }

    /**
     * Checks if the user should be prompted (nagged) to view the daily log and highlights the Command Center tab if
     * needed.
     *
     * <p>If the {@code logNagActive} flag is already set, the method returns immediately to prevent repeat
     * processing. If the currently selected tab is the Command Center, no nag is performed. Otherwise, the method
     * iterates through the tab list and highlights the Command Center tab by changing its label color and sets the
     * {@code logNagActive} flag.</p>
     *
     * <p>If no tab is currently selected, a warning is logged and no action is taken.</p>
     *
     * @param logType the category of daily report the UI should prompt the user to review
     */
    public void checkDailyLogNag(DailyReportType logType) {
        // If we're already nagging, no need to nag again
        boolean subTabNagActive = getCommandCenterTab().isLogNagActive(logType);
        int relevantIndex = logType.getTabIndex();

        // We're already nagging
        if (logNagActive && subTabNagActive) {
            return;
        }

        final int selectedIndex = tabMain.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= tabMain.getTabCount()) {
            logger.warn("No tab selected, cannot check for daily log nag");
            return;
        }

        // If the player is already viewing the correct log tab, no nag needed
        final Component selectedTab = tabMain.getComponentAt(selectedIndex);
        if (selectedTab instanceof CommandCenterTab commandCenterTab) {
            int logsSelected = commandCenterTab.getTabLogs().getSelectedIndex();

            if (logsSelected == relevantIndex) {
                return;
            }
        }

        // Loop through the tabs until we find the Command Center tab, then color that tab's label.
        for (int i = 0; i < tabMain.getTabCount(); i++) {
            Component component = tabMain.getComponentAt(i);

            if (component instanceof CommandCenterTab commandCenterTab) {
                // If the player is currently on the Command Center Tab, no need to nag that tab, though we're still
                // going to nag the sub-tab
                if (!(selectedTab instanceof CommandCenterTab)) {
                    tabMain.setBackgroundAt(i, UIUtil.uiDarkBlue());
                    logNagActive = true;
                }

                EnhancedTabbedPane tabLogs = commandCenterTab.getTabLogs();
                int logsSelected = tabLogs.getSelectedIndex();
                if (logsSelected != relevantIndex) {
                    DailyReportLogPanel reportTab = switch (logType) {
                        case GENERAL -> commandCenterTab.getGeneralLog();
                        case BATTLE -> commandCenterTab.getBattleLog();
                        case PERSONNEL -> commandCenterTab.getPersonnelLog();
                        case MEDICAL -> commandCenterTab.getMedicalLog();
                        case FINANCES -> commandCenterTab.getFinancesLog();
                        case ACQUISITIONS -> commandCenterTab.getAcquisitionsLog();
                        case TECHNICAL -> commandCenterTab.getTechnicalLog();
                        case POLITICS -> commandCenterTab.getPoliticsLog();
                        case SKILL_CHECKS -> commandCenterTab.getSkillLog();
                    };

                    if (!DailyReportLogPanel.isDateOnly(List.of(reportTab.getLogText()))) {
                        commandCenterTab.nagLogTab(relevantIndex);
                        commandCenterTab.setLogNagActive(logType, true);
                    }
                }

                break;
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
            // put a try-catch here so that bugs in the mek lab don't screw up other stuff
            try {
                lab.refreshRefitSummary();
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    public void refreshCalendar() {
        getFrame().setTitle(getCampaign().getTitle());
    }

    /**
     * Refreshes the 'funds' display on the GUI.
     */
    private void refreshFunds() {
        Money funds = getCampaign().getFunds();
        String inDebt = "";
        if (getCampaign().getFinances().isInDebt()) {
            // FIXME : Localize
            inDebt = " <font color='" + ReportingUtilities.getNegativeColor() + "'>(in Debt)</font>";
        }
        // FIXME : Localize
        String text = "<html><b>Funds</b>: " + funds.toAmountAndSymbolString() + inDebt + "</html>";
        lblFunds.setText(text);
    }

    private void refreshTempAsTechs() {
        // FIXME : Localize
        String text = "<html><b>Temp AsTechs</b>: " + getCampaign().getTemporaryAsTechPool() + "</html>";
        lblTempAsTechs.setText(text);
    }

    private void refreshTempMedics() {
        // FIXME : Localize
        String text = "<html><b>Temp Medics</b>: " + getCampaign().getTemporaryMedicPool() + "</html>";
        lblTempMedics.setText(text);
    }

    private void refreshPartsAvailability() {
        if (!getCampaign().getCampaignOptions().isUseAtB() ||
                  getCampaign().getCampaignOptions().getAcquisitionType() == AcquisitionsType.ANY_TECH) {
            lblPartsAvailabilityRating.setText("");
        } else {
            int partsAvailability = getCampaign().findAtBPartsAvailabilityLevel();
            // FIXME : Localize
            lblPartsAvailabilityRating.setText(String.format("<html><b>Parts Availability Modifier</b>: %d</html>",
                  partsAvailability));
        }
    }

    private final ActionScheduler fundsScheduler = new ActionScheduler(this::refreshFunds);

    public void refreshLocation() {
        boolean isUseCommandCircuit =
              FactionStandingUtilities.isUseCommandCircuit(getCampaign().isOverridingCommandCircuitRequirements(),
                    getCampaign().isGM(), getCampaign().getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
                    getCampaign().getFactionStandings(), getCampaign().getFutureAtBContracts());

        lblLocation.setText(getCampaign().getLocation()
                                  .getReport(getCampaign().getLocalDate(),
                                        isUseCommandCircuit,
                                        getCampaign().getTransportCostCalculation(EXP_REGULAR)));
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
                if (parent.getScenarioId() == NO_ASSIGNED_SCENARIO) {
                    break;
                }
                parent.clearScenarioIds(getCampaign(), false);
                scenario.removeForce(parent.getId());
                for (Force sub : parent.getSubForces()) {
                    if (sub.getId() == prevId) {
                        continue;
                    }
                    scenario.addForces(sub.getId());
                    sub.setScenarioId(scenario.getId(), getCampaign());
                }
                prevId = parent.getId();
            }
        }

        if (null != scenario) {
            MekHQ.triggerEvent(new DeploymentChangedEvent(f, scenario));
        }
    }

    // region Subscriptions

    /**
     * Handles the {@link DayEndingEvent} that is published immediately before a day ends in the campaign.
     *
     * <p>This method is subscribed to day-ending events and implements logic that can block or allow the end of the
     * day, depending on the current campaign state and conditions. If certain criteria are met (such as outstanding
     * loans, faction issues, overdue scenarios, or random retirement prompts), the event will be cancelled—preventing
     * day transition.</p>
     *
     * <ul>
     *   <li>Checks if daily nag dialogs should be shown and blocks day end if needed.</li>
     *   <li>Blocks new day progression for overdue loans, invalid faction status, or due scenarios.</li>
     *   <li>Handles the random retirement option, prompting the user and conditionally blocking end-of-day if required.</li>
     * </ul>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is wrong.</p>
     *
     * @param dayEndingEvent the event signaling the end of the day; may be canceled by this handler to halt day
     *                       progression
     */
    @Subscribe
    public void handleDayEnding(DayEndingEvent dayEndingEvent) {
        if (MekHQ.getMHQOptions().getNewDayAutomaticallyAssignUnmaintainedUnits()) {
            AutomatedTechAssignments.handleTheAutomaticAssignmentOfUnmaintainedUnits(getCampaign());
        }

        if (triggerDailyNags(getCampaign())) {
            dayEndingEvent.cancel();
            return;
        }

        // Compulsory New Day Blockers
        if (checkForOverdueLoans(dayEndingEvent)) {
            return;
        }

        if (checkForInvalidFaction(dayEndingEvent)) {
            return;
        }

        if (checkForDueScenarios(dayEndingEvent)) {
            return;
        }

        // Optional New Day Blocker
        if (getCampaign().getCampaignOptions().isUseRandomRetirement()) {
            int turnoverPrompt = getCampaign().checkTurnoverPrompt();

            switch (turnoverPrompt) {
                case -1:
                    // the user wasn't presented with the dialog
                    break;
                case 0:
                    // the user launched the turnover dialog
                    if (!showRetirementDefectionDialog()) {
                        dayEndingEvent.cancel();
                        return;
                    }
                case 1:
                    // the user picked 'Advance Day Regardless'
                    break;
                case 2:
                    // the user canceled
                    dayEndingEvent.cancel();
                    return;
                default:
                    throw new IllegalStateException("Unexpected value in mekhq/gui/CampaignGUI.java/handleDayEnding: " +
                                                          turnoverPrompt);
            }
        }
    }

    /**
     * Handles changes to the campaign's current location.
     *
     * <p>Invokes an update to ensure the location information is current within the user interface and data model.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param locationChangedEvent the event indicating that the campaign location has changed
     */
    @Subscribe
    public void handleLocationChanged(LocationChangedEvent locationChangedEvent) {
        refreshLocation();
    }

    /**
     * Handles updates when a mission event occurs.
     *
     * <p>Refreshes the availability of parts to ensure inventory and options reflect the latest mission context.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param missionEvent the event signaling a mission change
     */
    @Subscribe
    public void handleMissionChanged(MissionEvent missionEvent) {
        refreshPartsAvailability();
    }

    /**
     * Handles updates to personnel records.
     *
     * <p>If a logistics administrator has been updated, recalculates AtB parts availability, ensuring that changes
     * in roles are properly reflected in inventory calculations.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param personEvent the event containing updates related to a person in the campaign
     */
    @Subscribe
    public void handlePersonUpdate(PersonEvent personEvent) {
        // only bother recalculating AtB parts availability if a logistics admin has been changed
        // refreshPartsAvailability cuts out early with a "use AtB" check so it's not necessary here
        if (personEvent.getPerson().hasRole(PersonnelRole.ADMINISTRATOR_LOGISTICS)) {
            refreshPartsAvailability();
        }
    }

    /**
     * Checks if there are any due instances of the {@link Scenario} class. If the {@code checkScenariosDue()} method of
     * the {@link Campaign} associated with the given {@link DayEndingEvent} returns {@code true}, a dialog shows up
     * informing the user of the due scenarios, and the {@link DayEndingEvent} is canceled.
     *
     * @param dayEndingEvent the {@link DayEndingEvent} being checked.
     *
     * @return {@code true} if there are due scenarios and {@code false} otherwise.
     */
    private boolean checkForDueScenarios(DayEndingEvent dayEndingEvent) {
        if (getCampaign().checkScenariosDue()) {
            JOptionPane.showMessageDialog(null,
                  getResourceMap().getString("dialogCheckDueScenarios.text"),
                  getResourceMap().getString("dialogCheckDueScenarios.title"),
                  JOptionPane.WARNING_MESSAGE);

            dayEndingEvent.cancel();

            return true;
        }
        return false;
    }

    /**
     * Checks for overdue loan payments in the campaign and handles them by displaying a warning dialog and canceling
     * the current event if overdue payments are found.
     *
     * <p>This method queries the campaign’s finances to determine whether there are any overdue loan payments.
     * If an overdue amount is detected, it refreshes the campaign's funds, displays an immersive dialog containing both
     * in-character and out-of-character messages, and cancels the current {@link DayEndingEvent}. The method then
     * returns {@code true} to indicate that overdue payments were found and processed.</p>
     *
     * @param dayEndingEvent The {@link DayEndingEvent} representing the end-of-day event. This event will be canceled
     *                       if overdue payments are detected.
     *
     * @return {@code true} if overdue loan payments were detected and the event was canceled; {@code false} otherwise,
     *       indicating no overdue loans.
     */
    private boolean checkForOverdueLoans(DayEndingEvent dayEndingEvent) {
        Campaign campaign = getCampaign();
        Money overdueAmount = campaign.getFinances().checkOverdueLoanPayments(campaign);
        if (overdueAmount.isPositive()) {
            refreshFunds();

            String inCharacterMessage = getFormattedTextAt(resourceMap.getBaseBundleName(),
                  "dialogOverdueLoans.ic",
                  campaign.getCommanderAddress());
            String outOfCharacterMessage = getFormattedTextAt(resourceMap.getBaseBundleName(),
                  "dialogOverdueLoans.ooc");

            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(LOGISTICS),
                  null,
                  inCharacterMessage,
                  null,
                  outOfCharacterMessage,
                  null,
                  false);

            dayEndingEvent.cancel();

            return true;
        }
        return false;
    }

    /**
     * Checks whether the player’s current faction in the campaign is invalid for the current date, and if so, displays
     * a warning dialog and cancels the current event.
     *
     * <p>This method retrieves the campaign's current date and faction, then verifies if the faction
     * is valid using the {@link Faction#validIn(LocalDate)} method. If the faction is invalid, both an in-character and
     * an out-of-character message are displayed using an {@link ImmersiveDialogSimple} dialog. The event is
     * subsequently canceled, and the method returns {@code true} to indicate that an invalid faction was detected.</p>
     *
     * @param dayEndingEvent The {@link DayEndingEvent} instance that represents the end-of-day event. This event will
     *                       be canceled if an invalid faction is found.
     *
     * @return {@code true} if the faction was found to be invalid and the event was canceled; {@code false} otherwise.
     */
    private boolean checkForInvalidFaction(DayEndingEvent dayEndingEvent) {
        Campaign campaign = getCampaign();
        Faction campaignFaction = campaign.getFaction();
        LocalDate currentDate = campaign.getLocalDate();

        if (!campaignFaction.validIn(currentDate)) {
            String inCharacterMessage = getFormattedTextAt(resourceMap.getBaseBundleName(),
                  "dialogInvalidFaction.ic",
                  campaign.getCommanderAddress());
            String outOfCharacterMessage = getFormattedTextAt(resourceMap.getBaseBundleName(),
                  "dialogInvalidFaction.ooc");

            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(COMMAND),
                  null,
                  inCharacterMessage,
                  null,
                  outOfCharacterMessage,
                  null,
                  false);

            dayEndingEvent.cancel();

            return true;
        }

        return false;
    }

    /**
     * Handles the transition to a new day in the campaign.
     *
     * <p>Refreshes the calendar, location, funds, parts availability, and all relevant UI tabs to ensure the user
     * interface and data are up to date at the beginning of a new day.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param newDayEvent the event signalling that a new day has started
     */
    @Subscribe
    public void handleNewDay(NewDayEvent newDayEvent) {
        refreshCalendar();
        refreshLocation();
        refreshFunds();
        refreshPartsAvailability();
        refreshMarketButtonLabels();

        refreshAllTabs();
    }

    /**
     * Processes changes in campaign options.
     *
     * <p>Updates the visibility and availability of UI tabs and menu items based on the new campaign settings.
     * Also triggers a refresh of all tabs and schedules updates for funds and parts availability.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param optionsChangedEvent the event containing the updated options
     */
    @Subscribe
    public void handle(final OptionsChangedEvent optionsChangedEvent) {
        if (!getCampaign().getCampaignOptions().isUseStratCon() && (getTab(MHQTabType.STRAT_CON) != null)) {
            removeStandardTab(MHQTabType.STRAT_CON);
        } else if (getCampaign().getCampaignOptions().isUseStratCon() && (getTab(MHQTabType.STRAT_CON) == null)) {
            addStandardTab(MHQTabType.STRAT_CON);
        }

        refreshAllTabs();
        fundsScheduler.schedule();
        refreshPartsAvailability();

        miRetirementDefectionDialog.setVisible(optionsChangedEvent.getOptions().isUseRandomRetirement());
        miAwardEligibilityDialog.setVisible((optionsChangedEvent.getOptions().isEnableAutoAwards()));

        boolean isMaplessMode = getCampaign().getCampaignOptions().isUseStratConMaplessMode();
        int stratConTabIndex = tabMain.indexOfTab(MHQTabType.STRAT_CON.toString());

        if (stratConTabIndex != -1) {
            tabMain.setEnabledAt(stratConTabIndex, !isMaplessMode);
        }
    }

    /**
     * Handles updates to campaign state following a transaction event.
     *
     * <p>Schedules an update to the funds and refreshes parts availability to reflect the new state after
     * a transaction has occurred.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param transactionEvent the event signaling the completion of a transaction
     */
    @Subscribe
    public void handle(TransactionEvent transactionEvent) {
        fundsScheduler.schedule();
        refreshPartsAvailability();
    }

    /**
     * Handles changes in campaign funds due to a loan event.
     *
     * <p>Schedules a funds update and refreshes parts availability after a loan transaction is processed.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param loanEvent the event representing a loan-related action
     */
    @Subscribe
    public void handle(LoanEvent loanEvent) {
        fundsScheduler.schedule();
        refreshPartsAvailability();
    }

    /**
     * Handles updates related to assets within the campaign.
     *
     * <p>Schedules a funds update to ensure the campaign's financial state is current when assets change.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param assetEvent the event indicating a change in assets
     */
    @Subscribe
    public void handle(AssetEvent assetEvent) {
        fundsScheduler.schedule();
    }

    /**
     * Handles updates when the pool of available AsTechs changes.
     *
     * <p>Refreshes the temporary AsTech pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param asTechPoolChangedEvent the event indicating a change in the AsTech pool
     */
    @Subscribe
    public void handle(AsTechPoolChangedEvent asTechPoolChangedEvent) {
        refreshTempAsTechs();
    }

    /**
     * Handles updates when the pool of available medics changes.
     *
     * <p>Refreshes the temporary medic pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param medicPoolChangedEvent the event indicating a change in the medic pool
     */
    @Subscribe
    public void handle(MedicPoolChangedEvent medicPoolChangedEvent) {
        refreshTempMedics();
    }

    /**
     * Handles changes to general application options.
     *
     * <p>Updates the visibility of the company generator menu item according to the new option settings.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param mhqOptionsChangedEvent the event containing the updated general options
     */
    @Subscribe
    public void handle(final MHQOptionsChangedEvent mhqOptionsChangedEvent) {
        miCompanyGenerator.setVisible(MekHQ.getMHQOptions().getShowCompanyGenerator());
    }
    // endregion Subscriptions
}
