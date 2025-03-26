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
 */
package mekhq.gui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import javax.swing.*;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.Entity;
import megamek.common.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.parts.Part;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Taharqa
 */
public class LootDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(LootDialog.class);

    private JFrame            frame;
    private Loot              loot;
    private boolean           cancelled;
    private ArrayList<Entity> units;
    private ArrayList<Part>   parts;
    private Campaign          campaign;

    private JButton       btnOK;
    private JButton       btnCancel;
    private JTextField    txtName;
    private JSpinner      spnCash;
    private JButton       btnAddUnit;
    private JButton       btnRemoveUnit;
    private JButton       btnAddPart;
    private JButton       btnRemovePart;
    private JList<String> listUnits;
    private JList<String> listParts;
    private JScrollPane   scrUnits;
    private JScrollPane   scrParts;

    /** Creates new LootDialog form */
    public LootDialog(JFrame parent, boolean modal, Loot l, Campaign c) {
        super(parent, modal);
        this.frame    = parent;
        this.loot     = l;
        this.campaign = c;
        cancelled     = true;
        units         = new ArrayList<>();
        parts         = new ArrayList<>();
        units.addAll(l.getUnits());
        parts.addAll(l.getParts());
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {

        txtName       = new JTextField();
        btnOK         = new JButton("Done");
        btnCancel     = new JButton("Cancel");
        btnAddUnit    = new JButton("Add");
        btnRemoveUnit = new JButton("Remove");
        btnAddPart    = new JButton("Add");
        btnRemovePart = new JButton("Remove");
        listUnits     = new JList<>(new DefaultListModel<>());
        listParts     = new JList<>(new DefaultListModel<>());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Scenario Costs & Payouts");
        getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Name:"), gridBagConstraints);

        txtName.setText(loot.getName());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx   = 0.0;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(txtName, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Cash"), gridBagConstraints);

        spnCash                      = new JSpinner(new SpinnerNumberModel(loot.getCash().getAmount().intValue(),
              -300000000,
              300000000,
              10000));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx   = 0.0;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(spnCash, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Units"), gridBagConstraints);

        btnAddUnit.addActionListener(evt -> addUnit());

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        getContentPane().add(btnAddUnit, gridBagConstraints);

        btnRemoveUnit.setEnabled(false);
        btnRemoveUnit.addActionListener(evt -> removeUnit());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        getContentPane().add(btnRemoveUnit, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.weighty   = 1.0;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        scrUnits                     = new JScrollPaneWithSpeed(listUnits);
        listUnits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUnits.getSelectionModel().addListSelectionListener(evt -> listUnitsValueChanged());
        refreshUnitList();
        getContentPane().add(scrUnits, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Parts"), gridBagConstraints);

        btnAddPart.addActionListener(evt -> addPart());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        getContentPane().add(btnAddPart, gridBagConstraints);

        btnRemovePart.setEnabled(false);
        btnRemovePart.addActionListener(evt -> removePart());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        getContentPane().add(btnRemovePart, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.weighty   = 1.0;
        gridBagConstraints.anchor    = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        scrParts                     = new JScrollPaneWithSpeed(listParts);
        listParts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listParts.getSelectionModel().addListSelectionListener(evt -> listPartsValueChanged());
        refreshPartList();
        getContentPane().add(scrParts, gridBagConstraints);

        btnOK.addActionListener(evt -> done());
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.EAST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnCancel.addActionListener(evt -> setVisible(false));
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 2;
        gridBagConstraints.gridy     = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weighty   = 0.0;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        getContentPane().add(btnCancel, gridBagConstraints);

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
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(LootDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    public @Nullable Loot getLoot() {
        return cancelled ? null : loot;
    }

    private void addUnit() {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MekSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }
        AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(frame, unitLoadingDialog, campaign, false);
        usd.setVisible(true);

        Entity e = usd.getSelectedEntity();
        if (null != e) {
            units.add(e);
        }
        refreshUnitList();
    }

    private void removeUnit() {
        int row = listUnits.getSelectedIndex();
        if (-1 != row) {
            units.remove(row);
        }
        refreshUnitList();
    }

    private void addPart() {
        PartsStoreDialog psd = new PartsStoreDialog(frame, true, null, campaign, false);
        psd.setVisible(true);
        Part p = psd.getPart();
        if (null != p) {
            parts.add(p);
        }
        refreshPartList();
    }

    private void removePart() {
        int row = listParts.getSelectedIndex();
        if (-1 != row) {
            parts.remove(row);
        }
        refreshPartList();
    }

    private void done() {
        loot.setName(txtName.getText());
        loot.setCash(Money.of((Integer) spnCash.getModel().getValue()));
        cancelled = false;
        loot.clearUnits();
        loot.clearParts();
        for (Entity e : units) {
            loot.addUnit(e);
        }
        for (Part p : parts) {
            loot.addPart(p);
        }
        this.setVisible(false);
    }

    private void refreshUnitList() {
        int                      selectedRow = listUnits.getSelectedIndex();
        DefaultListModel<String> model       = (DefaultListModel<String>) listUnits.getModel();
        model.removeAllElements();
        for (Entity e : units) {
            model.addElement(e.getShortName());
        }
        scrUnits.setViewportView(listUnits);
        if (selectedRow != -1) {
            if (listUnits.getModel().getSize() > 0) {
                if (listUnits.getModel().getSize() == selectedRow) {
                    listUnits.setSelectedIndex(selectedRow - 1);
                } else {
                    listUnits.setSelectedIndex(selectedRow);
                }
            }
        }
    }

    private void refreshPartList() {
        int                      selectedRow = listParts.getSelectedIndex();
        DefaultListModel<String> model       = (DefaultListModel<String>) listParts.getModel();
        model.removeAllElements();
        for (Part p : parts) {
            model.addElement(p.getName());
        }
        scrParts.setViewportView(listParts);
        if (selectedRow != -1) {
            if (listParts.getModel().getSize() > 0) {
                if (listParts.getModel().getSize() == selectedRow) {
                    listParts.setSelectedIndex(selectedRow - 1);
                } else {
                    listParts.setSelectedIndex(selectedRow);
                }
            }
        }
    }

    private void listUnitsValueChanged() {
        int row = listUnits.getSelectedIndex();
        btnRemoveUnit.setEnabled(row != -1);
    }

    private void listPartsValueChanged() {
        int row = listParts.getSelectedIndex();
        btnRemovePart.setEnabled(row != -1);
    }
}
