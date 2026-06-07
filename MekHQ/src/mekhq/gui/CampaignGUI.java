/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.campaign.force.Formation.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.personnel.skills.SkillType.getExperienceLevelName;
import static mekhq.gui.dialog.nagDialogs.NagController.triggerDailyNags;
import static mekhq.gui.enums.MHQTabType.COMMAND_CENTER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
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
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.*;
import mekhq.campaign.events.loans.LoanEvent;
import mekhq.campaign.events.missions.MissionEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.transactions.TransactionEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.NewsItem;
import mekhq.campaign.utilities.AutomatedTechAssignments;
import mekhq.gui.baseComponents.ScalingWidthConstrainedPanel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.baseComponents.roundedComponents.RoundedMMToggleButton;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.glossary.GlossaryDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.menus.MekHQMenuBar;
import mekhq.gui.model.PartsTableModel;
import mekhq.gui.view.AdvanceTimePanel;
import mekhq.gui.view.CommandSummaryPanel;
import mekhq.gui.view.CurrentLocationPanel;

/**
 * The application's main frame.
 */
public class CampaignGUI extends JPanel {
    private static final MMLogger logger = MMLogger.create(CampaignGUI.class);

    @Serial
    private static final long serialVersionUID = 3126634639249129512L;
    // region Variable Declarations
    private static final int MAX_START_WIDTH = 1400;
    private static final int MAX_START_HEIGHT = 900;
    private static final int MIN_WINDOW_WIDTH = 1024;
    private static final int MIN_WINDOW_HEIGHT = 768;
    private static final int TOP_PANEL_HEIGHT = 90;
    public static int THIN_GAP = 2;
    public static int SMALL_GAP = 4;
    public static int MEDIUM_GAP = 8;

    // the max quantity when mass purchasing parts, hiring, etc. using the JSpinner
    public static final int MAX_QUANTITY_SPINNER = 10000;

    private JFrame frame;

    private final MekHQ app;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    /* for the main panel */
    private EnhancedTabbedPane tabMain;

    private MekHQMenuBar windowMenu;

    private final EnumMap<MHQTabType, CampaignGuiTab> standardTabs;

    /* Components for the status panel */
    private JPanel statusPanel;
    private JLabel lblTempAsTechs;
    private JLabel lblTempMedics;
    private JLabel lblTempSoldiers;
    private JLabel lblTempBattleArmor;
    private JLabel lblTempVehicleCrewGround;
    private JLabel lblTempVehicleCrewVTOL;
    private JLabel lblTempVehicleCrewNaval;
    private JPanel pnlVehicleCrew;
    private JLabel lblTempVesselPilot;
    private JLabel lblTempVesselGunner;
    private JLabel lblTempVesselCrew;
    private JPanel pnlVesselCrew;
    private JLabel lblPartsAvailabilityRating;

    /* Top Panel */
    private JPanel pnlTop;
    private RoundedJButton btnCompanyGenerator;
    private final RoundedJButton btnContractMarket =
          new RoundedJButton(resourceMap.getString("btnContractMarket.market"));
    private final RoundedJButton btnUnitMarket = new RoundedJButton(resourceMap.getString("btnUnitMarket.market"));

    ReportHyperlinkListener reportHLL;

    private boolean logNagActive = false;

    private transient StandardFormationIcon copyFormationIcon = null;
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
     * @return the formation icon to paste
     */
    public @Nullable StandardFormationIcon getCopyFormationIcon() {
        return copyFormationIcon;
    }

    public void setCopyFormationIcon(final @Nullable StandardFormationIcon copyFormationIcon) {
        this.copyFormationIcon = copyFormationIcon;
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

        initTopPanel();
        initStatusBar();

        setLayout(new BorderLayout());

        add(tabMain, BorderLayout.CENTER);
        add(pnlTop, BorderLayout.PAGE_START);
        add(statusPanel, BorderLayout.PAGE_END);

        standardTabs.values().forEach(CampaignGuiTab::refreshAll);

        refreshWindowTitle();
        refreshCampaignControlButtons();
        refreshTempAsTechs();
        refreshTempMedics();
        refreshTempSoldiers();
        refreshTempBattleArmor();
        refreshTempVehicleCrewGround();
        refreshTempVehicleCrewVTOL();
        refreshTempVehicleCrewNaval();
        refreshTempVesselPilot();
        refreshTempVesselGunner();
        refreshTempVesselCrew();
        refreshPartsAvailability();

        Dimension screenSize = getUsableScreenSize();

        frame.setSize(Math.min(MAX_START_WIDTH, screenSize.width), Math.min(MAX_START_HEIGHT, screenSize.height));
        refreshGuiScale();

        // Determine the new location of the window
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (screenSize.width - w) / 2;
        int y = (screenSize.height - h) / 2;

        // Move the window
        frame.setLocation(x, y);

        windowMenu = new MekHQMenuBar(getApplication(), this);
        frame.setJMenuBar(windowMenu);

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

        // on Mac, override auto-added "Quit MekHQ" behavior to work like other exit variants (ask for save etc)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            Desktop.getDesktop().setQuitHandler((e, response) -> {
                getApplication().exit(true);
                response.cancelQuit(); // don't remove this
            });
        }

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

    private void initStatusBar() {
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 20, 4));
        statusPanel.getAccessibleContext().setAccessibleName("Status Bar");

        lblTempAsTechs = new JLabel();
        lblTempMedics = new JLabel();
        lblTempSoldiers = new JLabel();
        lblTempBattleArmor = new JLabel();
        lblTempVehicleCrewGround = new JLabel();
        lblTempVehicleCrewVTOL = new JLabel();
        lblTempVehicleCrewNaval = new JLabel();
        lblTempVesselPilot = new JLabel();
        lblTempVesselGunner = new JLabel();
        lblTempVesselCrew = new JLabel();
        lblPartsAvailabilityRating = new JLabel();

        Border innerBorder = BorderFactory.createCompoundBorder(
              new RoundedLineBorder(UIUtil.uiIndependentGray(), 1, 8),
              BorderFactory.createEmptyBorder(1, 3, 1, 3));

        pnlVehicleCrew = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        pnlVehicleCrew.setBorder(innerBorder);
        pnlVehicleCrew.add(new JLabel(statusBarLabel("statusBar.pnlVehicleCrew.title")));
        pnlVehicleCrew.add(lblTempVehicleCrewGround);
        pnlVehicleCrew.add(lblTempVehicleCrewVTOL);
        pnlVehicleCrew.add(lblTempVehicleCrewNaval);

        pnlVesselCrew = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        pnlVesselCrew.setBorder(innerBorder);
        pnlVesselCrew.add(new JLabel(statusBarLabel("statusBar.pnlVesselCrew.title")));
        pnlVesselCrew.add(lblTempVesselPilot);
        pnlVesselCrew.add(lblTempVesselGunner);
        pnlVesselCrew.add(lblTempVesselCrew);

        JPanel pnlTempPersonnel = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 2));
        pnlTempPersonnel.setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        pnlTempPersonnel.add(new JLabel(statusBarLabel("statusBar.pnlTempPersonnel.title")));
        pnlTempPersonnel.add(lblTempAsTechs);
        pnlTempPersonnel.add(lblTempMedics);
        pnlTempPersonnel.add(lblTempSoldiers);
        pnlTempPersonnel.add(lblTempBattleArmor);
        pnlTempPersonnel.add(pnlVehicleCrew);
        pnlTempPersonnel.add(pnlVesselCrew);

        statusPanel.add(pnlTempPersonnel);
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
    private void initTopPanel() {
        pnlTop = new JPanel();
        pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.X_AXIS));

        pnlTop.add(new CurrentLocationPanel(365, 420, TOP_PANEL_HEIGHT - 30,
              getCampaign(), this::openRecruitmentDialog));
        pnlTop.add(createMarketsPanel(95, 130));
        pnlTop.add(new CommandSummaryPanel(250, 280, getCampaign()));
        pnlTop.add(Box.createHorizontalGlue());
        pnlTop.add(new AdvanceTimePanel(170, 240, getCampaign().getLocalDate(),
              getCampaignController()::advanceDay, () -> new AdvanceDaysDialog(getFrame(), this).setVisible(true)));
        pnlTop.add(createCampaignControlPanel(140, 170));
    }

    private JPanel createMarketsPanel(int minWidth, int maxWidth) {
        JPanel pnlMarkets = new ScalingWidthConstrainedPanel(minWidth, maxWidth);
        pnlMarkets.setLayout(new GridBagLayout());
        pnlMarkets.setBorder(RoundedLineBorder.createRoundedLineBorder(resourceMap.getString("pnlMarkets.title")));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;

        btnContractMarket.setToolTipText(resourceMap.getString("btnContractMarket.toolTipText"));
        btnContractMarket.addActionListener(e -> showContractMarket());
        btnContractMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnContractMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridy = 0;
        pnlMarkets.add(btnContractMarket, gridBagConstraints);

        btnUnitMarket.setToolTipText(resourceMap.getString("btnUnitMarket.toolTipText"));
        btnUnitMarket.addActionListener(e -> showUnitMarket());
        btnUnitMarket.setHorizontalTextPosition(SwingConstants.CENTER);
        btnUnitMarket.setVerticalTextPosition(SwingConstants.CENTER);
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(SMALL_GAP, 0, 0, 0);
        pnlMarkets.add(btnUnitMarket, gridBagConstraints);

        refreshMarketButtonLabels();

        return pnlMarkets;
    }

    public void refreshMarketButtonLabels() {
        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        String labelKey = campaignOptions.getContractMarketMethod().isNone() ? "manual" : "market";
        String label = resourceMap.getString("btnContractMarket." + labelKey);

        btnContractMarket.setText(label);

        labelKey = getCampaign().getUnitMarket().getMethod().isNone() ? "manual" : "market";
        label = resourceMap.getString("btnUnitMarket." + labelKey);
        btnUnitMarket.setText(label);
    }

    private JPanel createCampaignControlPanel(int minWidth, int maxWidth) {
        JPanel pnlButton = new ScalingWidthConstrainedPanel(minWidth, maxWidth);
        pnlButton.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        btnCompanyGenerator = new RoundedJButton(resourceMap.getString("btnCompanyGenerator.text"));
        btnCompanyGenerator.setToolTipText(resourceMap.getString("btnCompanyGenerator.toolTipText"));
        btnCompanyGenerator.addActionListener(
              e -> new CompanyGenerationDialog(getFrame(), getCampaign()).setVisible(true));
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(MEDIUM_GAP - 2, SMALL_GAP, -2, SMALL_GAP);
        pnlButton.add(btnCompanyGenerator, gridBagConstraints);

        RoundedMMToggleButton btnGMMode = new RoundedMMToggleButton(resourceMap.getString("btnGMMode.text"));
        btnGMMode.setToolTipText(resourceMap.getString("btnGMMode.toolTipText"));
        btnGMMode.setSelected(getCampaign().isGM());
        btnGMMode.addActionListener(e -> {
            getCampaign().setGMMode(btnGMMode.isSelected());
            windowMenu.refreshGMMenuItems();
        });
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(MEDIUM_GAP - 2, SMALL_GAP, 0, SMALL_GAP);
        pnlButton.add(btnGMMode, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        RoundedJButton btnGlossary = new RoundedJButton(resourceMap.getString("btnGlossary.text"));
        btnGlossary.setToolTipText(resourceMap.getString("btnGlossary.toolTipText"));
        btnGlossary.addActionListener(evt -> new GlossaryDialog(getFrame()));
        gridBagConstraints.weightx = 0.4;
        gridBagConstraints.insets = new Insets(SMALL_GAP, SMALL_GAP, THIN_GAP, 0);
        pnlButton.add(btnGlossary, gridBagConstraints);

        RoundedJButton btnBugReport = new RoundedJButton(resourceMap.getString("btnBugReport.text"));
        btnBugReport.setToolTipText(resourceMap.getString("btnBugReport.toolTipText"));
        btnBugReport.addActionListener(evt -> new EasyBugReportDialog(getFrame(), getCampaign()));
        gridBagConstraints.weightx = 0.6;
        gridBagConstraints.insets = new Insets(SMALL_GAP, SMALL_GAP, THIN_GAP, SMALL_GAP);
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

    public @Nullable RepairTab getRepairBayTab() {
        return (RepairTab) getTab(MHQTabType.REPAIR_BAY);
    }

    public @Nullable HangarTab getHangarTab() {
        return (HangarTab) getTab(MHQTabType.HANGAR);
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

    private void refreshGuiScale() {
        Dimension screenSize = getUsableScreenSize();
        int minWidth = Math.min(UIUtil.scaleForGUI(MIN_WINDOW_WIDTH), screenSize.width);
        int minHeight = Math.min(UIUtil.scaleForGUI(MIN_WINDOW_HEIGHT), screenSize.height);
        frame.setMinimumSize(new Dimension(minWidth, minHeight));
        pnlTop.setMinimumSize(new Dimension(minWidth, UIUtil.scaleForGUI(TOP_PANEL_HEIGHT)));
        pnlTop.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(TOP_PANEL_HEIGHT)));
    }

    private static Dimension getUsableScreenSize() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        int usableScreenWidth = screenSize.width - screenInsets.left - screenInsets.right;
        int usableScreenHeight = screenSize.height - screenInsets.top - screenInsets.bottom;
        return new Dimension(usableScreenWidth, usableScreenHeight);
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

    /**
     * Opens the recruitment dialog to hire a person, using the appropriate market style based on campaign options.
     *
     * <p>If the style recruitment is disabled in the campaign options, a deprecated {@link PersonnelMarketDialog} is
     * displayed. Otherwise, the new recruitment dialog is shown according to the campaign's current market style.</p>
     *
     * <p>If all recruitment options are disabled, display the bulk recruitment dialog (GM), instead.</p>
     */
    public void openRecruitmentDialog() {
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
            openBulkRecruitmentDialog();
        }
    }

    public void openBulkRecruitmentDialog() {
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
            NewContractDialog newContractDialog = campaignOptions.isUseStratCon() ?
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
                                   techList.getFirst());

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
        CommandCenterTab commandCenterTab = getCommandCenterTab();
        if (commandCenterTab == null) {
            logger.warn("Command Center tab is unavailable, cannot check for daily log nag");
            return;
        }

        // If we're already nagging, no need to nag again
        boolean subTabNagActive = commandCenterTab.isLogNagActive(logType);
        int relevantIndex = logType.getTabIndex();

        // We're already nagging
        if (logNagActive && subTabNagActive) {
            return;
        }

        final int selectedIndex = tabMain.getSelectedIndex();
        final Component selectedTab = ((selectedIndex >= 0) && (selectedIndex < tabMain.getTabCount())) ?
                                            tabMain.getComponentAt(selectedIndex) : null;

        EnhancedTabbedPane tabLogs = commandCenterTab.getTabLogs();
        int logsSelected = tabLogs.getSelectedIndex();

        // If the player is already viewing the correct log tab, no nag needed.
        if (commandCenterTab.isShowing() && (logsSelected == relevantIndex)) {
            return;
        }

        // If the Command Center is still attached and not currently selected, color that tab's label.
        int commandCenterIndex = tabMain.indexOfComponent(commandCenterTab);
        if ((commandCenterIndex >= 0) && (selectedTab != commandCenterTab)) {
            tabMain.setBackgroundAt(commandCenterIndex, UIUtil.uiDarkBlue());
            logNagActive = true;
        }

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
                case AGGREGATE -> commandCenterTab.getAggregateLog();
            };

            if (!DailyReportLogPanel.isDateOnly(List.of(reportTab.getLogText()))) {
                commandCenterTab.nagLogTab(relevantIndex);
                commandCenterTab.setLogNagActive(logType, true);
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

    public void refreshWindowTitle() {
        getFrame().setTitle(getCampaign().getTitle());
    }

    private String statusBarLabel(String key, Object... args) {
        String label = getFormattedTextAt("mekhq.resources.CampaignGUI", key);
        if (args.length == 0) {
            return "<html><b>" + label + "</b></html>";
        }
        return "<html><b>" + label + "</b>: " + args[0] + "</html>";
    }

    private void refreshTempAsTechs() {
        lblTempAsTechs.setText(statusBarLabel("statusBar.lblTempAsTechs.text",
              getCampaign().getTemporaryAsTechPool()));
    }

    private void refreshTempMedics() {
        lblTempMedics.setText(statusBarLabel("statusBar.lblTempMedics.text",
              getCampaign().getTemporaryMedicPool()));
    }

    private void refreshTempSoldiers() {
        if (!getCampaign().getCampaignOptions().isUseBlobInfantry()) {
            lblTempSoldiers.setVisible(false);
            return;
        }
        lblTempSoldiers.setVisible(true);
        lblTempSoldiers.setText(statusBarLabel("statusBar.lblTempSoldiers.text",
              getCampaign().getTempCrewPool(PersonnelRole.SOLDIER)));
    }

    private void refreshTempBattleArmor() {
        if (!getCampaign().getCampaignOptions().isUseBlobBattleArmor()) {
            lblTempBattleArmor.setVisible(false);
            return;
        }
        lblTempBattleArmor.setVisible(true);
        lblTempBattleArmor.setText(statusBarLabel("statusBar.lblTempBattleArmor.text",
              getCampaign().getTempCrewPool(PersonnelRole.BATTLE_ARMOUR)));
    }

    private void refreshTempVehicleCrewGround() {
        if (!getCampaign().getCampaignOptions().isUseBlobVehicleCrewGround()) {
            lblTempVehicleCrewGround.setVisible(false);
            refreshVehicleCrewPanelVisibility();
            return;
        }
        lblTempVehicleCrewGround.setVisible(true);
        lblTempVehicleCrewGround.setText(statusBarLabel("statusBar.lblTempVehicleCrewGround.text",
              getCampaign().getTempCrewPool(PersonnelRole.VEHICLE_CREW_GROUND)));
        refreshVehicleCrewPanelVisibility();
    }

    private void refreshTempVehicleCrewVTOL() {
        if (!getCampaign().getCampaignOptions().isUseBlobVehicleCrewVTOL()) {
            lblTempVehicleCrewVTOL.setVisible(false);
            refreshVehicleCrewPanelVisibility();
            return;
        }
        lblTempVehicleCrewVTOL.setVisible(true);
        lblTempVehicleCrewVTOL.setText(statusBarLabel("statusBar.lblTempVehicleCrewVTOL.text",
              getCampaign().getTempCrewPool(PersonnelRole.VEHICLE_CREW_VTOL)));
        refreshVehicleCrewPanelVisibility();
    }

    private void refreshTempVehicleCrewNaval() {
        if (!getCampaign().getCampaignOptions().isUseBlobVehicleCrewNaval()) {
            lblTempVehicleCrewNaval.setVisible(false);
            refreshVehicleCrewPanelVisibility();
            return;
        }
        lblTempVehicleCrewNaval.setVisible(true);
        lblTempVehicleCrewNaval.setText(statusBarLabel("statusBar.lblTempVehicleCrewNaval.text",
              getCampaign().getTempCrewPool(PersonnelRole.VEHICLE_CREW_NAVAL)));
        refreshVehicleCrewPanelVisibility();
    }

    private void refreshVehicleCrewPanelVisibility() {
        pnlVehicleCrew.setVisible(lblTempVehicleCrewGround.isVisible()
                                        || lblTempVehicleCrewVTOL.isVisible()
                                        || lblTempVehicleCrewNaval.isVisible());
    }

    private void refreshTempVesselPilot() {
        if (!getCampaign().getCampaignOptions().isUseBlobVesselPilot()) {
            lblTempVesselPilot.setVisible(false);
            refreshVesselCrewPanelVisibility();
            return;
        }
        lblTempVesselPilot.setVisible(true);
        lblTempVesselPilot.setText(statusBarLabel("statusBar.lblTempVesselPilot.text",
              getCampaign().getTempCrewPool(PersonnelRole.VESSEL_PILOT)));
        refreshVesselCrewPanelVisibility();
    }

    private void refreshTempVesselGunner() {
        if (!getCampaign().getCampaignOptions().isUseBlobVesselGunner()) {
            lblTempVesselGunner.setVisible(false);
            refreshVesselCrewPanelVisibility();
            return;
        }
        lblTempVesselGunner.setVisible(true);
        lblTempVesselGunner.setText(statusBarLabel("statusBar.lblTempVesselGunner.text",
              getCampaign().getTempCrewPool(PersonnelRole.VESSEL_GUNNER)));
        refreshVesselCrewPanelVisibility();
    }

    private void refreshTempVesselCrew() {
        if (!getCampaign().getCampaignOptions().isUseBlobVesselCrew()) {
            lblTempVesselCrew.setVisible(false);
            refreshVesselCrewPanelVisibility();
            return;
        }
        lblTempVesselCrew.setVisible(true);
        lblTempVesselCrew.setText(statusBarLabel("statusBar.lblTempVesselCrew.text",
              getCampaign().getTempCrewPool(PersonnelRole.VESSEL_CREW)));
        refreshVesselCrewPanelVisibility();
    }

    private void refreshVesselCrewPanelVisibility() {
        pnlVesselCrew.setVisible(lblTempVesselPilot.isVisible()
                                       || lblTempVesselGunner.isVisible()
                                       || lblTempVesselCrew.isVisible());
    }

    private void refreshPartsAvailability() {
        if (getCampaign().getCampaignOptions().getAcquisitionType() == AcquisitionsType.ANY_TECH) {
            lblPartsAvailabilityRating.setText("");
        } else {
            int partsAvailability = getCampaign().findAtBPartsAvailabilityLevel();
            lblPartsAvailabilityRating.setText(statusBarLabel("statusBar.lblPartsAvailabilityRating.text",
                  partsAvailability));
        }
    }

    private void refreshCampaignControlButtons() {
        boolean emptyHangar = getCampaign().getUnits().isEmpty();
        boolean noPersonnel = getCampaign().getPersonnel().isEmpty();
        btnCompanyGenerator.setVisible(emptyHangar && noPersonnel);
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
        Formation f = getCampaign().getFormation(u.getFormationId());
        if (f != null) {
            undeployForce(f, false);
        }
        Scenario s = getCampaign().getScenario(u.getScenarioId());
        s.removeUnit(u.getId());
        u.undeploy();
        MekHQ.triggerEvent(new DeploymentChangedEvent(u, s));
    }

    public void undeployForce(Formation f) {
        undeployForce(f, true);
    }

    public void undeployForce(Formation f, boolean killSubs) {
        int sid = f.getScenarioId();
        Scenario scenario = getCampaign().getScenario(sid);
        if (null != scenario) {
            f.clearScenarioIds(getCampaign(), killSubs);
            scenario.removeFormation(f.getId());
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
            Formation parent = f;
            int prevId = f.getId();
            while ((parent = parent.getParentFormation()) != null) {
                if (parent.getScenarioId() == NO_ASSIGNED_SCENARIO) {
                    break;
                }
                parent.clearScenarioIds(getCampaign(), false);
                scenario.removeFormation(parent.getId());
                for (Formation sub : parent.getSubFormations()) {
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
            boolean skipReports = false;
            AutomatedTechAssignments.handleTheAutomaticAssignmentOfUnmaintainedUnits(getCampaign(), skipReports);
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
        refreshWindowTitle();
        refreshCampaignControlButtons();
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

        // Update blob crew label visibility
        refreshTempSoldiers();
        refreshTempBattleArmor();
        refreshTempVehicleCrewGround();
        refreshTempVehicleCrewVTOL();
        refreshTempVehicleCrewNaval();
        refreshTempVesselPilot();
        refreshTempVesselGunner();
        refreshTempVesselCrew();

        refreshAllTabs();
        refreshPartsAvailability();

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
        refreshPartsAvailability();
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
     * Handles updates when the pool of available soldiers changes.
     *
     * <p>Refreshes the temporary soldier pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param soldierPoolChangedEvent the event indicating a change in the soldier pool
     */
    @Subscribe
    public void handle(SoldierPoolChangedEvent soldierPoolChangedEvent) {
        refreshTempSoldiers();
    }

    /**
     * Handles updates when the pool of available battle armor personnel changes.
     *
     * <p>Refreshes the temporary battle armor pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param battleArmorPoolChangedEvent the event indicating a change in the battle armor pool
     */
    @Subscribe
    public void handle(BattleArmorPoolChangedEvent battleArmorPoolChangedEvent) {
        refreshTempBattleArmor();
    }

    /**
     * Handles updates when the pool of available ground vehicle crew changes.
     *
     * <p>Refreshes the temporary ground vehicle crew pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vehicleCrewGroundPoolChangedEvent the event indicating a change in the vehicle crew ground pool
     */
    @Subscribe
    public void handle(VehicleCrewGroundPoolChangedEvent vehicleCrewGroundPoolChangedEvent) {
        refreshTempVehicleCrewGround();
    }

    /**
     * Handles updates when the pool of available VTOL crew changes.
     *
     * <p>Refreshes the temporary VTOL crew pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vehicleCrewVTOLPoolChangedEvent the event indicating a change in the vehicle crew VTOL pool
     */
    @Subscribe
    public void handle(VehicleCrewVTOLPoolChangedEvent vehicleCrewVTOLPoolChangedEvent) {
        refreshTempVehicleCrewVTOL();
    }

    /**
     * Handles updates when the pool of available naval vehicle crew changes.
     *
     * <p>Refreshes the temporary naval vehicle crew pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vehicleCrewNavalPoolChangedEvent the event indicating a change in the vehicle crew naval pool
     */
    @Subscribe
    public void handle(VehicleCrewNavalPoolChangedEvent vehicleCrewNavalPoolChangedEvent) {
        refreshTempVehicleCrewNaval();
    }

    /**
     * Handles updates when the pool of available vessel pilots changes.
     *
     * <p>Refreshes the temporary vessel pilot pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vesselPilotPoolChangedEvent the event indicating a change in the vessel pilot pool
     */
    @Subscribe
    public void handle(VesselPilotPoolChangedEvent vesselPilotPoolChangedEvent) {
        refreshTempVesselPilot();
    }

    /**
     * Handles updates when the pool of available vessel gunners changes.
     *
     * <p>Refreshes the temporary vessel gunner pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vesselGunnerPoolChangedEvent the event indicating a change in the vessel gunner pool
     */
    @Subscribe
    public void handle(VesselGunnerPoolChangedEvent vesselGunnerPoolChangedEvent) {
        refreshTempVesselGunner();
    }

    /**
     * Handles updates when the pool of available vessel crew changes.
     *
     * <p>Refreshes the temporary vessel crew pool, updating the related UI and game state.</p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param vesselCrewPoolChangedEvent the event indicating a change in the vessel crew pool
     */
    @Subscribe
    public void handle(VesselCrewPoolChangedEvent vesselCrewPoolChangedEvent) {
        refreshTempVesselCrew();
    }

    /**
     * Handles changes to general application options.
     *
     * <p>
     * Updates the visibility of the company generator menu item according to the new option settings. Ensures that GUI
     * scale changes are applied.
     * </p>
     *
     * <p><b>Important:</b> This method is not directly evoked, so IDEA will tell you it has no uses. IDEA is
     * wrong.</p>
     *
     * @param mhqOptionsChangedEvent the event containing the updated general options
     */
    @Subscribe
    public void handle(final MHQOptionsChangedEvent mhqOptionsChangedEvent) {
        refreshGuiScale();
    }
    // endregion Subscriptions
}
