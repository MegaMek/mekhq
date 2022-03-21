/*
 * MekBayAboutBox.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.EncodeControl;
import mekhq.MHQConstants;
import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ResourceBundle;

public class MekHQAboutBox extends JDialog {
    public MekHQAboutBox(JFrame parent) {
        super(parent);
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        javax.swing.JLabel appTitleLabel = new javax.swing.JLabel();
        javax.swing.JLabel versionLabel = new javax.swing.JLabel();
        javax.swing.JLabel appVersionLabel = new javax.swing.JLabel();
        javax.swing.JLabel versionLabelMegaMek = new javax.swing.JLabel();
        javax.swing.JLabel appVersionLabelMegaMek = new javax.swing.JLabel();
        javax.swing.JLabel versionLabelMegaMekLab = new javax.swing.JLabel();
        javax.swing.JLabel appVersionLabelMegaMekLab = new javax.swing.JLabel();
        javax.swing.JLabel homepageLabel = new javax.swing.JLabel();
        javax.swing.JLabel appHomepage = new javax.swing.JLabel();
        javax.swing.JLabel appDescLabel = new javax.swing.JLabel();

        final ResourceBundle mekhqProperties = ResourceBundle.getBundle("mekhq.resources.MekHQ",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQAboutBox",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MekHQ");
        setModal(false);
        setName("aboutBox");
        setResizable(false);
        getContentPane().setLayout(new GridBagLayout());

        appTitleLabel.setText(mekhqProperties.getString("Application.title"));
        appTitleLabel.setName("appTitleLabel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        getContentPane().add(appTitleLabel, gridBagConstraints);

        versionLabel.setText(resourceMap.getString("versionLabel.text"));
        versionLabel.setName("versionLabel");
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        getContentPane().add(versionLabel, gridBagConstraints);

        appVersionLabel.setText(MHQConstants.VERSION.toString());
        appVersionLabel.setName("appVersionLabel");
        gridBagConstraints.gridx = 1;
        getContentPane().add(appVersionLabel, gridBagConstraints);

        versionLabelMegaMek.setText(resourceMap.getString("versionLabelMegaMek.text"));
        versionLabelMegaMek.setName("versionLabelMegaMek");
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;
        getContentPane().add(versionLabelMegaMek, gridBagConstraints);

        appVersionLabelMegaMek.setText(MHQConstants.VERSION.toString());
        appVersionLabelMegaMek.setName("appVersionLabelMegaMek");
        gridBagConstraints.gridx = 1;
        getContentPane().add(appVersionLabelMegaMek, gridBagConstraints);

        versionLabelMegaMekLab.setText(resourceMap.getString("versionLabelMegaMekLab.text"));
        versionLabelMegaMekLab.setName("versionLabelMegaMekLab");
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;
        getContentPane().add(versionLabelMegaMekLab, gridBagConstraints);

        appVersionLabelMegaMekLab.setText(MHQConstants.VERSION.toString());
        appVersionLabelMegaMekLab.setName("appVersionLabelMegaMekLab");
        gridBagConstraints.gridx = 1;
        getContentPane().add(appVersionLabelMegaMekLab, gridBagConstraints);

        homepageLabel.setText(resourceMap.getString("homepageLabel.text"));
        homepageLabel.setName("homepageLabel");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(homepageLabel, gridBagConstraints);

        // use a JButton but make it look more like a regular link
        appHomepage.setText("<html><font color='#0000EE'>" + mekhqProperties.getString("Application.homepage") + "</font></html>");
        appHomepage.setName("appHomepageLabel");
        appHomepage.setOpaque(false);
        appHomepage.setToolTipText(mekhqProperties.getString("Application.homepage"));
        appHomepage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        appHomepage.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI(mekhqProperties.getString("Application.homepage"));
                        Desktop.getDesktop().browse(uri);
                    } catch (Exception e) {
                        return;
                    }
                }
            }
        });

        gridBagConstraints.gridx = 1;
        getContentPane().add(appHomepage, gridBagConstraints);


        appDescLabel.setText(mekhqProperties.getString("Application.description"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        // add some space at the bottom so the description text is easier to read
        gridBagConstraints.insets = new Insets(15,15,15,15);

        appDescLabel.setName("appDescLabel");

        getContentPane().add(appDescLabel, gridBagConstraints);

        setSize(200,200);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekHQAboutBox.class);

        preferences.manage(new JWindowPreference(this));
    }
}
