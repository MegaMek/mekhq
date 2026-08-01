/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.*;

import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class EditFamiliarityDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EditFamiliarityDialog";

    private final Person person;
    private final int cap;

    private final Map<String, JSpinner> rowSpinners = new LinkedHashMap<>();
    private final List<String> initialChassis;

    private JPanel rowsPanel;
    private JComboBox<String> comboAddChassis;
    private JSpinner spnAddValue;

    public EditFamiliarityDialog(final JFrame parent, final Campaign campaign, final Person person) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.title"), true);
        this.person = person;
        this.cap = campaign.getCampaignOptions().get(CampaignOption.CHASSIS_FAMILIARITY_MODE).getFamiliarityCap();
        this.initialChassis = new ArrayList<>(person.getChassisFamiliarity().keySet());

        for (Map.Entry<String, Integer> entry : new TreeSet<>(person.getChassisFamiliarity().keySet()).stream()
                                                      .map(chassis -> Map.entry(chassis,
                                                            person.getChassisFamiliarity(chassis)))
                                                      .toList()) {
            rowSpinners.put(entry.getKey(), buildValueSpinner(entry.getValue()));
        }

        initComponents(campaign);
        pack();
        setLocationRelativeTo(parent);
    }

    private JSpinner buildValueSpinner(final int value) {
        return new JSpinner(new SpinnerNumberModel(Math.min(value, cap), 0, cap, 1));
    }

    private void initComponents(final Campaign campaign) {
        getContentPane().setLayout(new BorderLayout(0, scaleForGUI(5)));
        int padding = scaleForGUI(10);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));

        getContentPane().add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.description", cap)),
              BorderLayout.NORTH);

        rowsPanel = new JPanel(new GridBagLayout());
        rebuildRows();
        JScrollPane scrollPane = new JScrollPane(rowsPanel);
        scrollPane.setPreferredSize(scaleForGUI(360, 260));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(buildFooter(campaign), BorderLayout.SOUTH);
    }

    private JPanel buildFooter(final Campaign campaign) {
        JPanel footer = new JPanel(new BorderLayout(0, scaleForGUI(5)));

        // Add-a-chassis row.
        JPanel addPanel = new JPanel();
        comboAddChassis = new JComboBox<>(getCandidateChassis(campaign).toArray(new String[0]));
        comboAddChassis.setEditable(true);
        comboAddChassis.setSelectedItem("");
        spnAddValue = new JSpinner(new SpinnerNumberModel(cap, 0, cap, 1));
        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.add"));
        btnAdd.addActionListener(evt -> addChassisRow());
        addPanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.addChassis")));
        addPanel.add(comboAddChassis);
        addPanel.add(spnAddValue);
        addPanel.add(btnAdd);
        footer.add(addPanel, BorderLayout.NORTH);

        // OK / Cancel.
        JPanel buttonPanel = new JPanel();
        JButton btnOK = new JButton(getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.confirm"));
        btnOK.addActionListener(evt -> {
            applyChanges();
            dispose();
        });
        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.cancel"));
        btnCancel.addActionListener(evt -> dispose());
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        footer.add(buttonPanel, BorderLayout.SOUTH);

        return footer;
    }

    private List<String> getCandidateChassis(final Campaign campaign) {
        TreeSet<String> chassis = new TreeSet<>(rowSpinners.keySet());
        for (Unit unit : campaign.getUnits()) {
            Entity unitEntity = unit.getEntity();
            if (unitEntity != null && unitEntity.isChassisFamiliarityEligible()) {
                chassis.add(unitEntity.getChassis());
            }
        }
        return new ArrayList<>(chassis);
    }

    private void addChassisRow() {
        Object selected = comboAddChassis.getSelectedItem();
        if (selected == null) {
            return;
        }
        String chassis = selected.toString().trim();
        if (chassis.isEmpty() || rowSpinners.containsKey(chassis)) {
            return;
        }
        rowSpinners.put(chassis, buildValueSpinner((int) spnAddValue.getValue()));
        rebuildRows();
    }

    private void rebuildRows() {
        rowsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        int padding = scaleForGUI(5);
        gbc.insets = new Insets(padding, padding, padding, padding);
        gbc.anchor = GridBagConstraints.WEST;

        int gridY = 0;
        for (String chassis : new TreeSet<>(rowSpinners.keySet())) {
            gbc.gridx = 0;
            gbc.gridy = gridY;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            rowsPanel.add(new JLabel(chassis), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            rowsPanel.add(rowSpinners.get(chassis), gbc);

            gbc.gridx = 2;
            JButton btnRemove = new JButton(getTextAt(RESOURCE_BUNDLE, "EditFamiliarityDialog.remove"));
            btnRemove.addActionListener(evt -> {
                rowSpinners.remove(chassis);
                rebuildRows();
            });
            rowsPanel.add(btnRemove, gbc);
            gridY++;
        }

        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        rowsPanel.add(Box.createGlue(), gbc);

        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private void applyChanges() {
        for (String chassis : initialChassis) {
            if (!rowSpinners.containsKey(chassis)) {
                person.setChassisFamiliarity(chassis, 0);
            }
        }
        for (Map.Entry<String, JSpinner> entry : rowSpinners.entrySet()) {
            person.setChassisFamiliarity(entry.getKey(), (int) entry.getValue().getValue());
        }
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }
}
