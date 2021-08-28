/*
 * Copyright (c) 2009 - 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.MechSummaryCache;
import megamek.common.QuirksHandler;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.GamePreset;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.CurrencyManager;
import mekhq.campaign.mod.am.InjuryTypes;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.eras.Eras;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
    private Task task;
    private MekHQ app;
    private JFrame frame;
    private File fileCampaign;
    private ResourceBundle resourceMap;

    public DataLoadingDialog(MekHQ app, JFrame frame, File f) {
        super(frame, "Data Loading");
        this.frame = frame;
        this.app = app;
        this.fileCampaign = f;

        resourceMap = ResourceBundle.getBundle("mekhq.resources.DataLoadingDialog", new EncodeControl());

        setUndecorated(true);
        progressBar = new JProgressBar(0, 4);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressBar.setVisible(true);
        progressBar.setString(resourceMap.getString("loadPlanet.text"));

        // initialize loading image
        double maxWidth = app.calculateMaxScreenWidth();
        Image imgSplash = getToolkit().getImage(app.getIconPackage().getLoadingScreenImage((int) maxWidth));

        // wait for loading image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }
        // make splash image panel
        ImageIcon icon = new ImageIcon(imgSplash);
        JLabel splash = new JLabel(icon);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splash, BorderLayout.CENTER);
        getContentPane().add(progressBar, BorderLayout.PAGE_END);

        setSize(imgSplash.getWidth(null), imgSplash.getHeight(null));
        this.setLocationRelativeTo(frame);

        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
        setUserPreferences();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(DataLoadingDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    class Task extends SwingWorker<Campaign, Campaign> {
        /*
         * Main task. Executed in background thread.
         */
        private boolean cancelled = false;

        @Override
        public Campaign doInBackground() throws Exception {
            //region Progress 0
            //Initialize progress property.
            setProgress(0);

            Eras.initializeEras();

            Factions.setInstance(Factions.loadDefault());

            CurrencyManager.getInstance().loadCurrencies();

            Bloodname.loadBloodnameData();

            //Load values needed for CampaignOptionsDialog
            RATManager.populateCollectionNames();

            // Initialize the systems
            Systems.setInstance(Systems.loadDefault());

            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            Ranks.initializeRankSystems();
            //endregion Progress 0

            //region Progress 1
            setProgress(1);

            QuirksHandler.initQuirksList();

            while (!MechSummaryCache.getInstance().isInitialized()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
            //endregion Progress 1

            //region Progress 2
            setProgress(2);
            //load in directory items and tilesets
            MHQStaticDirectoryManager.initialize();
            //endregion Progress 2

            //region Progress 3
            setProgress(3);

            Campaign campaign;
            boolean newCampaign = false;
            if (fileCampaign == null) {
                newCampaign = true;
                campaign = new Campaign();

                // TODO: Make this depending on campaign options
                InjuryTypes.registerAll();
                campaign.setApp(app);
            } else {
                MekHQ.getLogger().info(String.format("Loading campaign file from XML %s", fileCampaign));

                // And then load the campaign object from it.
                try (FileInputStream fis = new FileInputStream(fileCampaign)) {
                    campaign = CampaignFactory.newInstance(app).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                }
            }
            //endregion Progress 3

            //region Progress 4
            setProgress(4);
            if (newCampaign) {
                // show the date chooser
                DateChooser dc = new DateChooser(frame, campaign.getLocalDate());
                // user can either choose a date or cancel by closing
                if (dc.showDateChooser() == DateChooser.OK_OPTION) {
                    campaign.setLocalDate(dc.getDate());
                    campaign.getGameOptions().getOption("year").setValue(campaign.getGameYear());
                }

                // This must be after the date chooser to enable correct functionality.
                setVisible(false);

                // Game Presets
                GamePreset gamePreset = null;
                List<GamePreset> presets = GamePreset.getGamePresetsIn();
                if (!presets.isEmpty()) {
                    ChooseGamePresetDialog cgpd = new ChooseGamePresetDialog(frame, true, presets);
                    cgpd.setVisible(true);
                    gamePreset = cgpd.getSelectedPreset();
                }
                CampaignOptionsDialog optionsDialog = new CampaignOptionsDialog(frame, true, campaign);
                if (gamePreset != null) {
                    optionsDialog.applyPreset(gamePreset);
                }
                optionsDialog.setVisible(true);
                if (optionsDialog.wasCancelled()) {
                    cancelled = true;
                    cancel(true);
                } else {
                    campaign.beginReport("<b>" + MekHQ.getMekHQOptions().getLongDisplayFormattedDate(campaign.getLocalDate()) + "</b>");
                    campaign.setStartingSystem();
                    campaign.getPersonnelMarket().generatePersonnelForDay(campaign);
                    // TODO : AbstractContractMarket : Uncomment
                    //campaign.getContractMarket().generateContractOffers(campaign, 2);
                    campaign.getUnitMarket().generateUnitOffers(campaign);

                    campaign.reloadNews();
                    campaign.readNews();
                    if (campaign.getCampaignOptions().getUseAtB()) {
                        campaign.initAtB(true);
                    }
                }
            } else {
                // Make sure campaign options event handlers get their data
                MekHQ.triggerEvent(new OptionsChangedEvent(campaign));
            }
            //endregion Progress 4

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
            } catch (InterruptedException e) {
                cancelled = true;
                cancel(true);
            } catch (ExecutionException e) {
                MekHQ.getLogger().error(e.getCause());
                if (e.getCause() instanceof NullEntityException) {
                    NullEntityException nee = (NullEntityException) e.getCause();
                    JOptionPane.showMessageDialog(null,
                            "The following units could not be loaded by the campaign:\n "
                                    + nee.getError() + "\n\nPlease be sure to copy over any custom units "
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
                } else if (e.getCause() instanceof OutOfMemoryError) {
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
    public void propertyChange(PropertyChangeEvent arg0) {
        int progress = task.getProgress();
        progressBar.setValue(progress);

        // If you add a new tier you MUST increase the levels of the progressBar
        switch (progress) {
            case 0:
                progressBar.setString(resourceMap.getString("loadPlanet.text"));
                break;
            case 1:
                progressBar.setString(resourceMap.getString("loadUnits.text"));
                break;
            case 2:
                progressBar.setString(resourceMap.getString("loadImages.text"));
                break;
            case 3:
                progressBar.setString(resourceMap.getString("loadCampaign.text"));
                break;
            default:
                break;
        }

        getAccessibleContext().setAccessibleDescription(
                String.format(resourceMap.getString("accessibleDescription.format"), progressBar.getString()));
    }
}
