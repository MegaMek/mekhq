/*
 * Copyright (c) 2009-2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.MechSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryArcStub;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import mekhq.gui.baseComponents.AbstractMHQDialog;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class DataLoadingDialog extends AbstractMHQDialog implements PropertyChangeListener {
    //region Variable Declarations
    private final MekHQ application;
    private final File campaignFile;
    private final Task task;
    private JLabel splash;
    private JProgressBar progressBar;
    private StoryArcStub storyArcStub;

    //endregion Variable Declarations

    //region Constructors
    public DataLoadingDialog(final JFrame frame, final MekHQ application,
                             final @Nullable File campaignFile) {
            this(frame, application, campaignFile, null);
    }

    public DataLoadingDialog(final JFrame frame, final MekHQ application,
                             final @Nullable File campaignFile, StoryArcStub stub) {
        super(frame, "DataLoadingDialog", "DataLoadingDialog.title");
        this.application = application;
        this.campaignFile = campaignFile;
        this.storyArcStub = stub;
        this.task = new Task(this);
        getTask().addPropertyChangeListener(this);
        initialize();
        getTask().execute();
    }
    //endregion Constructors

    //region Getters/Setters
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
    //endregion Getters/Setters

    //region Initialization
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
    //endregion Initialization

    //region PropertyChangeListener
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
                progressBar.setString(resources.getString((getCampaignFile() == null) ? "initializingNewCampaign.text" : "loadingCampaign.text"));
                break;
            case 7:
                progressBar.setString(resources.getString((getCampaignFile() == null) ? "applyingNewCampaign.text" : "applyingLoadedCampaign.text"));
                break;
            default:
                progressBar.setString(resources.getString("Error.text"));
                break;
        }

        getAccessibleContext().setAccessibleDescription(String.format(
                resources.getString("DataLoadingDialog.progress.accessibleDescription"),
                progressBar.getString()));
    }
    //endregion PropertyChangeListener

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
         * 4 : Portraits, Camouflage, Mech Tileset, Force Icons, Awards
         * 5 : Units
         * 6 : New Campaign / Campaign Loading
         * 7 : Campaign Application
         * @return The loaded campaign
         * @throws Exception if anything goes wrong
         */
        @Override
        public Campaign doInBackground() throws Exception {
            //region Progress 0
            setProgress(0);
            CurrencyManager.getInstance().loadCurrencies();
            Eras.initializeEras();
            FinancialInstitutions.initializeFinancialInstitutions();
            InjuryTypes.registerAll(); // TODO : Isolate into an actual module
            Ranks.initializeRankSystems();
            RATManager.populateCollectionNames();
            SkillType.initializeTypes();
            SpecialAbility.initializeSPA();
            //endregion Progress 0

            //region Progress 1
            setProgress(1);
            Factions.setInstance(Factions.loadDefault());
            //endregion Progress 1

            //region Progress 2
            setProgress(2);
            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            Bloodname.loadBloodnameData();
            //endregion Progress 2

            //region Progress 3
            setProgress(3);
            Systems.setInstance(Systems.loadDefault());
            //endregion Progress 3

            //region Progress 4
            setProgress(4);
            MHQStaticDirectoryManager.initialize();
            //endregion Progress 4

            //region Progress 5
            setProgress(5);
            while (!MechSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            //endregion Progress 5

            setProgress(6);
            final Campaign campaign;
            if (getCampaignFile() == null) {
                //region Progress 6
                LogManager.getLogger().info("Starting a new campaign");
                campaign = new Campaign();

                // Campaign Preset
                final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(dialog, getFrame());
                if (presetSelectionDialog.showDialog().isCancelled()) {
                    return null;
                }
                final CampaignPreset preset = presetSelectionDialog.getSelectedPreset();

                // Date
                final LocalDate date = ((preset == null) || (preset.getDate() == null))
                        ? campaign.getLocalDate() : preset.getDate();
                final DateChooser dc = new DateChooser(dialog, date);
                dc.setLocationRelativeTo(getFrame());
                // user can either choose a date or cancel by closing
                if (dc.showDateChooser() != DateChooser.OK_OPTION) {
                    return null;
                }

                if ((preset != null) && (preset.getGameOptions() != null)) {
                    campaign.setGameOptions(preset.getGameOptions());
                }
                campaign.setLocalDate(dc.getDate());
                campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());
                campaign.setStartingSystem((preset == null) ? null : preset.getPlanet());

                // This must be after the date chooser to enable correct functionality.
                setVisible(false);

                // Campaign Options
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(dialog, getFrame(), campaign, true);
                optionsDialog.setLocationRelativeTo(getFrame());
                optionsDialog.applyPreset(preset);
                if (optionsDialog.showDialog().isCancelled()) {
                    return null;
                }
                //endregion Progress 6

                //region Progress 7
                setProgress(7);
                campaign.beginReport("<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) + "</b>");

                // Setup Personnel Modules
                campaign.setMarriage(campaign.getCampaignOptions().getRandomMarriageMethod().getMethod(campaign.getCampaignOptions()));
                campaign.setDivorce(campaign.getCampaignOptions().getRandomDivorceMethod().getMethod(campaign.getCampaignOptions()));
                campaign.setProcreation(campaign.getCampaignOptions().getRandomProcreationMethod().getMethod(campaign.getCampaignOptions()));

                // Setup Markets
                campaign.getPersonnelMarket().generatePersonnelForDay(campaign);
                // TODO : AbstractContractMarket : Uncomment
                //campaign.getContractMarket().generateContractOffers(campaign, (preset == null) ? 2 : preset.getContractCount());
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
                //endregion Progress 7
            } else {
                //region Progress 6
                LogManager.getLogger().info(String.format("Loading campaign file from XML file %s", getCampaignFile()));

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(getCampaignFile())) {
                    campaign = CampaignFactory.newInstance(getApplication()).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
                //endregion Progress 6

                //region Progress 7
                setProgress(7);
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
                //endregion Progress 7
            }
            campaign.setApp(getApplication());
            return campaign;
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
                LogManager.getLogger().error("", ex);
                if (ex.getCause() instanceof NullEntityException) {
                    JOptionPane.showMessageDialog(null,
                            String.format(resources.getString("DataLoadingDialog.NullEntityException.text"),
                                    ex.getCause().getMessage()),
                            resources.getString("DataLoadingDialog.NullEntityException.title"),
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
