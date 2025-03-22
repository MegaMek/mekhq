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
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * @author Taharqa
 */
public class SelectUnusedAbilityDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(SelectUnusedAbilityDialog.class);

    private JButton                     btnClose;
    private JButton                     btnOK;
    private ButtonGroup                 group;
    private Vector<String>              choices;
    private boolean                     cancelled;
    private Map<String, SpecialAbility> currentSPA;

    public SelectUnusedAbilityDialog(final JFrame frame, final Vector<String> unused, final Map<String, SpecialAbility> c) {
        super(frame, true);
        choices    = unused;
        currentSPA = c;
        cancelled  = false;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        btnOK    = new JButton();
        btnClose = new JButton();

        group = new ButtonGroup();

        int    ncol    = 2;
        JPanel panMain = new JPanel(new GridLayout((int) Math.ceil(choices.size() / (ncol * 1.0)), ncol));

        JRadioButton chk;
        for (String name : choices) {
            final SpecialAbility spa = SpecialAbility.getDefaultAbility(name);
            chk = new JRadioButton((spa == null) ? getDisplayName(name) : spa.getDisplayName());
            chk.setActionCommand(name);
            chk.setToolTipText((spa == null) ? getDesc(name) : spa.getDescription());
            group.add(chk);
            panMain.add(chk);
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
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SelectUnusedAbilityDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void done() {
        if (null != group.getSelection()) {
            String name        = group.getSelection().getActionCommand();
            String displayName = "";
            String desc        = "";

            PersonnelOptions poptions = new PersonnelOptions();
            for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();

                if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                    continue;
                }

                for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                    IOption option = j.nextElement();
                    if (option.getName().equals(name)) {
                        displayName = option.getDisplayableName();
                        desc        = option.getDescription();
                    }
                }
            }

            SpecialAbility spa;
            if (null != SpecialAbility.getDefaultAbility(name)) {
                spa = SpecialAbility.getDefaultAbility(name).clone();
            } else {
                spa = new SpecialAbility(name, displayName, desc);
            }
            EditSpecialAbilityDialog esad = new EditSpecialAbilityDialog(null, spa, currentSPA);
            esad.setVisible(true);
            if (!esad.wasCancelled()) {
                currentSPA.put(spa.getName(), spa);
            }
        }
        this.setVisible(false);
    }

    private void cancel() {
        this.setVisible(false);
        cancelled = true;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    private String getDisplayName(String lookup) {
        PersonnelOptions poptions = new PersonnelOptions();
        for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                if (option.getName().equals(lookup)) {
                    return (option.getDisplayableName());
                }
            }
        }
        return "??";
    }

    private String getDesc(String lookup) {
        PersonnelOptions poptions = new PersonnelOptions();
        for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                if (option.getName().equals(lookup)) {
                    return option.getDescription();
                }
            }
        }
        return "??";
    }
}
