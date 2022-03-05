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
import megamek.common.QuirksHandler;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {
    private JProgressBar progressBar;
    private Task task;
    private MekHQ app;
    private JFrame frame;
    private File campaignFile;
    private ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.DataLoadingDialog",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());

    public DataLoadingDialog(final JFrame frame, final MekHQ app, final File campaignFile) {
        super(frame, "Data Loading");
        this.frame = frame;
        this.app = app;
        this.campaignFile = campaignFile;

        setUndecorated(true);
        progressBar = new JProgressBar(0, 4);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        progressBar.setString(resources.getString("loadPlanet.text"));

        JLabel splash = UIUtil.createSplashComponent(app.getIconPackage().getLoadingScreenImages(), frame);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splash, BorderLayout.CENTER);
        getContentPane().add(progressBar, BorderLayout.PAGE_END);

        setPreferredSize(splash.getPreferredSize());
        setSize(splash.getPreferredSize());

        this.pack();
        this.setLocationRelativeTo(frame);

        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    /**
     * Main task. This is executed in a background thread.
     */
    class Task extends SwingWorker<Campaign, Campaign> {
        private boolean cancelled = false;

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
            Eras.initializeEras();
            CurrencyManager.getInstance().loadCurrencies();
            InjuryTypes.registerAll(); // TODO : Isolate into an actual module
            Ranks.initializeRankSystems();
            RATManager.populateCollectionNames();
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
            QuirksHandler.initQuirksList();

            while (!MechSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            //endregion Progress 5

            setProgress(6);
            final Campaign campaign;
            if (campaignFile == null) {
                //region Progress 6
                LogManager.getLogger().info("Starting a new campaign");
                campaign = new Campaign();

                // Campaign Preset
                final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(frame);
                presetSelectionDialog.setLocationRelativeTo(frame);
                if (presetSelectionDialog.showDialog().isCancelled()) {
                    setVisible(false);
                    cancelled = true;
                    cancel(true);
                    return campaign; // shouldn't be required, but this ensures no further code runs
                }
                final CampaignPreset preset = presetSelectionDialog.getSelectedPreset();

                // Date
                final LocalDate date = ((preset == null) || (preset.getDate() == null))
                        ? campaign.getLocalDate() : preset.getDate();
                final DateChooser dc = new DateChooser(frame, date);
                dc.setLocationRelativeTo(frame);
                // user can either choose a date or cancel by closing
                if (dc.showDateChooser() != DateChooser.OK_OPTION) {
                    setVisible(false);
                    cancelled = true;
                    cancel(true);
                    return campaign; // shouldn't be required, but this ensures no further code runs
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
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(frame, campaign, true);
                optionsDialog.setLocationRelativeTo(frame);
                optionsDialog.applyPreset(preset);
                if (optionsDialog.showDialog().isCancelled()) {
                    cancelled = true;
                    cancel(true);
                    return campaign; // shouldn't be required, but this ensures no further code runs
                }
                //endregion Progress 6

                //region Progress 7
                // 7 : Campaign Application
                setProgress(7);
                campaign.beginReport("<b>" + MekHQ.getMHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) + "</b>");

                // Setup Personnel Modules
                campaign.setMarriage(campaign.getCampaignOptions().getRandomMarriageMethod().getMethod(campaign.getCampaignOptions()));
                campaign.setDivorce(campaign.getCampaignOptions().getRandomDivorceMethod().getMethod(campaign.getCampaignOptions()));
                campaign.setProcreation(campaign.getCampaignOptions().getRandomProcreationMethod().getMethod(campaign.getCampaignOptions()));

                // Setup Markets
                campaign.getPersonnelMarket().generatePersonnelForDay(campaign);
                // TODO : AbstractContractMarket : Uncomment
                //campaign.getContractMarket().generateContractOffers(campaign, preset.getContractCount());
                if (!campaign.getCampaignOptions().getUnitMarketMethod().isNone()) {
                    campaign.setUnitMarket(campaign.getCampaignOptions().getUnitMarketMethod().getUnitMarket());
                    campaign.getUnitMarket().generateUnitOffers(campaign);
                }

                // News
                campaign.reloadNews();
                campaign.readNews();

                campaign.setGMMode(true);

                if (campaign.getCampaignOptions().getUseAtB()) {
                    campaign.initAtB(true);
                }
                //endregion Progress 7
            } else {
                //region Progress 6
                LogManager.getLogger().info(String.format("Loading campaign file from XML %s", campaignFile));

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(campaignFile)) {
                    campaign = CampaignFactory.newInstance(app).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
                //endregion Progress 6

                //region Progress 7
                // 7 : Campaign Application
                setProgress(7);
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
                //endregion Progress 7
            }

            campaign.setApp(app);
            return campaign;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Campaign campaign = null;
            try {
                campaign = get();
            } catch (InterruptedException | CancellationException e) {
                cancelled = true;
                cancel(true);
            } catch (ExecutionException ex) {
                LogManager.getLogger().error("", ex);
                if (ex.getCause() instanceof NullEntityException) {
                    NullEntityException nee = (NullEntityException) ex.getCause();
                    JOptionPane.showMessageDialog(null,
                            "The following units could not be loaded by the campaign:\n "
                                    + nee.getMessage() + "\n\nPlease be sure to copy over any custom units "
                                    + "before starting a new version of MekHQ.\nIf you believe the units "
                                    + "listed are not customs, then try deleting the file data/mechfiles/units.cache "
                                    + "and restarting MekHQ.\nIt is also possible that unit chassi "
                                    + "and model names have changed across versions of MegaMek. "
                                    + "You can check this by opening up MegaMek and searching for the units. "
                                    + "Chassis and models can be edited in your MekHQ save file with a text editor.",
                            "Unit Loading Error",
                            JOptionPane.ERROR_MESSAGE);
                    cancelled = true;
                    cancel(true);
                } else if (ex.getCause() instanceof OutOfMemoryError) {
                    JOptionPane.showMessageDialog(null,
                    "MekHQ ran out of memory attempting to load the campaign file. "
                            + "\nTry increasing the memory allocated to MekHQ and reloading. "
                            + "\nSee the FAQ at http://megamek.org for details.",
                    "Not Enough Memory",
                    JOptionPane.ERROR_MESSAGE);
                    cancelled = true;
                    cancel(true);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "The campaign file could not be loaded. \nPlease check the log file for details.",
                            "Campaign Loading Error",
                            JOptionPane.ERROR_MESSAGE);
                    cancelled = true;
                    cancel(true);
                }
            }

            setVisible(false);
            if (!cancelled && (campaign != null)) {
                app.setCampaign(campaign);
                app.getCampaignController().setHost(campaign.getId());
                frame.setVisible(false);
                frame.dispose();
                app.showNewView();
            }
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        int progress = task.getProgress();
        progressBar.setValue(progress);

        // If you add a new tier you MUST increase the levels of the progressBar
        switch (progress) {
            case 0:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 1:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 2:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 3:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 4:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 5:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 6:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            case 7:
                progressBar.setString(resources.getString("loadBaseData.text"));
                break;
            default:
                progressBar.setString(resources.getString("Error.text"));
                break;
        }

        getAccessibleContext().setAccessibleDescription(
                String.format(resources.getString("accessibleDescription.format"), progressBar.getString()));
    }
}
