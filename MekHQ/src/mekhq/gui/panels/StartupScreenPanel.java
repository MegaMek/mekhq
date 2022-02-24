/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQPanel;
import mekhq.gui.dialog.DataLoadingDialog;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class StartupScreenPanel extends AbstractMHQPanel {
    //region Variable Declarations
    private MekHQ app;
    private File lastSaveFile;
    private BufferedImage backgroundIcon;
    //endregion Variable Declarations

    //region Constructors
    public StartupScreenPanel(final MekHQ app) {
        super(new JFrame(MHQConstants.PROJECT_NAME), "StartupScreenPanel");

        this.app = app;
        lastSaveFile = Utilities.lastFileModified(MekHQ.getCampaignsDirectory().getValue(),
                (dir, name) -> (name.toLowerCase().endsWith(".cpnx") || name.toLowerCase().endsWith(".xml"))
                        || name.toLowerCase().endsWith(".cpnx.gz"));

        initialize();
    }
    //endregion Constructors

    //region Initialization
    @Override
    protected void initialize() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);

        setBackground(UIManager.getColor("controlHighlight"));

        final Image imgSplash = getToolkit().getImage(app.getIconPackage()
                .getStartupScreenImage((int) app.calculateMaxScreenWidth()));

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(getFrame());
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        // make splash image panel
        ImageIcon icon = new ImageIcon(imgSplash);
        JLabel panTitle = new JLabel(icon);
        add(panTitle, BorderLayout.CENTER);
        if (skinSpec.hasBackgrounds()) {
            if (skinSpec.backgrounds.size() > 1) {
                File file = new MegaMekFile(Configuration.widgetsDir(),
                        skinSpec.backgrounds.get(1)).getFile();
                if (!file.exists()) {
                    LogManager.getLogger().error("Background icon doesn't exist: " + file.getAbsolutePath());
                } else {
                    backgroundIcon = (BufferedImage) ImageUtil.loadImageFromFile(file.toString());
                }
            }
        } else {
            backgroundIcon = null;
        }

        final JLabel lblVersion = new JLabel(String.format("%s %s %s", MHQConstants.PROJECT_NAME,
                resources.getString("Version.text"), MHQConstants.VERSION), JLabel.CENTER);
        lblVersion.setPreferredSize(new Dimension(250,15));
        if (!skinSpec.fontColors.isEmpty()) {
            lblVersion.setForeground(skinSpec.fontColors.get(0));
        }

        /*
        final MMButton btnNewCampaign = new MMButton("btnNewCampaign", resources,
                "btnNewCampaign.text", null, evt -> startCampaign(null));

        final MMButton btnLoadCampaign = new MMButton("btnLoadCampaign", resources,
                "btnLoadCampaign.text", null, evt -> {
            final File file = selectCampaignFile();
            if (file != null) {
                startCampaign(file);
            }
        });

        final MMButton btnLoadLastCampaign = new MMButton("btnLoadLastCampaign", resources,
                "btnLoadLastCampaign.text", null, evt -> startCampaign(lastSaveFile));
        btnLoadLastCampaign.setEnabled(lastSaveFile != null);

        final MMButton btnQuit = new MMButton("btnQuit", resources, "Quit.text",
                null, evt -> System.exit(0));
         */

        MegamekButton btnNewCampaign = new MegamekButton(resources.getString("btnNewCampaign.text"),
                UIComponents.MainMenuButton.getComp(), true);
        btnNewCampaign.addActionListener(evt -> startCampaign(null));

        MegamekButton btnLoadCampaign = new MegamekButton(resources.getString("btnLoadCampaign.text"),
                UIComponents.MainMenuButton.getComp(), true);
        btnLoadCampaign.addActionListener(evt -> {
            final File file = selectCampaignFile();
            if (file != null) {
                startCampaign(file);
            }
        });

        MegamekButton btnLoadLastCampaign = new MegamekButton(resources.getString("btnLoadLastCampaign.text"),
                UIComponents.MainMenuButton.getComp(), true);
        btnLoadLastCampaign.setEnabled(lastSaveFile != null);
        btnLoadLastCampaign.addActionListener(evt -> startCampaign(lastSaveFile));

        MegamekButton btnQuit = new MegamekButton(resources.getString("Quit.text"),
                UIComponents.MainMenuButton.getComp(), true);
        btnQuit.addActionListener(evt -> System.exit(0));

        // Use the current monitor, so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        DisplayMode currentMonitor = getFrame().getGraphicsConfiguration().getDevice().getDisplayMode();
        FontMetrics metrics = btnNewCampaign.getFontMetrics(btnNewCampaign.getFont());
        int width = metrics.stringWidth(btnNewCampaign.getText());
        int height = metrics.getHeight();
        Dimension textDim =  new Dimension(width + 50, height + 10);

        // Strive for no more than ~90% of the screen and use golden ratio to make
        // the button width "look" reasonable.
        int imageWidth = imgSplash.getWidth(getFrame());

        // But keep the maximum to no more than 50% of image width
        int maximumWidth = (int) Math.min((0.9 * currentMonitor.getWidth()) - imageWidth, 0.5 * imageWidth);

        Dimension minButtonDim = new Dimension((int) (maximumWidth / 1.618), 25);
        if (textDim.getWidth() > minButtonDim.getWidth()) {
            minButtonDim = textDim;
        }

        btnNewCampaign.setMinimumSize(minButtonDim);
        btnNewCampaign.setPreferredSize(minButtonDim);
        btnLoadCampaign.setMinimumSize(minButtonDim);
        btnLoadCampaign.setPreferredSize(minButtonDim);
        btnLoadLastCampaign.setMinimumSize(minButtonDim);
        btnLoadLastCampaign.setPreferredSize(minButtonDim);
        btnQuit.setMinimumSize(minButtonDim);
        btnQuit.setPreferredSize(minButtonDim);

        // layout
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        // Left Column
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 5, 10, 10);
        c.ipadx = 10; c.ipady = 5;
        c.gridx = 0;  c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0; c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 12;
        add(panTitle, c);
        // Right Column
        c.insets = new Insets(2, 2, 2, 10);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 1.0;
        c.ipadx = 0; c.ipady = 0;
        c.gridheight = 1;
        c.gridx = 1; c.gridy = 0;
        add(lblVersion, c);
        c.gridy++;
        add(btnNewCampaign, c);
        c.gridy++;
        add(btnLoadCampaign, c);
        c.gridy++;
        add(btnLoadLastCampaign, c);
        c.gridy++;
        add(btnQuit, c);

        getFrame().setResizable(false);
        getFrame().getContentPane().setLayout(new BorderLayout());
        getFrame().getContentPane().add(this, BorderLayout.CENTER);
        getFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                getFrame().setVisible(false);
                System.exit(0);
            }
        });
        getFrame().validate();
        getFrame().pack();

        // Determine the location of the window
        int w = getFrame().getSize().width;
        int h = getFrame().getSize().height;
        int x = (currentMonitor.getWidth() - w) / 2;
        int y = (currentMonitor.getHeight() - h) / 2;
        getFrame().setLocation(x, y);
    }
    //endregion Initialization

    //region Button Actions
    private void startCampaign(final @Nullable File file) {
        new DataLoadingDialog(app, getFrame(), file).setVisible(true);
    }

    private @Nullable File selectCampaignFile() {
        return FileDialogs.openCampaign(getFrame()).orElse(null);
    }
    //endregion Button Actions

    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundIcon == null) {
            super.paintComponent(g);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int iW = backgroundIcon.getWidth();
        int iH = backgroundIcon.getHeight();
        // If the image isn't loaded, prevent an infinite loop
        if ((iW < 1) || (iH < 1)) {
            return;
        }
        for (int x = 0; x < w; x += iW) {
            for (int y = 0; y < h; y += iH) {
                g.drawImage(backgroundIcon, x, y, null);
            }
        }
    }
}
