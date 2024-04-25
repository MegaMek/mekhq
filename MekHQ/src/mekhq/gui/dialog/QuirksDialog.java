/*
 * QuirksDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
import megamek.client.ui.swing.DialogOptionComponent;
import megamek.client.ui.swing.DialogOptionListener;
import megamek.client.ui.swing.QuirksPanel;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.WeaponQuirks;
import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @since 3/26/2012
 */
public class QuirksDialog extends JDialog implements DialogOptionListener, ActionListener {
    private QuirksPanel qpanel;
    private HashMap<Integer, WeaponQuirks> h_wpnQuirks = new HashMap<>();
    private Entity entity;

    private JButton okayButton;
    private JButton cancelButton;

    /**
     * Handles the editing and deteling of Quirks. Utilizes the QuirksPanel from megamek for the bulk of its work.
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

        //Set up the megamek QuirksPanel.
        for (Mounted m : entity.getWeaponList()) {
            h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
        }
        qpanel = new QuirksPanel(entity, entity.getQuirks(), true, this, h_wpnQuirks);
        qpanel.refreshQuirks();

        //Set up the display of this dialog.
        JScrollPane scroller = new JScrollPane(qpanel);
        scroller.setPreferredSize(new Dimension(300,200));
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(QuirksDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
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
    public void optionClicked(DialogOptionComponent dialogOptionComponent, IOption iOption, boolean b) {
        //Not Used  Included because QuriksPanel requires a DialogOptionListener interface.
    }

    @Override
    public void optionSwitched(DialogOptionComponent comp, IOption option, int i) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (okayButton.equals(e.getSource())) {
            qpanel.setQuirks();
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        }
    }
}
