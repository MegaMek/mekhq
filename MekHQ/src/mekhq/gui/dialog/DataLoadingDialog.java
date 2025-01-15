/*
 * Copyright (c) 2009-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.CampaignPreset;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryArcStub;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import mekhq.gui.baseComponents.AbstractMHQDialogBasic;
import mekhq.gui.campaignOptions.CampaignOptionsDialog;
import mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode;
import mekhq.gui.campaignOptions.SelectPresetDialog;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.ABRIDGED;
import static mekhq.gui.campaignOptions.CampaignOptionsDialog.CampaignOptionsDialogMode.STARTUP;
import static mekhq.gui.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CANCELLED;
import static mekhq.gui.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CUSTOMIZE;
import static mekhq.gui.campaignOptions.SelectPresetDialog.PRESET_SELECTION_SELECT;

public class DataLoadingDialog extends AbstractMHQDialogBasic implements PropertyChangeListener {
    private static final MMLogger logger = MMLogger.create(DataLoadingDialog.class);

    // region Variable Declarations
    private final MekHQ application;
    private final File campaignFile;
    private final Task task;
    private JLabel splash;
    private JProgressBar progressBar;
    private StoryArcStub storyArcStub;
    private boolean isInAppNewCampaign;

    private final LocalDate DEFAULT_START_DATE = LocalDate.of(3051, 1, 1);

    // endregion Variable Declarations

    // region Constructors
    public DataLoadingDialog(final JFrame frame, final MekHQ application,
            final @Nullable File campaignFile) {
        this(frame, application, campaignFile, null, false);
    }

    public DataLoadingDialog(final JFrame frame, final MekHQ application,
            final @Nullable File campaignFile, StoryArcStub stub, final boolean isInAppNewCampaign) {
        super(frame, "DataLoadingDialog", "DataLoadingDialog.title");
        this.application = application;
        this.campaignFile = campaignFile;
        this.storyArcStub = stub;
        this.isInAppNewCampaign = isInAppNewCampaign;
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

    public JLabel getSplash() {
        return splash;
    }

    public void setSplash(final JLabel splash) {
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
        setSplash(UIUtil.createSplashComponent(
                getApplication().getIconPackage().getLoadingScreenImages(), getFrame()));
        return getSplash();
    }

    private JProgressBar createProgressBar() {
        setProgressBar(new JProgressBar(0, 7));
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
        getFrame().setVisible(false);
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
                progressBar.setString(resources.getString(
                        (getCampaignFile() == null) ? "initializingNewCampaign.text" : "loadingCampaign.text"));
                break;
            case 7:
                progressBar.setString(resources.getString(
                        (getCampaignFile() == null) ? "applyingNewCampaign.text" : "applyingLoadedCampaign.text"));
                break;
            default:
                progressBar.setString(resources.getString("Error.text"));
                break;
        }

        getAccessibleContext().setAccessibleDescription(String.format(
                resources.getString("DataLoadingDialog.progress.accessibleDescription"),
                progressBar.getString()));
    }
    // endregion PropertyChangeListener

    /**
     * Main task. This is executed in a background thread.
     */
    private class Task extends SwingWorker<Campaign, Campaign> {
        JDialog dialog;

        public Task(JDialog dialog) {
            this.dialog = dialog;
        }

        /**
         * This uses the following stages of loading:
         * 0 : Basics
         * 1 : Factions
         * 2 : Names
         * 3 : Planetary Systems
         * 4 : Portraits, Camouflage, Mek Tileset, Force Icons, Awards
         * 5 : Units
         * 6 : New Campaign / Campaign Loading
         * 7 : Campaign Application
         *
         * @return The loaded campaign
         * @throws Exception if anything goes wrong
         */
        @Override
        public Campaign doInBackground() throws Exception {
            // region Progress 0
            setProgress(0);
            CurrencyManager.getInstance().loadCurrencies();
            Eras.initializeEras();
            FinancialInstitutions.initializeFinancialInstitutions();
            InjuryTypes.registerAll(); // TODO : Isolate into an actual module
            Ranks.initializeRankSystems();
            RATManager.populateCollectionNames();
            SkillType.initializeTypes();
            SpecialAbility.initializeSPA();
            // endregion Progress 0

            // region Progress 1
            setProgress(1);
            Factions.setInstance(Factions.loadDefault());
            // endregion Progress 1

            // region Progress 2
            setProgress(2);
            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            RandomCompanyNameGenerator.getInstance();
            Bloodname.loadBloodnameData();
            // endregion Progress 2

            // region Progress 3
            setProgress(3);
            Systems.setInstance(Systems.loadDefault());
            // endregion Progress 3

            // region Progress 4
            setProgress(4);
            MHQStaticDirectoryManager.initialize();
            // endregion Progress 4

            // region Progress 5
            setProgress(5);
            while (!MekSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            // endregion Progress 5

            setProgress(6);
            final Campaign campaign;
            if (getCampaignFile() == null) {
                // region Progress 6
                logger.info("Starting a new campaign");
                campaign = new Campaign();

                // Campaign Preset
                final SelectPresetDialog presetSelectionDialog =
                    new SelectPresetDialog(getFrame(), true, true);
                CampaignPreset preset;
                boolean isSelect = false;

                switch (presetSelectionDialog.getReturnState()) {
                    case PRESET_SELECTION_CANCELLED -> {
                        if (isInAppNewCampaign) {
                            application.exit(false);
                        }
                        return null;
                    }
                    case PRESET_SELECTION_SELECT -> {
                        preset = presetSelectionDialog.getSelectedPreset();
                        isSelect = true;
                    }
                    case PRESET_SELECTION_CUSTOMIZE -> preset = presetSelectionDialog.getSelectedPreset();
                    default -> throw new IllegalStateException("Unexpected value in mekhq/gui/dialog/DataLoadingDialog.java/Step 6: "
                        + presetSelectionDialog.getReturnState());
                }

                // MegaMek Options
                if ((preset != null) && (preset.getGameOptions() != null)) {
                    campaign.setGameOptions(preset.getGameOptions());
                }

                // Campaign Options
                // This needs to be before we trigger the customize preset dialog
                campaign.setLocalDate(DEFAULT_START_DATE);
                campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());
                campaign.setStartingSystem((preset == null) ? null : preset.getPlanet());

                CampaignOptionsDialogMode mode = isSelect ? ABRIDGED : STARTUP;
                CampaignOptionsDialog optionsDialog =
                    new CampaignOptionsDialog(getFrame(), campaign, preset, mode);
                setVisible(false); // cede visibility to `optionsDialog`
                optionsDialog.setVisible(true);
                if (optionsDialog.wasCanceled()) {
                    return null;
                } else {
                    setVisible(true); // restore loader visibility
                }

                // initialize reputation
                ReputationController reputationController = new ReputationController();
                reputationController.initializeReputation(campaign);
                campaign.setReputation(reputationController);
                // endregion Progress 6

                // region Progress 7
                setProgress(7);
                campaign.beginReport(
                        "<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) + "</b>");

                // Setup Personnel Modules
                campaign.setMarriage(campaign.getCampaignOptions().getRandomMarriageMethod()
                        .getMethod(campaign.getCampaignOptions()));
                campaign.setDivorce(campaign.getCampaignOptions().getRandomDivorceMethod()
                        .getMethod(campaign.getCampaignOptions()));
                campaign.setProcreation(campaign.getCampaignOptions().getRandomProcreationMethod()
                        .getMethod(campaign.getCampaignOptions()));

                // Setup Markets
                campaign.getPersonnelMarket().generatePersonnelForDay(campaign);
                ContractMarketMethod contractMarketMethod = campaign.getCampaignOptions().getContractMarketMethod();
                campaign.setContractMarket(contractMarketMethod.getContractMarket());
                if (!contractMarketMethod.isNone()) {
                    campaign.getContractMarket().generateContractOffers(campaign, true);
                }
                if (!campaign.getCampaignOptions().getUnitMarketMethod().isNone()) {
                    campaign.setUnitMarket(campaign.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
                    campaign.getUnitMarket().generateUnitOffers(campaign);
                }

                // News
                campaign.reloadNews();
                campaign.readNews();

                // GM Mode
                campaign.setGMMode((preset == null) || preset.isGM());

                // AtB
                if (campaign.getCampaignOptions().isUseAtB()) {
                    campaign.initAtB(true);
                }

                // Turnover
                campaign.initTurnover();
                // endregion Progress 7
            } else {
                // region Progress 6
                logger.info(String.format("Loading campaign file from XML file %s", getCampaignFile()));

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(getCampaignFile())) {
                    campaign = CampaignFactory.newInstance(getApplication()).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
                // endregion Progress 6

                // region Progress 7
                setProgress(7);
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));

                // this is to handle pre-50.0 campaigns
                if (campaign.getReputation() == null) {
                    ReputationController reputationController = new ReputationController();
                    reputationController.initializeReputation(campaign);
                    campaign.setReputation(reputationController);
                }

                sellUnsupportedUnits(campaign);
                // endregion Progress 7
            }
            campaign.setApp(getApplication());
            return campaign;
        }



        /**
         * Sells unsupported units.
         * <p>
         * This method checks all units in the specified campaign and determines if they are unsupported.
         * Currently, only gun emplacements are identified as unsupported and are sold upon campaign load.
         * If unsupported units are sold, a notification is displayed to the user detailing the sold units,
         * including their names and IDs. Additionally, personnel assigned to these units are
         * automatically unassigned.
         * </p>
         *
         * @param retVal The campaign being checked for unsupported units. This includes the unit list
         *              and the quartermaster who handles the selling process.
         */
        private void sellUnsupportedUnits(Campaign retVal) {
            List<Unit> soldUnits = new ArrayList<>();
            for (Unit unit : retVal.getUnits()) {
                Entity entity = unit.getEntity();

                if (entity == null) {
                    continue;
                }

                // We don't support Gun Emplacements in mhq.
                // If the user has somehow acquired one, it is sold on load.
                if (entity instanceof GunEmplacement) {
                    soldUnits.add(unit);
                }
            }

            if (!soldUnits.isEmpty()) {
                StringBuilder message = new StringBuilder(resources.getString("unsupportedUnits.body"));

                for (Unit soldUnit : soldUnits) {
                    retVal.getQuartermaster().sellUnit(soldUnit);
                    message.append("- ").append(soldUnit.getName()).append("<br>");
                }

                JOptionPane.showMessageDialog(null,
                    String.format("<html>%s</html>", message),
                    resources.getString("unsupportedUnits.title"),
                    JOptionPane.WARNING_MESSAGE);
            }
        }

        /**
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Campaign campaign;
            try {
                campaign = get();
            } catch (InterruptedException | CancellationException ignored) {
                campaign = null;
            } catch (ExecutionException ex) {
                logger.error("", ex);
                if (ex.getCause() instanceof NullEntityException) {
                    JOptionPane.showMessageDialog(null,
                            String.format(resources.getString("DataLoadingDialog.NullEntityException.text"),
                                    ex.getCause().getMessage()),
                            resources.getString("DataLoadingDialog.NullEntityException.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof NullPointerException) {
                    JOptionPane.showMessageDialog(null,
                            String.format(resources.getString("DataLoadingDialog.NullPointerException.text"),
                                    ex.getCause().getMessage()),
                            resources.getString("DataLoadingDialog.NullPointerException.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else if (ex.getCause() instanceof OutOfMemoryError) {
                    JOptionPane.showMessageDialog(null,
                            resources.getString("DataLoadingDialog.OutOfMemoryError.text"),
                            resources.getString("DataLoadingDialog.OutOfMemoryError.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            resources.getString("DataLoadingDialog.ExecutionException.text"),
                            resources.getString("DataLoadingDialog.ExecutionException.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
                campaign = null;
            }

            setVisible(false);
            if (campaign != null) {
                getApplication().setCampaign(campaign);
                getApplication().getCampaignController().setHost(campaign.getId());
                getApplication().showNewView();
                getFrame().dispose();
                if (null != storyArcStub) {
                    StoryArc storyArc = storyArcStub.loadStoryArc(campaign);
                    if (null != storyArc) {
                        campaign.useStoryArc(storyArc, true);
                    }
                }
            } else {
                cancel(true);
                getFrame().setVisible(true);
            }
        }
    }
}
