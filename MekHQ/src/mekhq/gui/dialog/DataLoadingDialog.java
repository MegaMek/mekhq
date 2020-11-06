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

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomCallsignGenerator;
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
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;
import mekhq.campaign.universe.Systems;

public class DataLoadingDialog extends JDialog implements PropertyChangeListener {
    private static final long serialVersionUID = -3454307876761238915L;
    private JProgressBar progressBar;
    private Task task;
    private MekHQ app;
    private JFrame frame;
    private Campaign campaign;
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

    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */
        private boolean cancelled = false;

        @Override
        public Void doInBackground() {
            //region Progress 0
            //Initialize progress property.
            setProgress(0);
            try {
                Faction.generateFactions();
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
            try {
                CurrencyManager.getInstance().loadCurrencies();
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
            try {
                Bloodname.loadBloodnameData();
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
            try {
                //Load values needed for CampaignOptionsDialog
                RATManager.populateCollectionNames();
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
            while (!Systems.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {

                }
            }
            RandomNameGenerator.getInstance();
            RandomCallsignGenerator.getInstance();
            //endregion Progress 0

            //region Progress 1
            setProgress(1);
            try {
                QuirksHandler.initQuirksList();
            } catch (IOException e) {
                MekHQ.getLogger().error(e);
            }
            while (!MechSummaryCache.getInstance().isInitialized()) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    MekHQ.getLogger().error(e);
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
            boolean newCampaign = false;
            if (fileCampaign == null) {
                try {
                    newCampaign = true;
                    campaign = new Campaign();
                     // TODO: Make this depending on campaign options
                    InjuryTypes.registerAll();
                    campaign.setApp(app);
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                }
            } else {
                MekHQ.getLogger().info("Loading campaign file from XML...");

                // And then load the campaign object from it.
                FileInputStream fis;

                try {
                    fis = new FileInputStream(fileCampaign);
                    campaign = CampaignFactory.newInstance(app).createCampaign(fis);
                    // Restores all transient attributes from serialized objects
                    campaign.restore();
                    campaign.cleanUp();
                    fis.close();
                } catch (NullEntityException e) {
                    JOptionPane.showMessageDialog(null,
                            "The following units could not be loaded by the campaign:\n "
                                    + e.getError() + "\n\nPlease be sure to copy over any custom units "
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
                } catch (Exception e) {
                    MekHQ.getLogger().error(e);
                    JOptionPane.showMessageDialog(null,
                            "The campaign file could not be loaded. \nPlease check the log file for details.",
                            "Campaign Loading Error",
                            JOptionPane.ERROR_MESSAGE);
                    cancelled = true;
                    cancel(true);
                } catch (OutOfMemoryError e) {
                    JOptionPane.showMessageDialog(null,
                            "MekHQ ran out of memory attempting to load the campaign file. "
                                    + "\nTry increasing the memory allocated to MekHQ and reloading. "
                                    + "\nSee the FAQ at http://megamek.org for details.",
                            "Not Enough Memory",
                            JOptionPane.ERROR_MESSAGE);
                    cancelled = true;
                    cancel(true);
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
                    campaign.generateNewPersonnelMarket();
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

            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            setVisible(false);
            if (!cancelled) {
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
    }
}
