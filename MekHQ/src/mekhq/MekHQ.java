/*
 * MekHQ.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq;

import io.sentry.Sentry;
import megamek.MMLoggingConstants;
import megamek.MegaMek;
import megamek.SuiteConstants;
import megamek.client.Client;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.preferences.SuitePreferences;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.gameConnectionDialogs.ConnectDialog;
import megamek.client.ui.swing.gameConnectionDialogs.HostDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.event.*;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;
import megameklab.MegaMekLab;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignController;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.autoresolve.Resolver;
import mekhq.campaign.autoresolve.acar.SimulatedClient;
import mekhq.campaign.autoresolve.acar.SimulationOptions;
import mekhq.campaign.autoresolve.event.AutoResolveConcludedEvent;
import mekhq.campaign.handler.PostScenarioDialogHandler;
import mekhq.campaign.handler.XPHandler;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.AutoResolveChanceDialog;
import mekhq.gui.dialog.ChooseMulFilesDialog;
import mekhq.gui.dialog.ResolveScenarioWizardDialog;
import mekhq.gui.dialog.helpDialogs.AutoResolveSimulationLogDialog;
import mekhq.gui.panels.StartupScreenPanel;
import mekhq.gui.preferences.StringPreference;
import mekhq.gui.utilities.ObservableString;
import mekhq.service.AutosaveService;
import mekhq.service.IAutosaveService;
import mekhq.utilities.Internationalization;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputFilter.Config;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;

/**
 * The main class of the application.
 */
public class MekHQ implements GameListener {
    private static final MMLogger logger = MMLogger.create(MekHQ.class);

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

    private IconPackage iconPackage = new IconPackage();

    private final IAutosaveService autosaveService;
    // endregion Variable Declarations
    private static final SanityInputFilter sanityInputFilter = new SanityInputFilter();

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

        initEventHandlers();
        // create a start-up frame and display it
        new StartupScreenPanel(this).getFrame().setVisible(true);
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

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private static void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekHQ.class);

            // TODO: complete integration of Suite Preferences, including GUIPreferences
            selectedTheme = new ObservableString("selectedTheme", GUIPreferences.UI_THEME);
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
            logger.error(ex, "Failed to set user preferences");
        }
    }

    public void exit(boolean includeSavePrompt) {
        if (includeSavePrompt) {
            int savePrompt = JOptionPane.showConfirmDialog(null,
                "Do you want to save the game before quitting MekHQ?",
                    "Save First?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
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

        // Configure Sentry with defaults. Although the client defaults to enabled, the
        // properties file is used to disable
        // it and additional configuration can be done inside of the sentry.properties
        // file. The defaults for everything else is set here.
        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setDsn("https://a05b2064798e2b8d46ac620b4497a072@sentry.tapenvy.us/10");
            options.setEnvironment("production");
            options.setTracesSampleRate(0.2);
            options.setDebug(true);
            options.setServerName("MegaMekClient");
            options.setRelease(SuiteConstants.VERSION.toString());
        });

        // First, create a global default exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            final String name = t.getClass().getName();
            final String message = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION, name);
            final String title = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION_TITLE, name);
            logger.error(t, message, title);
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
        logger.info(ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    public static void initializeLogging(final String originProject) {
        logger.info(getUnderlyingInformation(originProject));
    }

    /**
     * @param originProject the project that launched MekHQ
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
     * @return the campaigngui
     */
    public CampaignGUI getCampaigngui() {
        return campaignGUI;
    }

    /**
     * @param campaigngui the campaigngui to set
     */
    public void setCampaigngui(CampaignGUI campaigngui) {
        this.campaignGUI = campaigngui;
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
            logger.error(ex, "Failed to connect to server properly");
            return;
        }

        client.getGame().addGameListener(this);
        currentScenario = scenario;

        // Start the game thread
        gameThread = new GameThread(playerName, client, this, meks, scenario, false);
        gameThread.start();
    }

    /**
     * Start hosting a game.
     * This method is used to start hosting a game. It will create a new server and a client and connect to it.
     *
     * @param scenario                      The scenario to host
     * @param loadSavegame                  Whether to load a savegame
     * @param meks                          The units you want to use in the scenario
     */
    public void startHost(Scenario scenario, boolean loadSavegame, List<Unit> meks) {
        startHost(scenario, loadSavegame, meks, null);
    }

    /**
     * Start hosting a game.
     * This method is used to start hosting a game. It will create a new server and a client and connect to it.
     *
     * @param scenario                      The scenario to host
     * @param loadSavegame                  Whether to load a savegame
     * @param meks                          The units you want to use in the scenario
     * @param autoResolveBehaviorSettings   The auto resolve behavior settings to use if running an AtB scenario and auto resolve is wanted
     */
    public void startHost(Scenario scenario, boolean loadSavegame, List<Unit> meks, @Nullable BehaviorSettings autoResolveBehaviorSettings)
    {
        HostDialog hostDialog = new HostDialog(campaignGUI.getFrame(), getCampaign().getName());
        hostDialog.setVisible(true);

        if (!hostDialog.dataValidation("MegaMek.HostGameAlert.title")) {
            stopHost();
            return;
        }

        this.autosaveService.requestBeforeMissionAutosave(getCampaign());

        final String playerName = hostDialog.getPlayerName();
        final String password = hostDialog.getServerPass();
        final int port = hostDialog.getPort();
        final boolean register = hostDialog.isRegister();
        final String metaserver = register ? hostDialog.getMetaserver() : "";

        // Force cleanup of the current modal, since we are (possibly) about to display
        // a new one and MacOS
        // seems to struggle with that (see https://github.com/MegaMek/mekhq/issues/953)
        hostDialog.dispose();

        try {
            myServer = new Server(password, port, new TWGameManager(), register, metaserver);
            if (loadSavegame) {
                FileDialog f = new FileDialog(campaignGUI.getFrame(), "Load Savegame");
                f.setDirectory(System.getProperty("user.dir") + "/savegames");
                f.setVisible(true);
                if (null != f.getFile()) {
                    getMyServer().loadGame(new File(f.getDirectory(), f.getFile()));
                } else {
                    stopHost();
                    return; // exceptions as flow control? no thanks.
                }
            }
        } catch (FileNotFoundException ex) {
            // The dialog was cancelled or the file not found
            // Return to the UI
            stopHost();
            return;
        } catch (Exception ex) {
            logger.error(ex, "Failed to start up server");
            stopHost();
            return;
        }

        client = new Client(playerName, "127.0.0.1", port);

        client.getGame().addGameListener(this);
        currentScenario = scenario;

        // Start the game thread
        if (getCampaign().getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario)) {
            gameThread = new AtBGameThread(playerName, password, client, this, meks, (AtBScenario) scenario, autoResolveBehaviorSettings);
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
     * This method is called automatically when the megamek game is over.
     * @param gve
     */
    @Override
    public void gameVictory(PostGameResolution gve) {
        // Prevent double run
        if (gameThread.stopRequested()) {
            return;
        }

        try {
            boolean control = yourSideControlsTheBattlefieldDialogAsk(
                Internationalization.getText("ResolveDialog.control.message"),
                Internationalization.getText("ResolveDialog.control.title"));
            ResolveScenarioTracker tracker = new ResolveScenarioTracker(currentScenario, getCampaign(), control);
            tracker.setClient(gameThread.getClient());
            tracker.setEvent(gve);
            tracker.processGame();

            ResolveScenarioWizardDialog resolveDialog =
                new ResolveScenarioWizardDialog(campaignGUI.getCampaign(), campaignGUI.getFrame(),
                    true, tracker);
            resolveDialog.setVisible(true);

            if (resolveDialog.wasAborted()) {
                return;
            }

            PostScenarioDialogHandler.handle(
                campaignGUI, getCampaign(), (AtBScenario) currentScenario, tracker, control);

            gameThread.requestStop();
        } catch (Exception ex) {
            logger.error(ex, "gameVictory()");
        }
    }

    /**
     * This method is called when player wants to manually resolve the scenario providing MUL files.
     */
    public void resolveScenario(Scenario selectedScenario) {
        if (null == selectedScenario) {
            return;
        }
        boolean control = yourSideControlsTheBattlefieldDialogAsk(
            Internationalization.getText("ResolveDialog.control.message"),
            Internationalization.getText("ResolveDialog.control.title"));

        ResolveScenarioTracker tracker = new ResolveScenarioTracker(selectedScenario, getCampaign(), control);

        ChooseMulFilesDialog chooseFilesDialog = new ChooseMulFilesDialog(campaignGUI.getFrame(), true, tracker);
        chooseFilesDialog.setVisible(true);
        if (chooseFilesDialog.wasCancelled()) {
            return;
        }

        ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(getCampaign(),
            campaignGUI.getFrame(), true, tracker);
        resolveDialog.setVisible(true);

        if (resolveDialog.wasAborted()) {
            return;
        }

        PostScenarioDialogHandler.handle(
            campaignGUI, getCampaign(), (AtBScenario) selectedScenario, tracker, control);
    }

    private boolean yourSideControlsTheBattlefieldDialogAsk(String message, String title) {
        return JOptionPane.showConfirmDialog(campaignGUI.getFrame(),
            message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
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

    /**
     * This method is called when the player wants to auto resolve the scenario using ACAR method
     * @param units The list of player units involved in the scenario
     */
    public void startAutoResolve(AtBScenario scenario, List<Unit> units) {

        if (getCampaign().getCampaignOptions().isAutoResolveVictoryChanceEnabled()) {
            var proceed = AutoResolveChanceDialog
                .showSimulationProgressDialog(
                    getCampaigngui().getFrame(),
                    getCampaign().getCampaignOptions().getAutoResolveNumberOfScenarios(),
                    units,
                    scenario,
                    getCampaign()) == JOptionPane.YES_OPTION;
            if (!proceed) {
                return;
            }
        }

        var event = new Resolver(getCampaign(), units, scenario, new SimulationOptions(getCampaign().getGameOptions()))
            .resolveSimulation();

        var autoResolveBattleReport = new AutoResolveSimulationLogDialog(getCampaigngui().getFrame(), event.getLogFile());
        autoResolveBattleReport.setModal(true);
        autoResolveBattleReport.setVisible(true);

        autoResolveConcluded(event);
    }

    /**
     * This method is called when the auto resolve game is over.
     * @param autoResolveConcludedEvent The event that contains the results of the auto resolve game.
     */
    public void autoResolveConcluded(AutoResolveConcludedEvent autoResolveConcludedEvent) {
        try {
            String message = autoResolveConcludedEvent.controlledScenario() ?
                Internationalization.getText("AutoResolveDialog.message.victory") :
                Internationalization.getText("AutoResolveDialog.message.defeat");
            String title = autoResolveConcludedEvent.controlledScenario() ?
                Internationalization.getText("AutoResolveDialog.victory") :
                Internationalization.getText("AutoResolveDialog.defeat");
            boolean control = yourSideControlsTheBattlefieldDialogAsk(message, title);
            var scenario = autoResolveConcludedEvent.getScenario();

            ResolveScenarioTracker tracker = new ResolveScenarioTracker(scenario, getCampaign(), control);
            tracker.setClient(new SimulatedClient(autoResolveConcludedEvent.getGame()));
            tracker.setEvent(autoResolveConcludedEvent);
            tracker.processGame();

            ResolveScenarioWizardDialog resolveDialog =
                new ResolveScenarioWizardDialog(getCampaign(), campaignGUI.getFrame(),
                    true, tracker);
            resolveDialog.setVisible(true);
            if (resolveDialog.wasAborted()) {
                for (UUID personId : tracker.getPeopleStatus().keySet()) {
                    Person person = getCampaign().getPerson(personId);
                    person.setHits(person.getHitsPrior());
                }
                return;
            }
            PostScenarioDialogHandler.handle(
                campaignGUI, getCampaign(), scenario, tracker, autoResolveConcludedEvent.controlledScenario());
        } catch (Exception ex) {
            logger.error("Error during auto resolve concluded", ex);
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
        final String theme = themeName.isBlank() ? "com.formdev.flatlaf.FlatDarculaLaf" : themeName;

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
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                logger.error(e, "setLookAndFeel()");
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    public static void updateGuiScaling() {
        System.setProperty("flatlaf.uiScale", Double.toString(GUIPreferences.getInstance().getGUIScale()));
        setLookAndFeel(GUIPreferences.getInstance().getUITheme());
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
