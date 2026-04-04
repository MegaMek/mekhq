/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.dialogs.LicensingDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;

public class MekHQAboutBox extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(MekHQAboutBox.class);

    public MekHQAboutBox(JFrame parent) {
        super(parent);
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekHQAboutBox",
              MekHQ.getMHQOptions().getLocale());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MekHQ");
        setModal(false);
        setName("aboutBox");
        setResizable(false);

        // Version info panel
        JPanel versionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 5, 2, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel versionLabel = new JLabel(resourceMap.getString("versionLabel.text"));
        versionPanel.add(versionLabel, gbc);

        gbc.gridx = 1;
        versionPanel.add(new JLabel(MHQConstants.VERSION.toString()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        versionPanel.add(new JLabel(resourceMap.getString("versionLabelMegaMek.text")), gbc);

        gbc.gridx = 1;
        versionPanel.add(new JLabel(MHQConstants.VERSION.toString()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        versionPanel.add(new JLabel(resourceMap.getString("versionLabelMegaMekLab.text")), gbc);

        gbc.gridx = 1;
        versionPanel.add(new JLabel(MHQConstants.VERSION.toString()), gbc);

        // Legal/licensing content
        JEditorPane aboutPane = new JEditorPane();
        aboutPane.setContentType("text/html");
        aboutPane.setEditable(false);
        aboutPane.setOpaque(false);
        aboutPane.setText(buildAboutHtml());
        aboutPane.setCaretPosition(0);
        aboutPane.addHyperlinkListener(this::handleHyperlink);

        JScrollPane scrollPane = new JScrollPane(aboutPane);
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(550, 350));
        scrollPane.setBorder(null);

        // Layout
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.add(versionPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(contentPanel);
        pack();
    }

    private static String buildAboutHtml() {
        return "<html><body width='" + UIUtil.scaleForGUI(500) + "'>"
              + LicensingDialog.buildLegalHtml()
              + "</body></html>";
    }

    private void handleHyperlink(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            UIUtil.browse(event.getURL().toString(), this);
        }
    }

    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekHQAboutBox.class);
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
