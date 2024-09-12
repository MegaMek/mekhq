/*
 * SelectAbilitiesDialog.java
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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * @author Taharqa
 */
public class SelectAbilitiesDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(SelectAbilitiesDialog.class);

    private JButton btnClose;
    private JButton btnOK;
    private List<JCheckBox> chkAbil;
    private Vector<String> selected;
    private List<String> spaNames;
    private boolean cancelled;

    private Map<String, SpecialAbility> allSPA;

    public SelectAbilitiesDialog(JFrame parent, Vector<String> s, Map<String, SpecialAbility> hash) {
        super(parent, true);
        selected = s;
        allSPA = hash;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        btnOK = new JButton();
        btnClose = new JButton();

        chkAbil = new ArrayList<>();
        spaNames = new ArrayList<>();

        JPanel panMain = new JPanel(new GridLayout(0, 3));

        JCheckBox chk;
        for (final SpecialAbility spa : allSPA.values().stream()
                .sorted((a, b) -> new NaturalOrderComparator().compare(a.getDisplayName(), b.getDisplayName()))
                .collect(Collectors.toList())) {
            chk = new JCheckBox(spa.getDisplayName());
            if (selected.contains(spa.getName())) {
                chk.setSelected(true);
            }
            chkAbil.add(chk);
            panMain.add(chk);
            spaNames.add(spa.getName());
        }

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        btnOK.setText("Done");
        btnOK.addActionListener(evt -> done());

        btnClose.setText("Cancel");
        btnClose.addActionListener(evt -> cancel());

        panButtons.add(btnOK);
        panButtons.add(btnClose);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Abilities");
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SelectAbilitiesDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void done() {
        selected = new Vector<>();
        for (int i = 0; i < spaNames.size(); i++) {
            if (chkAbil.get(i).isSelected()) {
                selected.add(spaNames.get(i));
            }
        }
        this.setVisible(false);
    }

    public Vector<String> getSelected() {
        return selected;
    }

    private void cancel() {
        this.setVisible(false);
        cancelled = true;
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
