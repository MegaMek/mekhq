/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.dialogs.customMek.QuirksPanel;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.WeaponQuirks;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @since 3/26/2012
 */
public class QuirksDialog extends JDialog implements DialogOptionListener, ActionListener {
    private static final MMLogger logger = MMLogger.create(QuirksDialog.class);

    private QuirksPanel quirksPanel;
    private final HashMap<Integer, WeaponQuirks> h_wpnQuirks = new HashMap<>();
    private final Entity entity;

    private JButton okayButton;
    private JButton cancelButton;

    /**
     * Handles the editing and deleting of Quirks. Uses the QuirksPanel from MegaMek for the bulk of its work.
     *
     * @param entity The {@link Entity} being edited.
     * @param parent The {@link JFrame} of the parent panel.
     */
    public QuirksDialog(Entity entity, JFrame parent) {
        super(parent, "Edit Quirks", true);
        this.entity = entity;
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initGUI() {

        // Set up the MegaMek QuirksPanel.
        for (Mounted<?> m : entity.getWeaponList()) {
            h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
        }
        quirksPanel = new QuirksPanel(entity, entity.getQuirks(), true, this, h_wpnQuirks);
        quirksPanel.refreshQuirks();

        // Set up the display of this dialog.
        JScrollPane scroller = new JScrollPaneWithSpeed(quirksPanel);
        scroller.setPreferredSize(new Dimension(300, 200));
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(QuirksDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        okayButton = new JButton("Okay");
        okayButton.setMnemonic('o');
        okayButton.addActionListener(this);
        panel.add(okayButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('c');
        cancelButton.addActionListener(this);
        panel.add(cancelButton);

        return panel;
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel DialogOptionComponentYPanel, IOption iOption, boolean b) {
        // Not Used Included because QuirksPanel requires a DialogOptionListener
        // interface.
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel comp, IOption option, int i) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (okayButton.equals(e.getSource())) {
            quirksPanel.setQuirks();
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        }
    }
}
