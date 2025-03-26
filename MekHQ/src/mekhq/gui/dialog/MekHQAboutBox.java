/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;

public class MekHQAboutBox extends JDialog {
    private static final MMLogger logger = MMLogger.create(MekHQAboutBox.class);

    public MekHQAboutBox(JFrame parent) {
        super(parent);
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {
        JLabel appTitleLabel             = new JLabel();
        JLabel versionLabel              = new JLabel();
        JLabel appVersionLabel           = new JLabel();
        JLabel versionLabelMegaMek       = new JLabel();
        JLabel appVersionLabelMegaMek    = new JLabel();
        JLabel versionLabelMegaMekLab    = new JLabel();
        JLabel appVersionLabelMegaMekLab = new JLabel();
        JLabel homepageLabel             = new JLabel();
        JLabel appHomepage               = new JLabel();
        JLabel appDescLabel              = new JLabel();

        final ResourceBundle mekhqProperties = ResourceBundle.getBundle("mekhq.resources.MekHQ",
              MekHQ.getMHQOptions().getLocale());
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQAboutBox",
              MekHQ.getMHQOptions().getLocale());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MekHQ");
        setModal(false);
        setName("aboutBox");
        setResizable(false);
        getContentPane().setLayout(new GridBagLayout());

        appTitleLabel.setText(mekhqProperties.getString("Application.title"));
        appTitleLabel.setName("appTitleLabel");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx   = 0.0;
        gridBagConstraints.weighty   = 0.0;
        getContentPane().add(appTitleLabel, gridBagConstraints);

        versionLabel.setText(resourceMap.getString("versionLabel.text"));
        versionLabel.setName("versionLabel");
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        getContentPane().add(versionLabel, gridBagConstraints);

        appVersionLabel.setText(MHQConstants.VERSION.toString());
        appVersionLabel.setName("appVersionLabel");
        gridBagConstraints.gridx = 1;
        getContentPane().add(appVersionLabel, gridBagConstraints);

        versionLabelMegaMek.setText(resourceMap.getString("versionLabelMegaMek.text"));
        versionLabelMegaMek.setName("versionLabelMegaMek");
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridwidth = 1;
        getContentPane().add(versionLabelMegaMek, gridBagConstraints);

        appVersionLabelMegaMek.setText(MHQConstants.VERSION.toString());
        appVersionLabelMegaMek.setName("appVersionLabelMegaMek");
        gridBagConstraints.gridx = 1;
        getContentPane().add(appVersionLabelMegaMek, gridBagConstraints);

        versionLabelMegaMekLab.setText(resourceMap.getString("versionLabelMegaMekLab.text"));
        versionLabelMegaMekLab.setName("versionLabelMegaMekLab");
        gridBagConstraints.gridy     = 3;
        gridBagConstraints.gridx     = 0;
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
        appHomepage.setText("<html><font color='#0000EE'>" +
                            mekhqProperties.getString("Application.homepage") +
                            "</font></html>");
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
                    } catch (Exception ex) {
                        logger.error(ex, "Unable to display the About Box.");
                    }
                }
            }
        });

        gridBagConstraints.gridx = 1;
        getContentPane().add(appHomepage, gridBagConstraints);

        appDescLabel.setText(mekhqProperties.getString("Application.description"));
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.weighty   = 1.0;

        // add some space at the bottom so the description text is easier to read
        gridBagConstraints.insets = new Insets(15, 15, 15, 15);

        appDescLabel.setName("appDescLabel");

        getContentPane().add(appDescLabel, gridBagConstraints);

        setSize(200, 200);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekHQAboutBox.class);
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }
}
