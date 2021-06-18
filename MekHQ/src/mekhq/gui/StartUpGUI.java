/*
 * StartUpGUI.java
 *
 * Copyright (c) 2010 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2019 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.gui.dialog.DataLoadingDialog;

public class StartUpGUI extends JPanel {
    private static final long serialVersionUID = 8376874926997734492L;
    private MekHQ app;
    private File lastSave;
    private Image imgSplash;

    private JFrame frame;

    public StartUpGUI(MekHQ app) {
        this.app = app;
        lastSave = Utilities.lastFileModified(MekHQ.getCampaignsDirectory().getValue(),
                (dir, name) -> (name.toLowerCase().endsWith(".cpnx") || name.toLowerCase().endsWith(".xml"))
                        || name.toLowerCase().endsWith(".cpnx.gz"));

        initComponents();
    }

    private void initComponents() {
        frame = new JFrame("MekHQ");

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.StartUpDialog", new EncodeControl());

        // initialize splash image
        double maxWidth = app.calculateMaxScreenWidth();
        imgSplash = getToolkit().getImage(app.getIconPackage().getStartupScreenImage((int) maxWidth));

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        // Determine if the splash screen image is "small"
        // and if so switch to shorter text and smaller buttons
        boolean shortText = false;
        int buttonWidth = 200;
        if (imgSplash.getWidth(null) < 840) {
            shortText = true;
            buttonWidth = 150;
        }

        JButton btnNewGame = new JButton(resourceMap.getString(shortText ? "btnNewGame.text.short" : "btnNewGame.text"));
        btnNewGame.setMinimumSize(new Dimension(buttonWidth, 25));
        btnNewGame.setPreferredSize(new Dimension(buttonWidth, 25));
        btnNewGame.setMaximumSize(new Dimension(buttonWidth, 25));
        btnNewGame.addActionListener(evt -> newCampaign());

        JButton btnLoadGame = new JButton(resourceMap.getString(shortText ? "btnLoadGame.text.short" : "btnLoadGame.text"));
        btnLoadGame.setMinimumSize(new Dimension(buttonWidth, 25));
        btnLoadGame.setPreferredSize(new Dimension(buttonWidth, 25));
        btnLoadGame.setMaximumSize(new Dimension(buttonWidth, 25));
        btnLoadGame.addActionListener(evt -> {
            File f = selectLoadCampaignFile();
            if (null != f) {
                loadCampaign(f);
            }
        });

        JButton btnLastSave = new JButton(resourceMap.getString(shortText ? "btnLastSave.text.short" : "btnLastSave.text"));
        btnLastSave.setMinimumSize(new Dimension(buttonWidth, 25));
        btnLastSave.setPreferredSize(new Dimension(buttonWidth, 25));
        btnLastSave.setMaximumSize(new Dimension(buttonWidth, 25));
        btnLastSave.addActionListener(evt -> loadCampaign(lastSave));
        if (null == lastSave) {
            btnLastSave.setEnabled(false);
        }

        JButton btnQuit = new JButton(resourceMap.getString("btnQuit.text"));
        btnQuit.setMinimumSize(new Dimension(buttonWidth, 25));
        btnQuit.setPreferredSize(new Dimension(buttonWidth, 25));
        btnQuit.setMaximumSize(new Dimension(buttonWidth, 25));
        btnQuit.addActionListener(evt -> System.exit(0));

        setLayout(new BorderLayout(1, 1));

        JPanel buttonPanel = new JPanel();
        buttonPanel.getAccessibleContext().setAccessibleName("Loader Actions");
        buttonPanel.getAccessibleContext().setAccessibleDescription("Contains buttons to perform top-level actions, such as starting a new campaign or loading an old campaign.");
        buttonPanel.setOpaque(false);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(btnNewGame);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(btnLoadGame);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(btnLastSave);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        buttonPanel.add(btnQuit);
        add(buttonPanel, BorderLayout.PAGE_END);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setSize(imgSplash.getWidth(null), imgSplash.getHeight(null));
        frame.setResizable(false);
        // Determine the new location of the window
        int w = frame.getSize().width;
        int h = frame.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;

        // Move the window
        frame.setLocation(x, y);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(imgSplash, 1, 1, null);
    }

    private void newCampaign() {
        loadCampaign(null);
    }

    private void loadCampaign(File f) {
        DataLoadingDialog dataLoadingDialog = new DataLoadingDialog(app, frame, f);
        dataLoadingDialog.setVisible(true);
    }

    private File selectLoadCampaignFile() {
        return FileDialogs.openCampaign(frame).orElse(null);
    }
}
