/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
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
    private static final MMLogger LOGGER = MMLogger.create(SelectAbilitiesDialog.class);

    private List<JCheckBox> chkAbility;
    private Vector<String> selected;
    private List<String> spaNames;
    private boolean cancelled;

    private final Map<String, SpecialAbility> allSPA;

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
        JButton btnOK = new JButton();
        JButton btnClose = new JButton();

        chkAbility = new ArrayList<>();
        spaNames = new ArrayList<>();

        JPanel panMain = new JPanel(new GridLayout(0, 3));

        JCheckBox chk;
        for (final SpecialAbility spa : allSPA.values()
                                              .stream()
                                              .sorted((a, b) -> new NaturalOrderComparator().compare(a.getDisplayName(),
                                                    b.getDisplayName()))
                                              .toList()) {
            chk = new JCheckBox(spa.getDisplayName());
            if (selected.contains(spa.getName())) {
                chk.setSelected(true);
            }
            chkAbility.add(chk);
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

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SelectAbilitiesDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void done() {
        selected = new Vector<>();
        for (int i = 0; i < spaNames.size(); i++) {
            if (chkAbility.get(i).isSelected()) {
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
