/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panels;

import static mekhq.MHQConstants.LAUNCHER_NEW_PLAYER_QUICKSTART_PATH;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.RawImagePanel;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinSpecification.UIComponents;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.storyArc.StoryArcStub;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQPanel;
import mekhq.gui.dialog.DataLoadingDialog;
import mekhq.gui.dialog.MHQOptionsDialog;
import mekhq.gui.dialog.NewPlayerQuickstartDialog;
import mekhq.gui.dialog.StoryArcSelectionDialog;

public class StartupScreenPanel extends AbstractMHQPanel {
    private static final MMLogger LOGGER = MMLogger.create(StartupScreenPanel.class);

    // region Variable Declarations
    private final MekHQ app;
    private final File lastSaveFile;
    private final File newPlayerQuickstartFile;
    private BufferedImage backgroundIcon;

    // Save file filtering needs to avoid loading some special files
    public static FilenameFilter saveFilter = (dir, name) -> {
        // Allow any .xml, .cpnx, and .cpnx.gz file that is not in the list of excluded
        // files
        List<String> toReject = List.of(PreferenceManager.DEFAULT_CFG_FILE_NAME.toLowerCase());
        return (((name.toLowerCase().endsWith(".cpnx") || name.toLowerCase().endsWith(".xml")) ||
                       name.toLowerCase().endsWith(".cpnx.gz")) && !toReject.contains(name.toLowerCase()));
    };

    // endregion Variable Declarations

    // region Constructors
    public StartupScreenPanel(final MekHQ app) {
        super(new JFrame(MHQConstants.PROJECT_NAME), "StartupScreenPanel");

        this.app = app;
        lastSaveFile = Utilities.lastFileModified(MekHQ.getCampaignsDirectory().getValue(), saveFilter);
        newPlayerQuickstartFile = new File(LAUNCHER_NEW_PLAYER_QUICKSTART_PATH);

        initialize();
    }
    // endregion Constructors

    // region Initialization
    @Override
    protected void initialize() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);

        setBackground(UIManager.getColor("controlHighlight"));

        Dimension scaledMonitorSize = UIUtil.getScaledScreenSize(getFrame());
        RawImagePanel splash = UIUtil.createSplashComponent(app.getIconPackage().getStartupScreenImagesScreenImages(),
              getFrame());
        add(splash, BorderLayout.CENTER);

        if (skinSpec.hasBackgrounds()) {
            if (skinSpec.backgrounds.size() > 1) {
                File file = new MegaMekFile(Configuration.widgetsDir(), skinSpec.backgrounds.get(1)).getFile();
                if (!file.exists()) {
                    LOGGER.error("Background icon doesn't exist: {}", file.getAbsolutePath());
                } else {
                    backgroundIcon = (BufferedImage) ImageUtil.loadImageFromFile(file.toString());
                }
            }
        } else {
            backgroundIcon = null;
        }

        final JLabel lblVersion = new JLabel(String.format("%s %s %s",
              MHQConstants.PROJECT_NAME,
              resources.getString("Version.text"),
              MHQConstants.VERSION), JLabel.CENTER);
        lblVersion.setPreferredSize(new Dimension(250, 15));
        if (!skinSpec.fontColors.isEmpty()) {
            lblVersion.setForeground(skinSpec.fontColors.get(0));
        }

        MegaMekButton btnNewCampaign = new MegaMekButton(resources.getString("btnNewCampaign.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnNewCampaign.addActionListener(evt -> {
            btnNewCampaign.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    startCampaign(null);
                } finally {
                    btnNewCampaign.setEnabled(true);
                }
            });
        });

        MegaMekButton btnLoadCampaign = new MegaMekButton(resources.getString("btnLoadCampaign.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnLoadCampaign.addActionListener(evt -> {
            btnLoadCampaign.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    final File file = selectCampaignFile();
                    if (file != null) {
                        startCampaign(file);
                    }
                } finally {
                    btnLoadCampaign.setEnabled(true);
                }
            });
        });

        MegaMekButton btnLoadLastCampaign = new MegaMekButton(resources.getString("btnLoadLastCampaign.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnLoadLastCampaign.setEnabled(lastSaveFile != null);
        btnLoadLastCampaign.addActionListener(evt -> {
            btnLoadLastCampaign.setEnabled(false);
            startCampaign(lastSaveFile);
        });

        MegaMekButton btnNewPlayerQuickstart = new MegaMekButton(resources.getString("btnNewPlayerQuickstart.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnNewPlayerQuickstart.setEnabled(newPlayerQuickstartFile != null);
        btnNewPlayerQuickstart.addActionListener(evt -> {
            btnNewPlayerQuickstart.setEnabled(false);

            if (!new NewPlayerQuickstartDialog(getFrame()).wasCanceled()) {
                startCampaign(newPlayerQuickstartFile);
            } else {
                btnNewPlayerQuickstart.setEnabled(true);
            }
        });

        MegaMekButton btnLoadStoryArc = new MegaMekButton(resources.getString("btnLoadStoryArc.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnLoadStoryArc.addActionListener(evt -> {
            btnLoadStoryArc.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try {
                    StoryArcStub storyArcStub = selectStoryArc();
                    if ((null != storyArcStub) && (null != storyArcStub.getInitCampaignFile())) {
                        startCampaign(storyArcStub.getInitCampaignFile(), storyArcStub);
                    }
                } finally {
                    btnLoadStoryArc.setEnabled(true);
                }
            });
        });

        MegaMekButton btnMHQOptions = new MegaMekButton(resources.getString("MHQOptions.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnMHQOptions.addActionListener(evt -> new MHQOptionsDialog(getFrame()).setVisible(true));

        MegaMekButton btnQuit = new MegaMekButton(resources.getString("Quit.text"),
              UIComponents.MainMenuButton.getComp(),
              true);
        btnQuit.addActionListener(evt -> System.exit(0));

        FontMetrics metrics = btnNewCampaign.getFontMetrics(btnNewCampaign.getFont());
        int width = metrics.stringWidth(btnNewCampaign.getText());
        int height = metrics.getHeight();
        Dimension textDim = new Dimension(width + 50, height + 10);

        // Strive for no more than ~90% of the screen and use golden ratio to make
        // the button width "look" reasonable.
        int maximumWidth = (int) (0.9 * scaledMonitorSize.width) - splash.getPreferredSize().width;

        //no more than 50% of image width
        if (maximumWidth > (int) (0.5 * splash.getPreferredSize().width)) {
            maximumWidth = (int) (0.5 * splash.getPreferredSize().width);
        }

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
        btnNewPlayerQuickstart.setMinimumSize(minButtonDim);
        btnNewPlayerQuickstart.setPreferredSize(minButtonDim);
        btnLoadStoryArc.setMinimumSize(minButtonDim);
        btnLoadStoryArc.setPreferredSize(minButtonDim);
        btnMHQOptions.setMinimumSize(minButtonDim);
        btnMHQOptions.setPreferredSize(minButtonDim);
        btnQuit.setMinimumSize(minButtonDim);
        btnQuit.setPreferredSize(minButtonDim);

        // layout
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        // Left Column
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 12;
        add(splash, c);
        // Right Column
        c.insets = new Insets(2, 2, 2, 10);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 0;
        add(lblVersion, c);
        c.gridy++;
        add(btnNewPlayerQuickstart, c);
        c.gridy++;
        add(btnNewCampaign, c);
        c.gridy++;
        add(btnLoadCampaign, c);
        c.gridy++;
        add(btnLoadLastCampaign, c);
        c.gridy++;
        add(btnLoadStoryArc, c);
        c.gridy++;
        add(btnMHQOptions, c);
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
        // center window in screen
        getFrame().setLocationRelativeTo(null);
    }
    // endregion Initialization

    // region Button Actions
    private void startCampaign(final @Nullable File file) {
        startCampaign(file, null);
    }

    private void startCampaign(final @Nullable File file, @Nullable StoryArcStub storyArcStub) {
        new DataLoadingDialog(getFrame(), app, file, storyArcStub, false).setVisible(true);
    }

    private @Nullable File selectCampaignFile() {
        return FileDialogs.openCampaign(getFrame()).orElse(null);
    }
    // endregion Button Actions

    private @Nullable StoryArcStub selectStoryArc() {
        final StoryArcSelectionDialog storyArcSelectionDialog = new StoryArcSelectionDialog(getFrame(), true);
        if (storyArcSelectionDialog.showDialog().isCancelled()) {
            return null;
        }
        return (storyArcSelectionDialog.getSelectedStoryArc());
    }

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
