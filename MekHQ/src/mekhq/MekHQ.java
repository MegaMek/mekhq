/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq;

import static megamek.MMConstants.LOCALHOST_IP;

import java.awt.FileDialog;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputFilter.Config;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.InputMap;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultEditorKit;

import io.sentry.Sentry;
import megamek.MMLoggingConstants;
import megamek.MegaMek;
import megamek.SuiteConstants;
import megamek.client.Client;
import megamek.client.HeadlessClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.abstractDialogs.AutoResolveChanceDialog;
import megamek.client.ui.dialogs.abstractDialogs.AutoResolveProgressDialog;
import megamek.client.ui.dialogs.gameConnectionDialogs.ConnectDialog;
import megamek.client.ui.dialogs.gameConnectionDialogs.HostDialog;
import megamek.client.ui.dialogs.helpDialogs.AutoResolveSimulationLogDialog;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.preferences.SuitePreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.autoResolve.acar.SimulatedClient;
import megamek.common.autoResolve.converter.SetupForces;
import megamek.common.autoResolve.converter.SingletonForces;
import megamek.common.autoResolve.event.AutoResolveConcludedEvent;
import megamek.common.board.Board;
import megamek.common.event.*;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityNewOffboardEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.event.player.GamePlayerConnectedEvent;
import megamek.common.event.player.GamePlayerDisconnectedEvent;
import megamek.common.internationalization.I18n;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megameklab.MegaMekLab;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.autoresolve.MekHQSetupForces;
import mekhq.campaign.autoresolve.StratconSetupForces;
import mekhq.campaign.handler.PostScenarioDialogHandler;
import mekhq.campaign.handler.XPHandler;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.ScenarioTemplate.BattlefieldControlType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.ChooseMulFilesDialog;
import mekhq.gui.dialog.ResolveScenarioWizardDialog;
import mekhq.gui.panels.StartupScreenPanel;
import mekhq.gui.preferences.StringPreference;
import mekhq.gui.utilities.ObservableString;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ScenarioUtils;

/**
 * The main class of the application.
 */
public class MekHQ implements GameListener {
    private static final MMLogger LOGGER = MMLogger.create(MekHQ.class);

    // region Variable Declarations
    private static final SuitePreferences mhqPreferences = new SuitePreferences();
    private static final MHQOptions mhqOptions = new MHQOptions();
    private static final EventBus EVENT_BUS = new EventBus();

    private static ObservableString selectedTheme;

    // Deprecated directory options
    private static ObservableString personnelDirectory;
    private static ObservableString partsDirectory;
    private static ObservableString planetsDirectory;
    private static ObservableString starMapsDirectory;
    private static ObservableString unitsDirectory;
    private static ObservableString campaignsDirectory;
    private static ObservableString scenarioTemplatesDirectory;
    private static ObservableString financesDirectory;

    // stuff related to MM games
    private Server myServer = null;
    private GameThread gameThread = null;
    private Scenario currentScenario = null;
    private Client client = null;

    // the actual campaign - this is where the good stuff is
    private CampaignController campaignController;
    private CampaignGUI campaignGUI;

    private final IconPackage iconPackage = new IconPackage();

    private final IAutosaveService autosaveService;
    // endregion Variable Declarations
    private static final SanityInputFilter sanityInputFilter = new SanityInputFilter();
    private static final String defaultTheme = "com.formdev.flatlaf.FlatDarculaLaf";

    public static SuitePreferences getMHQPreferences() {
        return mhqPreferences;
    }

    public static MHQOptions getMHQOptions() {
        return mhqOptions;
    }

    public static ObservableString getSelectedTheme() {
        return selectedTheme;
    }

    public static ObservableString getPersonnelDirectory() {
        return personnelDirectory;
    }

    public static ObservableString getPartsDirectory() {
        return partsDirectory;
    }

    public static ObservableString getPlanetsDirectory() {
        return planetsDirectory;
    }

    public static ObservableString getStarMapsDirectory() {
        return starMapsDirectory;
    }

    public static ObservableString getUnitsDirectory() {
        return unitsDirectory;
    }

    public static ObservableString getCampaignsDirectory() {
        return campaignsDirectory;
    }

    public static ObservableString getScenarioTemplatesDirectory() {
        return scenarioTemplatesDirectory;
    }

    public static ObservableString getFinancesDirectory() {
        return financesDirectory;
    }

    protected static MekHQ getInstance() {
        return new MekHQ();
    }

    private MekHQ() {
        this.autosaveService = new AutosaveService();
    }

    /**
     * At startup create and show the main frame of the application.
     */
    protected void startup() {
        // Setup user preferences
        MegaMek.getMMPreferences().loadFromFile(SuiteConstants.MM_PREFERENCES_FILE);
        MegaMekLab.getMMLPreferences().loadFromFile(SuiteConstants.MML_PREFERENCES_FILE);
        getMHQPreferences().loadFromFile(SuiteConstants.MHQ_PREFERENCES_FILE);

        setUserPreferences();
        updateGuiScaling(); // also sets the look-and-feel
        setTooltipSettings();

        initEventHandlers();
        // create a start-up frame and display it
        new StartupScreenPanel(this).getFrame().setVisible(true);
    }

    /**
     * Configures the global tooltip display settings to show tooltips immediately and keep them visible.
     *
     * <p>This method sets three tooltip behaviors:</p>
     * <ul>
     *     <li>Initial Delay: 0 ms (tooltips appear instantly when hovering)</li>
     *     <li>Dismiss Delay: Maximum integer value (tooltips stay visible indefinitely)</li>
     *     <li>Reshow Delay: 0 ms (tooltips reappear instantly when moving between components)</li>
     * </ul>
     * <p>
     * These settings affect all tooltips application-wide through the shared ToolTipManager instance.
     */
    private static void setTooltipSettings() {
        ToolTipManager tooltipManager = ToolTipManager.sharedInstance();
        tooltipManager.setDismissDelay(Integer.MAX_VALUE);
        tooltipManager.setReshowDelay(0);
    }

    /**
     * restart back to the splash screen
     */
    public void restart() {

        // Actually close MHQ
        if (campaignGUI != null) {
            campaignGUI.getFrame().dispose();
        }

        new StartupScreenPanel(this).getFrame().setVisible(true);
    }

    /**
     * Retrieves the autosave service instance associated with this instance of {@link MekHQ}.
     *
     * <p>This service is responsible for handling autosave operations, such as saving the current state
     * of the campaign or mission. It provides an interface to manage autosave requests.</p>
     *
     * @return the {@link IAutosaveService} instance responsible for managing autosave operations.
     */
    public IAutosaveService getAutosaveService() {
        return autosaveService;
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private static void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekHQ.class);

            // TODO: complete integration of Suite Preferences, including GUIPreferences
            selectedTheme = new ObservableString("selectedTheme", "");
            selectedTheme.addPropertyChangeListener(new MekHqPropertyChangedListener());
            preferences.manage(new StringPreference(selectedTheme));

            personnelDirectory = new ObservableString("personnelDirectory", ".");
            preferences.manage(new StringPreference(personnelDirectory));

            partsDirectory = new ObservableString("partsDirectory", ".");
            preferences.manage(new StringPreference(partsDirectory));

            planetsDirectory = new ObservableString("planetsDirectory", ".");
            preferences.manage(new StringPreference(planetsDirectory));

            starMapsDirectory = new ObservableString("starMapsDirectory", ".");
            preferences.manage(new StringPreference(starMapsDirectory));

            unitsDirectory = new ObservableString("unitsDirectory", ".");
            preferences.manage(new StringPreference(unitsDirectory));

            campaignsDirectory = new ObservableString("campaignsDirectory", "./campaigns");
            preferences.manage(new StringPreference(campaignsDirectory));

            scenarioTemplatesDirectory = new ObservableString("scenarioTemplatesDirectory", ".");
            preferences.manage(new StringPreference(scenarioTemplatesDirectory));

            financesDirectory = new ObservableString("financesDirectory", ".");
            preferences.manage(new StringPreference(financesDirectory));
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to set user preferences");
        }
    }

    public void exit(boolean includeSavePrompt) {
        if (includeSavePrompt) {
            int savePrompt = JOptionPane.showConfirmDialog(null,
                  "Do you want to save the game before quitting MekHQ?",
                  "Save First?",
                  JOptionPane.YES_NO_CANCEL_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
            if ((savePrompt == JOptionPane.CANCEL_OPTION) || (savePrompt == JOptionPane.CLOSED_OPTION)) {
                return;
            } else if ((savePrompt == JOptionPane.YES_OPTION) && !getCampaigngui().saveCampaign(null)) {
                // When the user did not actually save the game, don't close MHQ
                return;
            }
        }

        // Actually close MHQ
        if (campaignGUI != null) {
            campaignGUI.getFrame().dispose();
        }

        MegaMek.getMMPreferences().saveToFile(SuiteConstants.MM_PREFERENCES_FILE);
        MegaMekLab.getMMLPreferences().saveToFile(SuiteConstants.MML_PREFERENCES_FILE);
        getMHQPreferences().saveToFile(SuiteConstants.MHQ_PREFERENCES_FILE);

        System.exit(0);
    }

    public void showNewView() {
        campaignGUI = new CampaignGUI(this);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String... args) {
        Config.setSerialFilter(sanityInputFilter);

        // Configure Sentry with defaults. Although the client defaults to enabled, the properties file is used to
        // disable it and additional configuration can be done inside the sentry.properties file. The defaults for
        // everything else is set here.
        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setDsn("https://a05b2064798e2b8d46ac620b4497a072@sentry.tapenvy.us/10");
            options.setEnvironment("production");
            options.setTracesSampleRate(1.0);
            options.setProfilesSampleRate(1.0);
            options.setEnableAppStartProfiling(true);
            options.setDebug(true);
            options.setServerName("MekHQClient");
            options.setRelease(SuiteConstants.VERSION.toString());
        });

        // First, create a global default exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            final String name = t.getClass().getName();
            final String message = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION, name);
            final String title = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION_TITLE, name);
            LOGGER.errorDialog(t, message, title);
        });

        // Second, let's handle logging
        MegaMek.initializeLogging(MHQConstants.PROJECT_NAME);
        MegaMekLab.initializeLogging(MHQConstants.PROJECT_NAME);
        MekHQ.initializeLogging(MHQConstants.PROJECT_NAME);

        // Third, let's handle suite graphical setup initialization
        MegaMek.initializeSuiteGraphicalSetups(MHQConstants.PROJECT_NAME);

        // Finally, let's handle startup
        SwingUtilities.invokeLater(() -> MekHQ.getInstance().startup());

        // log jvm parameters
        LOGGER.info(ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    public static void initializeLogging(final String originProject) {
        LOGGER.info(getUnderlyingInformation(originProject));
    }

    /**
     * @param originProject the project that launched MekHQ
     *
     * @return the underlying information for this launch of MekHQ
     */
    public static String getUnderlyingInformation(final String originProject) {
        return MegaMek.getUnderlyingInformation(originProject, MHQConstants.PROJECT_NAME);
    }

    public Server getMyServer() {
        return myServer;
    }

    public Campaign getCampaign() {
        return campaignController.getLocalCampaign();
    }

    public void setCampaign(Campaign c) {
        campaignController = new CampaignController(c);
    }

    public CampaignController getCampaignController() {
        return campaignController;
    }

    /**
     * @return the {@link CampaignGUI}
     */
    public CampaignGUI getCampaigngui() {
        return campaignGUI;
    }

    /**
     * @param campaignGUI the {@link CampaignGUI} to set
     */
    public void setCampaigngui(CampaignGUI campaignGUI) {
        this.campaignGUI = campaignGUI;
    }

    public void joinGame(Scenario scenario, List<Unit> meks) {
        ConnectDialog joinGameDialog = new ConnectDialog(campaignGUI.getFrame(), campaignGUI.getCampaign().getName());
        joinGameDialog.setVisible(true);

        if (!joinGameDialog.dataValidation("MegaMek.ConnectDialog.title")) {
            return;
        }

        final String playerName = joinGameDialog.getPlayerName();
        final String serverAddress = joinGameDialog.getServerAddress();
        final int port = joinGameDialog.getPort();
        joinGameDialog.dispose();

        try {
            client = new Client(playerName, serverAddress, port);
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to connect to server properly");
            return;
        }

        client.getGame().addGameListener(this);
        currentScenario = scenario;

        // Start the game thread
        gameThread = new GameThread(playerName, client, this, meks, scenario, false);
        gameThread.start();
    }

    /**
     * Start hosting a game. This method is used to start hosting a game. It will create a new server and a client and
     * connect to it.
     *
     * @param scenario     The scenario to host
     * @param loadSaveGame Whether to load a save game
     * @param meks         The units you want to use in the scenario
     */
    public void startHost(Scenario scenario, boolean loadSaveGame, List<Unit> meks) {
        startHost(scenario, loadSaveGame, meks, null);
    }

    /**
     * Start hosting a game. This method is used to start hosting a game. It will create a new server and a client and
     * connect to it.
     *
     * @param scenario                    The scenario to host
     * @param loadSaveGame                Whether to load a save game
     * @param meks                        The units you want to use in the scenario
     * @param autoResolveBehaviorSettings The auto resolve behavior settings to use if running an AtB scenario and auto
     *                                    resolve is wanted
     */
    public void startHost(Scenario scenario, boolean loadSaveGame, List<Unit> meks,
          @Nullable BehaviorSettings autoResolveBehaviorSettings) {
        HostDialog hostDialog = new HostDialog(campaignGUI.getFrame(), getCampaign().getName());
        hostDialog.setVisible(true);

        if (!hostDialog.dataValidation("MegaMek.HostGameAlert.title")) {
            stopHost();
            return;
        }

        this.autosaveService.requestBeforeScenarioAutosave(getCampaign());

        final String playerName = hostDialog.getPlayerName();
        final String password = hostDialog.getServerPass();
        final int port = hostDialog.getPort();
        final boolean register = hostDialog.isRegister();
        final String metaServer = register ? hostDialog.getMetaserver() : "";

        // Force cleanup of the current modal, since we are (possibly) about to display a new one and macOS seems to
        // struggle with that (see https://github.com/MegaMek/mekhq/issues/953)
        hostDialog.dispose();

        try {
            myServer = new Server(password, port, new TWGameManager(), register, metaServer);
            if (loadSaveGame) {
                FileDialog f = new FileDialog(campaignGUI.getFrame(), "Load Save Game");
                f.setDirectory(System.getProperty("user.dir") + "/savegames");
                f.setVisible(true);
                if (null != f.getFile()) {
                    getMyServer().loadGame(new File(f.getDirectory(), f.getFile()));
                } else {
                    stopHost();
                    return; // exceptions as flow control? no, thanks.
                }
            }
        } catch (FileNotFoundException ex) {
            // The dialog was cancelled or the file not found Return to the UI
            stopHost();
            return;
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to start up server");
            stopHost();
            return;
        }
        // Refactor this into a factory
        var useExperimentalPacarGui = getCampaign().getCampaignOptions().isAutoResolveExperimentalPacarGuiEnabled();
        if (autoResolveBehaviorSettings != null && useExperimentalPacarGui) {
            client = new HeadlessClient(playerName, LOCALHOST_IP, port);
        } else {
            client = new Client(playerName, LOCALHOST_IP, port);
        }

        client.getGame().addGameListener(this);
        currentScenario = scenario;

        // Start the game thread - also refactor this into a factory
        if (getCampaign().getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario atBScenario)) {
            gameThread = new AtBGameThread(playerName,
                  password,
                  client,
                  this,
                  meks,
                  atBScenario,
                  autoResolveBehaviorSettings,
                  useExperimentalPacarGui,
                  true);
        } else {
            gameThread = new GameThread(playerName, password, client, this, meks, scenario);
        }
        gameThread.start();
    }

    // Stop & send the close game event to the Server
    public synchronized void stopHost() {
        if (getMyServer() != null) {
            getMyServer().die();
            myServer = null;
        }
        currentScenario = null;
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // Why Empty?
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
        // Why Empty?
    }

    @Override
    public void gameEnd(GameEndEvent e) {
        // Why Empty?
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        // Why Empty?
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // Why Empty?
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
        // Why Empty?
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // Why Empty?
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
        // Why Empty?
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
        // Why Empty?
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Why Empty?
    }

    /**
     * This method is called automatically when the MegaMek game is over.
     *
     * @param gve post game resolution {@link PostGameResolution}
     */
    @Override
    public void gameVictory(PostGameResolution gve) {
        // Prevent double run
        if (gameThread.stopRequested()) {
            return;
        }
        try {
            String victoryMessage = MHQInternationalization.getText("ResolveDialog.control.message");

            if (currentScenario instanceof AtBDynamicScenario) {
                ScenarioTemplate template = ((AtBDynamicScenario) currentScenario).getTemplate();

                if (template != null) {
                    BattlefieldControlType battlefieldControl = template.getBattlefieldControl();

                    String controlMessage = MHQInternationalization.getText("ResolveDialog.control." +
                                                                                  battlefieldControl.name());

                    victoryMessage = String.format("%s\n\n%s", controlMessage, victoryMessage);
                }
            }

            boolean control = yourSideControlsTheBattlefieldDialogAsk(victoryMessage,
                  MHQInternationalization.getText("ResolveDialog.control.title"));
            ResolveScenarioTracker tracker = new ResolveScenarioTracker(currentScenario, getCampaign(), control);
            tracker.setClient(gameThread.getClient());
            tracker.setEvent(gve);
            tracker.processGame();

            ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(campaignGUI.getCampaign(),
                  campaignGUI.getFrame(),
                  true,
                  tracker);
            resolveDialog.setVisible(true);
            resolveDialog.dispose();

            if (!resolveDialog.wasAborted()) {
                PostScenarioDialogHandler.handle(campaignGUI, getCampaign(), currentScenario, tracker);
            }

        } catch (Exception ex) {
            LOGGER.error(ex, "gameVictory()");
        } finally {
            gameThread.requestStop();
        }
    }

    /**
     * This method is called when player wants to manually resolve the scenario providing MUL files.
     */
    public void resolveScenario(Scenario selectedScenario) {
        if (null == selectedScenario) {
            return;
        }

        String victoryMessage = MHQInternationalization.getText("ResolveDialog.control.message");

        if (selectedScenario instanceof AtBDynamicScenario) {
            ScenarioTemplate template = ((AtBDynamicScenario) selectedScenario).getTemplate();

            if (template != null) {
                BattlefieldControlType battlefieldControl = template.getBattlefieldControl();

                String controlMessage = MHQInternationalization.getText("ResolveDialog.control." +
                                                                              battlefieldControl.name());

                victoryMessage = String.format("%s\n\n%s", controlMessage, victoryMessage);
            }
        }

        boolean control = yourSideControlsTheBattlefieldDialogAsk(victoryMessage,
              MHQInternationalization.getText("ResolveDialog.control.title"));

        ResolveScenarioTracker tracker = new ResolveScenarioTracker(selectedScenario, getCampaign(), control);

        ChooseMulFilesDialog chooseFilesDialog = new ChooseMulFilesDialog(campaignGUI.getFrame(), true, tracker);
        chooseFilesDialog.setVisible(true);
        if (chooseFilesDialog.wasCancelled()) {
            return;
        }

        ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(getCampaign(),
              campaignGUI.getFrame(),
              true,
              tracker);
        resolveDialog.setVisible(true);

        if (resolveDialog.wasAborted()) {
            return;
        }

        PostScenarioDialogHandler.handle(campaignGUI, getCampaign(), selectedScenario, tracker);
    }

    private boolean yourSideControlsTheBattlefieldDialogAsk(String message, String title) {
        return JOptionPane.showConfirmDialog(campaignGUI.getFrame(),
              message,
              title,
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // region Event Handling Methods that are not implemented
    // These methods are here because MekHQ implements GameListener
    // but currently only needs to hear the post game resolution event

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    @Override
    public void gameReport(GameReportEvent e) {
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent e) {
    }
    // end region

    public IconPackage getIconPackage() {
        return iconPackage;
    }

    private SetupForces getSetupForces(Scenario scenario, List<Unit> units) {
        if (scenario instanceof AtBScenario atBScenario) {
            return new StratconSetupForces(getCampaign(), units, atBScenario, new SingletonForces());
        }
        return new MekHQSetupForces(getCampaign(), units, scenario, new SingletonForces());
    }

    /**
     * This method is called when the player wants to auto resolve the scenario using ACAR method
     *
     * @param units The list of player units involved in the scenario
     */
    public void startAutoResolve(Scenario scenario, List<Unit> units) {
        this.autosaveService.requestBeforeScenarioAutosave(getCampaign());

        Board board = ScenarioUtils.getBoardFor(scenario);
        SetupForces setupForces = getSetupForces(scenario, units);

        PlanetaryConditions planetaryConditions = getCampaign().getCurrentPlanetaryConditions(scenario);
        if (getCampaign().getCampaignOptions().isAutoResolveVictoryChanceEnabled()) {

            var proceed = AutoResolveChanceDialog.showDialog(campaignGUI.getFrame(),
                  getCampaign().getCampaignOptions().getAutoResolveNumberOfScenarios(),
                  Runtime.getRuntime().availableProcessors(),
                  1,
                  setupForces,
                  board,
                  planetaryConditions) == JOptionPane.YES_OPTION;
            if (!proceed) {
                return;
            }
        }

        var event = AutoResolveProgressDialog.showDialog(campaignGUI.getFrame(),
              setupForces,
              board, planetaryConditions);

        var autoResolveBattleReport = new AutoResolveSimulationLogDialog(campaignGUI.getFrame(), event.getLogFile());
        autoResolveBattleReport.setModal(true);
        autoResolveBattleReport.setVisible(true);

        autoResolveConcluded(event, scenario);
    }

    /**
     * This method is called when the auto resolve game is over.
     *
     * @param autoResolveConcludedEvent The event that contains the results of the auto resolve game.
     */
    public void autoResolveConcluded(AutoResolveConcludedEvent autoResolveConcludedEvent, Scenario scenario) {
        try {
            String victoryMessage = autoResolveConcludedEvent.controlledScenario() ?
                                          MHQInternationalization.getText("AutoResolveDialog.message.victory") :
                                          MHQInternationalization.getText("AutoResolveDialog.message.defeat");

            String decisionMessage = MHQInternationalization.getText("ResolveDialog.control.message");

            if (scenario instanceof AtBDynamicScenario atBDynamicScenario) {
                ScenarioTemplate template = atBDynamicScenario.getTemplate();

                if (template != null) {
                    BattlefieldControlType battlefieldControl = template.getBattlefieldControl();

                    String controlMessage = MHQInternationalization.getText("ResolveDialog.control." +
                                                                                  battlefieldControl.name());

                    victoryMessage = String.format("%s\n\n%s\n\n%s", controlMessage, victoryMessage, decisionMessage);
                }
            }

            String title = autoResolveConcludedEvent.controlledScenario() ?
                                 MHQInternationalization.getText("AutoResolveDialog.victory") :
                                 MHQInternationalization.getText("AutoResolveDialog.defeat");
            boolean control = yourSideControlsTheBattlefieldDialogAsk(victoryMessage, title);

            ResolveScenarioTracker tracker = new ResolveScenarioTracker(scenario, getCampaign(), control);
            tracker.setClient(new SimulatedClient(autoResolveConcludedEvent.getGame()));
            tracker.setEvent(autoResolveConcludedEvent);
            tracker.processGame();

            ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(getCampaign(),
                  campaignGUI.getFrame(),
                  true,
                  tracker);
            resolveDialog.setVisible(true);
            resolveDialog.dispose();

            if (resolveDialog.wasAborted()) {
                postAbortedAutoResolve(autoResolveConcludedEvent, scenario, tracker);
            } else {
                // If the autoresolve is not aborted, follow with the PostScenario Handler as normal
                PostScenarioDialogHandler.handle(campaignGUI, getCampaign(), scenario, tracker);
            }
        } catch (Exception ex) {
            LOGGER.error("Error during auto resolve concluded", ex);
        }
    }

    private void postAbortedAutoResolve(AutoResolveConcludedEvent autoResolveConcludedEvent, Scenario scenario,
          ResolveScenarioTracker tracker) {
        try {
            resetPersonsHits(tracker);
        } catch (NullPointerException ex) {
            LOGGER.error(ex,
                  "Error during auto resolve concluded, dumping stack trace and events, " +
                        "AtbScenario {}, AutoResolveConcludedEvent {}", scenario, autoResolveConcludedEvent);
            LOGGER.errorDialog(
                  I18n.getTextAt("AbortingResolveScenarioWizard",
                        Sentry.isEnabled() ? "errorMessage.withSentry" : "errorMessage.withoutSentry"),
                  I18n.getTextAt("AbortingResolveScenarioWizard",
                        "errorMessage.title"));
        }
    }

    private void resetPersonsHits(ResolveScenarioTracker tracker) {
        var peopleStatus = tracker.getPeopleStatus();
        Objects.requireNonNull(peopleStatus, "getPeopleStatus() returned null");
        Objects.requireNonNull(getCampaign(), "getCampaign() returned null");
        List<Throwable> errors = new ArrayList<>();
        for (var entry : peopleStatus.entrySet()) {
            try {
                Person person = getCampaign().getPerson(entry.getKey());
                Objects.requireNonNull(person, "getPerson() returned null for Person ID=" + entry.getKey() + ".");
                person.setHits(person.getHitsPrior());
            } catch (Throwable ex) {
                errors.add(ex);
            }
        }
        if (!errors.isEmpty()) {
            String errorMessage = errors.stream().map(Throwable::getMessage).collect(Collectors.joining("\n"));
            throw new NullPointerException(errorMessage);
        }
    }

    /*
     * Access methods for event bus.
     */
    public static void registerHandler(Object handler) {
        EVENT_BUS.register(handler);
    }

    public static boolean triggerEvent(MMEvent event) {
        return EVENT_BUS.trigger(event);
    }

    public static void unregisterHandler(Object handler) {
        EVENT_BUS.unregister(handler);
    }

    /**
     * TODO : This needs to be way more flexible, but it will do for now.
     */
    private void initEventHandlers() {
        EVENT_BUS.register(new XPHandler());

        StratconRulesManager srm = new StratconRulesManager();
        srm.startup();
        EVENT_BUS.register(srm);
    }

    private static void setLookAndFeel(String themeName) {
        final String theme = themeName.isBlank() || themeName.equals("UITheme") ? defaultTheme : themeName;

        Runnable runnable = () -> {
            try {
                UIManager.setLookAndFeel(theme);
                if (System.getProperty("os.name", "").startsWith("Mac OS X")) {
                    // Ensure OSX key bindings are used for copy, paste etc
                    addOSXKeyStrokes((InputMap) UIManager.get("EditorPane.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextField.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextPane.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextArea.focusInputMap"));
                }

                UIUtil.updateAfterUiChange();
                return;
            } catch (ClassNotFoundException |
                           InstantiationException |
                           IllegalAccessException |
                           UnsupportedLookAndFeelException e) {
                LOGGER.error(e, "setLookAndFeel() with exception {}", e.getMessage());
            }
            try {
                UIManager.setLookAndFeel(defaultTheme);
                if (System.getProperty("os.name", "").startsWith("Mac OS X")) {
                    // Ensure OSX key bindings are used for copy, paste etc
                    addOSXKeyStrokes((InputMap) UIManager.get("EditorPane.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextField.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextPane.focusInputMap"));
                    addOSXKeyStrokes((InputMap) UIManager.get("TextArea.focusInputMap"));
                }

                UIUtil.updateAfterUiChange();
            } catch (ClassNotFoundException |
                           InstantiationException |
                           IllegalAccessException |
                           UnsupportedLookAndFeelException e) {
                LOGGER.error(e, "setLookAndFeel()");
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    public static void updateGuiScaling() {
        System.setProperty("flatlaf.uiScale", Double.toString(GUIPreferences.getInstance().getGUIScale()));
        setLookAndFeel(selectedTheme.getValue());
    }

    private static class MekHqPropertyChangedListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource().equals(selectedTheme)) {
                setLookAndFeel((String) evt.getNewValue());
            }
        }
    }

    private static void addOSXKeyStrokes(InputMap inputMap) {
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK),
              DefaultEditorKit.selectAllAction);
    }
}
