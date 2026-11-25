/*
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static java.util.Arrays.sort;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.universe.warriorsAlmanac.WarriorsAlmanacEntry.buildAlmanacPartsData;
import static mekhq.campaign.universe.warriorsAlmanac.WarriorsAlmanacEntry.buildAlmanacUnitsData;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP_ABRIDGED;
import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.RawImagePanel;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.OptionsConstants;
import megamek.common.ui.AppIcons;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.release.ReleaseChannel;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.NewPlayerQuickstart;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.io.CustomUnitFileUUIDRepair;
import mekhq.campaign.market.PartsStore;
import mekhq.campaign.mission.contract.contractGeneration.RandomOperationNameGenerator;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.reputation.camOpsReputation.ForceReputationController;
import mekhq.campaign.roleplay.JournalRescue;
import mekhq.campaign.roleplay.RandomOracleGenerator;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import mekhq.campaign.universe.factionHints.WarAndPeaceProcessor;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.AbstractMHQDialogBasic;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.CampaignOptionsPresetPicker;
import mekhq.gui.utilities.EventDispatch;

public class DataLoadingDialog extends AbstractMHQDialogBasic implements PropertyChangeListener {
    private static final MMLogger LOGGER = MMLogger.create(DataLoadingDialog.class);

    // region Variable Declarations
    private final MekHQ application;
    private final File campaignFile;
    private final Task task;
    private RawImagePanel splash;
    private JProgressBar progressBar;
    private final boolean isInAppNewCampaign;
    private final boolean isNewPlayerQuickstart;
    private final Consumer<Campaign> completionHandler;

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(3151, 1, 1);

    // endregion Variable Declarations

    /**
     * Determines whether loading a campaign should prompt the player to upgrade it.
     *
     * <p>A campaign saved in an older release line (major, minor, and patch) always warrants the prompt. A newer
     * build of the same line normally does not, because it is a point release.</p>
     *
     * <p>The exception is a Core build opening a campaign of its own line that a Core build did not save. Core and
     * Experimental share one line's build numbers, and Experimental builds of a line are published before its first
     * Core build, so such a campaign comes from Experimental (or a development build) and goes through the same
     * upgrade as everyone else arriving in Core. Saves that predate release channels carry none, and no Core build
     * predates them, so they are treated the same way.</p>
     *
     * @param campaignVersion the version the campaign was saved in
     * @param campaignChannel the release channel of the build that saved the campaign, or {@code null} if none
     * @param currentVersion  the version currently running
     * @param currentChannel  the release channel of the running build, or {@code null} for a development build
     *
     * @return {@code true} if the upgrade prompt should be shown
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isUpgradePromptRequired(Version campaignVersion, @Nullable ReleaseChannel campaignChannel,
          Version currentVersion, @Nullable ReleaseChannel currentChannel) {
        Version campaignRelease = new Version(campaignVersion.getMajor(),
              campaignVersion.getMinor(),
              campaignVersion.getPatch());
        Version currentRelease = new Version(currentVersion.getMajor(),
              currentVersion.getMinor(),
              currentVersion.getPatch());
        if (campaignRelease.isLowerThan(currentRelease)) {
            return true;
        }
        return (currentChannel == ReleaseChannel.CORE)
              && (campaignRelease.compareTo(currentRelease) == 0)
              && (campaignChannel != ReleaseChannel.CORE);
    }

    // region Constructors
    public DataLoadingDialog(final JFrame frame, final MekHQ application, final @Nullable File campaignFile,
          Consumer<Campaign> completionHandler) {
        this(frame, application, campaignFile, false, completionHandler);
    }

    public DataLoadingDialog(final JFrame frame, final MekHQ application, final @Nullable File campaignFile,
          final boolean isInAppNewCampaign, Consumer<Campaign> completionHandler) {
        this(frame, application, campaignFile, isInAppNewCampaign, false, completionHandler);
    }

    /**
     * Starts loading the New Player Quickstart: a new campaign set up and populated without asking the player anything.
     * See {@link NewPlayerQuickstart}.
     *
     * @param frame             the frame hidden while loading
     * @param application       the running application
     * @param completionHandler receives the finished campaign, or {@code null} if loading failed
     *
     * @return the loading dialog
     */
    public static DataLoadingDialog newPlayerQuickstart(final JFrame frame, final MekHQ application,
          final Consumer<Campaign> completionHandler) {
        return new DataLoadingDialog(frame, application, null, false, true, completionHandler);
    }

    private DataLoadingDialog(final JFrame frame, final MekHQ application, final @Nullable File campaignFile,
          final boolean isInAppNewCampaign, final boolean isNewPlayerQuickstart,
          final Consumer<Campaign> completionHandler) {
        // Every caller hides its frame before loading, and Windows leaves windows owned by a hidden frame out of the
        // taskbar and Alt+Tab, so the loading screen has no owner and carries the MekHQ icon itself.
        super((JDialog) null, frame, false, "DataLoadingDialog", "DataLoadingDialog.title");
        AppIcons.applyToWindow(this, "mekhq");
        this.application = application;
        this.campaignFile = campaignFile;
        this.isInAppNewCampaign = isInAppNewCampaign;
        this.isNewPlayerQuickstart = isNewPlayerQuickstart;
        this.completionHandler = completionHandler;
        this.task = new Task(this);
        getTask().addPropertyChangeListener(this);
        initialize();
        getTask().execute();
    }
    // endregion Constructors

    // region Getters/Setters
    public MekHQ getApplication() {
        return application;
    }

    public @Nullable File getCampaignFile() {
        return campaignFile;
    }

    public Task getTask() {
        return task;
    }

    public RawImagePanel getSplash() {
        return splash;
    }

    public void setSplash(final RawImagePanel splash) {
        this.splash = splash;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(final JProgressBar progressBar) {
        this.progressBar = progressBar;
    }
    // endregion Getters/Setters

    // region Initialization
    @Override
    protected void initialize() {
        setUndecorated(true);
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        add(createProgressBar(), BorderLayout.PAGE_END);
        finalizeInitialization();
    }

    @Override
    protected Container createCenterPane() {
        setSplash(UIUtil.createSplashComponent(getApplication().getIconPackage().getLoadingScreenImages(), this));
        return getSplash();
    }

    private JProgressBar createProgressBar() {
        setProgressBar(new JProgressBar(0, 8));
        getProgressBar().setString(resources.getString("loadingBaseData.text"));
        getProgressBar().setValue(0);
        getProgressBar().setStringPainted(true);
        getProgressBar().setVisible(true);
        return getProgressBar();
    }

    @Override
    protected void finalizeInitialization() {
        setPreferredSize(getSplash().getPreferredSize());
        setSize(getSplash().getPreferredSize());
        pack();
        fitAndCenter();
        setVisible(true);
    }
    // endregion Initialization

    // region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        int progress = task.getProgress();
        progressBar.setValue(progress);

        // If you add a new tier you MUST increase the levels of the progressBar
        switch (progress) {
            case 0:
                progressBar.setString(resources.getString("loadingBaseData.text"));
                break;
            case 1:
                progressBar.setString(resources.getString("loadingFactionData.text"));
                break;
            case 2:
                progressBar.setString(resources.getString("loadingNameData.text"));
                break;
            case 3:
                progressBar.setString(resources.getString("loadingPlanetaryData.text"));
                break;
            case 4:
                progressBar.setString(resources.getString("loadingImageData.text"));
                break;
            case 5:
                progressBar.setString(resources.getString("loadingUnits.text"));
                break;
            case 6:
                progressBar.setString(resources.getString((getCampaignFile() == null) ?
                                                                "initializingNewCampaign.text" :
                                                                "loadingCampaign.text"));
                break;
            case 7:
                final String applyingKey;
                if (isNewPlayerQuickstart) {
                    applyingKey = "generatingNewPlayerCompany.text";
                } else if (getCampaignFile() == null) {
                    applyingKey = "applyingNewCampaign.text";
                } else {
                    applyingKey = "applyingLoadedCampaign.text";
                }
                progressBar.setString(resources.getString(applyingKey));
                break;
            case 8:
                progressBar.setString(resources.getString("performingFinalSteps.text"));
                break;
            default:
                progressBar.setString(resources.getString("Error.text"));
                break;
        }

        getAccessibleContext().setAccessibleDescription(String.format(resources.getString(
              "DataLoadingDialog.progress.accessibleDescription"), progressBar.getString()));
    }
    // endregion PropertyChangeListener

    /**
     * Main task. This is executed in a background thread.
     */
    public class Task extends SwingWorker<Campaign, Campaign> {
        JDialog dialog;

        public Task(JDialog dialog) {
            this.dialog = dialog;
        }

        /**
         * This uses the following stages of loading:
         * <ol>
         *     <li>Basics</li>
         *     <li>Factions</li>
         *     <li>Names</li>
         *     <li>Planetary Systems</li>
         *     <li>Portraits, Camouflage, Mek Tileset, Formation Icons, Awards</li>
         *     <li>Units</li>
         *     <li>New Campaign / Campaign Loading</li>
         *     <li>Campaign Application</li>
         * </ol>
         *
         * @return The loaded campaign
         *
         * @throws Exception if anything goes wrong
         */
        @Override
        public Campaign doInBackground() throws Exception {
            // region progress 0
            setProgress(0);
            CurrencyManager.getInstance().loadCurrencies();
            Eras.initializeEras();
            FinancialInstitutions.initializeFinancialInstitutions();
            InjuryTypes.registerAll(); // TODO : Isolate into an actual module
            Ranks.initializeRankSystems();
            SkillType.initializeTypes();
            sort(SkillType.getSkillList()); // sort all skills alphabetically
            SpecialAbility.initializeSPA(false);
            AtBScenarioModifier.initializeScenarioModifiers(false);
            // endregion Progress 0

            // region progress 1
            setProgress(1);
            Factions.setInstance(Factions.loadDefault(false));
            // endregion Progress 1

            // region progress 2
            setProgress(2);
            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            RandomCompanyNameGenerator.getInstance();
            RandomOracleGenerator.getInstance();
            RandomOperationNameGenerator.getInstance();
            Bloodname.loadBloodnameData();
            // endregion Progress 2

            // region progress 3
            setProgress(3);
            Systems.initializeDefaultSystems();
            // endregion Progress 3

            // region progress 4
            setProgress(4);
            MHQStaticDirectoryManager.initialize();
            // endregion Progress 4

            // region progress 5
            setProgress(5);
            while (!MekSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            CustomUnitFileUUIDRepair.repairCustomUnitFiles();
            // endregion Progress 5

            setProgress(6);
            final Campaign campaign;
            boolean isNewCampaign = false;
            if (getCampaignFile() == null) {
                // region progress 6
                LOGGER.info("Starting a new campaign");
                campaign = CampaignFactory.createCampaign();
                isNewCampaign = true;

                if (isNewPlayerQuickstart) {
                    NewPlayerQuickstart.configure(campaign);
                    MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
                } else if (!configureNewCampaign(campaign)) {
                    return null;
                }

                // initialize reputation
                ForceReputationController reputationController = new ForceReputationController();
                reputationController.initializeReputation(campaign);
                campaign.getPlayerForce().setCamOpsReputation(reputationController);

                // initialize starting faction standings
                CampaignOptions campaignOptions = campaign.getCampaignOptions();
                if (campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)) {
                    FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();
                    String report = factionStandings.updateClimateRegard(campaign.getPlayerForce().getFaction(),
                          campaign.getLocalDate(), campaignOptions.get(CampaignOption.REGARD_MULTIPLIER),
                          true);
                    campaign.addReport(POLITICS, report);
                }
                // endregion Progress 6

                // region progress 7
                setProgress(7);
                campaign.beginReport("<b>" +
                                           MekHQ.getMHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) +
                                           "</b>");

                // Setup Personnel Modules
                final AbstractMarriage marriage = campaignOptions
                                                        .get(CampaignOption.RANDOM_MARRIAGE_METHOD)
                                                        .getMethod(campaignOptions);
                campaign.getPlayerForce().getHumanResources().setMarriage(marriage);
                final AbstractDivorce divorce = campaignOptions
                                                      .get(CampaignOption.RANDOM_DIVORCE_METHOD)
                                                      .getMethod(campaignOptions);
                campaign.getPlayerForce().getHumanResources().setDivorce(divorce);
                final AbstractProcreation procreation = campaignOptions
                                                              .get(CampaignOption.RANDOM_PROCREATION_METHOD)
                                                              .getMethod(campaignOptions);
                campaign.getPlayerForce().getHumanResources().setProcreation(procreation);

                // Setup Markets
                campaign.getPlayerForce().getHumanResources().refreshApplicants(campaign, true);
                showRarePersonnelDialog(campaign, true);

                if (!campaignOptions.get(CampaignOption.UNIT_MARKET_METHOD).isNone()) {
                    campaign.setUnitMarket(campaignOptions.get(CampaignOption.UNIT_MARKET_METHOD).getUnitMarket());
                    campaign.getUnitMarket().generateUnitOffers(campaign);
                }

                // News
                campaign.reloadNews();
                campaign.readNews();

                // AtB
                if (campaignOptions.isUseStratCon()) {
                    campaign.initAtB(true);
                }

                // Turnover
                campaign.initTurnover();

                if (isNewPlayerQuickstart) {
                    // The mercenary organization's welcome is a dialog over the campaign window, so it waits until
                    // the campaign is open; see NewPlayerQuickstart#welcome
                    NewPlayerQuickstart.generateCommand(campaign, null);
                }
                // endregion Progress 7
            } else {
                // region progress 6
                LOGGER.info("Loading campaign file from XML file {}", getCampaignFile());

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(getCampaignFile())) {
                    campaign = CampaignFactory.newInstance(getApplication()).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
                // endregion Progress 6

                // region progress 7
                setProgress(7);

                unassignCrewFromUnsupportedUnits(campaign.getUnits());

                // <50.10 compatibility handler
                final Version campaignVersion = campaign.getVersion();
                if (campaignVersion.isLowerThan(new Version("0.50.10"))) {
                    new WarAndPeaceProcessor(campaign, true);
                }

                // Campaign upgrading
                // This needs to be the final stage in Progress 7 as otherwise the display of any confirmation
                // dialogs will get 'stuck' behind other dialogs
                if (isUpgradePromptRequired(campaignVersion, campaign.getVersionChannel(), MHQConstants.VERSION,
                      Version.getBuildChannel())) {
                    handleCampaignUpgrading(application, campaign, getCampaignFile());
                }
                // endregion Progress 7
            }

            // region progress 8
            setProgress(8);

            PartsStore partsStore = campaign.getPartsStore();
            // Parts carry separate Inner Sphere and Clan tech dates; use the player force's perspective.
            boolean isClanForce = campaign.getPlayerForce().isClanForce();
            campaign.setPartsAlmanac(buildAlmanacPartsData(partsStore, isClanForce));
            campaign.setUnitsAlmanac(buildAlmanacUnitsData());

            if (isNewCampaign) {
                new WarAndPeaceProcessor(campaign, true);
            }
            // endregion Progress 8

            return campaign;
        }

        /**
         * Lets the player set up a new campaign: pick a preset, then review it in Campaign Options. Runs on the worker
         * thread, showing each dialog on the Event Dispatch Thread.
         *
         * @param campaign the new campaign to set up
         *
         * @return {@code true} once the campaign is set up, or {@code false} if the player canceled
         */
        private boolean configureNewCampaign(final Campaign campaign) {
            // Campaign Preset
            // This method runs in a worker thread; build and show the picker on the EDT
            final CampaignOptionsPresetPicker campaignOptionsPresetPicker;
            try {
                campaignOptionsPresetPicker = EventDispatch.callAndWait(
                      () -> new CampaignOptionsPresetPicker(getFrame(), true));
            } catch (InterruptedException exception) {
                return false;
            }
            CampaignPreset preset;
            boolean isSelect;

            if (campaignOptionsPresetPicker.wasCanceled()) {
                if (isInAppNewCampaign) {
                    application.exit(false);
                }
                return false;
            }

            preset = campaignOptionsPresetPicker.getSelectedPreset();
            isSelect = campaignOptionsPresetPicker.wasApplied();

            // MegaMek Options
            if ((preset != null) && (preset.getGameOptions() != null)) {
                campaign.setGameOptions(preset.getGameOptions());
            }

            // Campaign Options
            // This needs to be before we trigger the customize preset dialog
            campaign.setLocalDate(DEFAULT_START_DATE);
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());

            CampaignOptionsDialogMode mode = isSelect ? STARTUP_ABRIDGED : STARTUP;
            // This method runs in a worker thread, but the construction of the UI for Campaign Options must run on
            // the EDT. A UI-construction failure propagates through the worker so done() reports it via its
            // ExecutionException handling, rather than returning false (which looks like a normal cancel).
            final boolean isOptionsCanceled;
            try {
                isOptionsCanceled = EventDispatch.callAndWait(
                      () -> showStartupCampaignOptionsDialog(campaign, preset, mode));
            } catch (InterruptedException exception) {
                return false;
            } catch (RuntimeException | Error exception) {
                LOGGER.error("Failed to display the Campaign Options dialog", exception);
                throw exception;
            }
            if (isOptionsCanceled) {
                return false;
            }

            // Starting planet: the campaign options General page resolves and sets the starting system during
            // apply. A preset with a pinned planet overrides that choice.
            if ((preset != null) && (preset.getPlanet() != null)) {
                campaign.setStartingSystem(preset.getPlanet());
            }

            // GM Mode
            campaign.setGMMode((preset == null) || preset.isGM());
            return true;
        }

        /**
         * Handles the upgrade process for a campaign in a thread-safe and blocking manner.
         *
         * <p>This method shows the campaign upgrade console for the specified {@link Campaign} and blocks until the
         * player's choice has been applied. This ensures that campaign loading or other interactions do not proceed
         * while the campaign data may be in an inconsistent, mid-upgrade state, which can prevent a variety of random
         * and challenging-to-debug errors.</p>
         *
         * <p><b>Note:</b> This method should not be called from the Event Dispatch Thread (EDT), as it will block
         * the thread until the upgrade is finished.</p>
         *
         * @param app      the application context
         * @param campaign the {@link Campaign} instance to be upgraded
         * @param saveFile the save the campaign was loaded from, offered to the player for backup; may be {@code null}
         *
         * @author Illiani
         * @since 0.50.07
         */
        private static void handleCampaignUpgrading(MekHQ app, Campaign campaign, @Nullable File saveFile) {
            CampaignUpgradeDialog.campaignUpgradeDialog(app, campaign, saveFile);
        }

        /**
         * Builds and shows the modal Campaign Options dialog for a brand-new campaign, then reports whether the user
         * canceled it.
         *
         * <p>Must run on the Event Dispatch Thread (it is invoked through {@link EventDispatch#callAndWait} from
         * the worker thread) because it constructs the dialog's entire Swing component tree.</p>
         *
         * @param campaign the new campaign being configured
         * @param preset   the preset to seed the dialog with, or {@code null}
         * @param mode     the dialog mode (abridged or full startup)
         *
         * @return {@code true} if the user canceled the dialog; {@code false} if the settings were applied
         */
        private boolean showStartupCampaignOptionsDialog(final Campaign campaign, final @Nullable CampaignPreset preset,
              final CampaignOptionsDialogMode mode) {
            final CampaignOptionsDialog optionsDialog = CampaignOptionsDialog.create(getFrame(), campaign, preset, mode);
            setVisible(false); // cede visibility to `optionsDialog`
            optionsDialog.setVisible(true);
            if (optionsDialog.wasCanceled()) {
                return true;
            }
            setVisible(true); // restore loader visibility
            return false;
        }

        /**
         * Unassigns the crew from unsupported units in the given collection of units.
         *
         * <p>This method iterates through the provided {@link Collection} of {@link Unit} objects
         * and checks if each unit's associated {@link Entity} is of an unsupported type.
         *
         * <p>If the entity is {@code null}, it is skipped. For unsupported unit types, the unit's
         * crew is cleared using {@link Unit#clearCrew()}.</p>
         *
         * @param units The {@link Collection} of {@link Unit} instances to process. Must not be {@code null}.
         */
        private static void showRarePersonnelDialog(Campaign campaign, boolean isCampaignStart) {
            if (!campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket().getHasRarePersonnel()) {
                return;
            }

            StringBuilder oocReport = new StringBuilder(
                  campaign.getResources().getString("personnelMarket.rareProfession.outOfCharacter"));
            for (PersonnelRole profession : campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket().getRareProfessions()) {
                oocReport.append("<p>- ").append(profession.getLabel(campaign.getPlayerForce().isClanForce())).append("</p>");
            }

            List<String> buttons = new ArrayList<>();
            buttons.add(campaign.getResources().getString("personnelMarket.rareProfession.button.later"));
            buttons.add(campaign.getResources().getString("personnelMarket.rareProfession.button.decline"));
            if (!isCampaignStart) {
                buttons.add(campaign.getResources().getString("personnelMarket.rareProfession.button.immediate"));
            }

            ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
                  campaign.getPlayerForce()
                        .getHumanResources()
                        .getSeniorAdminPerson(campaign.getCampaignOptions(),
                              campaign.getPlayerForce().isClanForce(),
                              campaign.getLocalDate()),
                  null,
                  campaign.getResources().getString("personnelMarket.rareProfession.inCharacter"),
                  buttons,
                  oocReport.toString(),
                  null,
                  true);

            if (dialog.getDialogChoice() == 2) {
                campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket().showPersonnelMarketDialog();
            }
        }

        private void unassignCrewFromUnsupportedUnits(Collection<Unit> units) {
            for (Unit unit : units) {
                Entity entity = unit.getEntity();

                if (entity == null) {
                    continue;
                }

                if (isUnsupportedEntity(entity)) {
                    unit.clearCrew();
                }
            }
        }

        /**
         * When a save fails to load, saves its Oracle journal next to it so the player's story isn't lost, and tells
         * them where it is. The save is read again on a background thread so the interface stays responsive.
         */
        private void rescueJournal() {
            final File save = getCampaignFile();
            if (save == null) {
                return;
            }
            final Thread rescuer = new Thread(() -> {
                try {
                    final File rescued = JournalRescue.rescue(save,
                          date -> MekHQ.getMHQOptions().getDisplayFormattedDate(date));
                    if (rescued != null) {
                        SwingUtilities.invokeLater(() -> showStartupMessage(
                              getFormattedTextAt("mekhq.resources.Roleplay", "JournalRescue.saved",
                                    rescued.getAbsolutePath()),
                              getTextAt("mekhq.resources.Roleplay", "JournalRescue.savedTitle"),
                              JOptionPane.INFORMATION_MESSAGE));
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to rescue the journal from {}", save, throwable);
                }
            }, "Journal rescue");
            rescuer.setDaemon(true);
            rescuer.start();
        }

        /**
         * Shows a message about loading a campaign. The dialog has no owner window and carries the MekHQ icon so it
         * appears in the Windows taskbar and Alt+Tab even while the MekHQ window is hidden. A {@code null} parent would
         * instead attach it to Swing's hidden shared frame, which leaves it out of both.
         *
         * @param message     the message to show
         * @param title       the dialog title
         * @param messageType the {@link JOptionPane} message type
         */
        private static void showStartupMessage(String message, String title, int messageType) {
            final JDialog dialog = new JOptionPane(message, messageType).createDialog(title);
            AppIcons.applyToWindow(dialog, "mekhq");
            dialog.setVisible(true);
            dialog.dispose();
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            // The loader has no owner window to clean it up, so release it rather than just hiding it
            dispose();
            try {
                Campaign campaign = get();
                completionHandler.accept(campaign);
            } catch (InterruptedException | CancellationException ignored) {
                completionHandler.accept(null);
            } catch (ExecutionException ex) {
                LOGGER.error("", ex);
                if (ex.getCause() instanceof NullEntityException) {
                    showStartupMessage(
                          String.format(resources.getString("DataLoadingDialog.NullEntityException.text"),
                                ex.getCause().getMessage()),
                          resources.getString("DataLoadingDialog.NullEntityException.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof NullPointerException) {
                    showStartupMessage(
                          String.format(resources.getString("DataLoadingDialog.NullPointerException.text"),
                                ex.getCause().getMessage()),
                          resources.getString("DataLoadingDialog.NullPointerException.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof OutOfMemoryError) {
                    showStartupMessage(
                          resources.getString("DataLoadingDialog.OutOfMemoryError.text"),
                          resources.getString("DataLoadingDialog.OutOfMemoryError.title"),
                          JOptionPane.ERROR_MESSAGE);
                } else {
                    showStartupMessage(
                          resources.getString("DataLoadingDialog.ExecutionException.text"),
                          resources.getString("DataLoadingDialog.ExecutionException.title"),
                          JOptionPane.ERROR_MESSAGE);
                }
                try {
                    // After the error, so the player learns the save failed before being told the journal is safe.
                    // Out of memory, reading the save again would only fail the same way.
                    if (!(ex.getCause() instanceof OutOfMemoryError)) {
                        rescueJournal();
                    }
                } finally {
                    completionHandler.accept(null);
                }
            }
        }
    }
}
