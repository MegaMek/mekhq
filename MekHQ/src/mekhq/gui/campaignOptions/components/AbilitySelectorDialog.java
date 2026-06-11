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
package mekhq.gui.campaignOptions.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * A compact, searchable multi-select popup for choosing a set of special abilities by name.
 *
 * <p>This replaces the original wall-of-checkboxes grid with a filter field over a single scrollable checklist, so a
 * user can type to narrow the list (for example "Gunnery") instead of scanning every ability.</p>
 */
public class AbilitySelectorDialog extends JDialog {
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final List<String> abilityNames = new ArrayList<>();
    private final JPanel checklistPanel = new JPanel();

    private Vector<String> selected;
    private boolean cancelled;

    /**
     * Creates a searchable ability selector seeded with the current selection.
     *
     * @param parent           the window to center on, may be {@code null}
     * @param title            the dialog title shown to the user
     * @param currentSelection the ability lookup names that should start checked
     * @param allSPAs          every selectable special ability, keyed by lookup name
     */
    public AbilitySelectorDialog(@Nullable Window parent, @Nonnull String title,
          @Nonnull Collection<String> currentSelection, @Nonnull Map<String, SpecialAbility> allSPAs) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.selected = new Vector<>(currentSelection);
        this.cancelled = false;
        initComponents(currentSelection, allSPAs);
        setLocationRelativeTo(parent);
    }

    private void initComponents(Collection<String> currentSelection, Map<String, SpecialAbility> allSPAs) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        JTextField filterField = new JTextField();
        filterField.putClientProperty("JTextField.placeholderText", "Filter…");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }
        });
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(4),
              UIUtil.scaleForGUI(8)));
        filterPanel.add(filterField, BorderLayout.CENTER);
        getContentPane().add(filterPanel, BorderLayout.NORTH);

        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));
        checklistPanel.setBorder(BorderFactory.createEmptyBorder(0,
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8)));

        for (SpecialAbility spa : allSPAs.values()
                                        .stream()
                                        .sorted((a, b) -> new NaturalOrderComparator().compare(a.getDisplayName(),
                                              b.getDisplayName()))
                                        .toList()) {
            JCheckBox checkBox = new JCheckBox(spa.getDisplayName());
            checkBox.setSelected(currentSelection.contains(spa.getName()));
            checkBoxes.add(checkBox);
            abilityNames.add(spa.getName());
            checklistPanel.add(checkBox);
        }

        getContentPane().add(new FastJScrollPane(checklistPanel), BorderLayout.CENTER);

        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(evt -> confirm());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(evt -> cancel());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(UIUtil.scaleForGUI(360), UIUtil.scaleForGUI(480)));
        pack();
    }

    private void applyFilter(String filter) {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        for (JCheckBox checkBox : checkBoxes) {
            checkBox.setVisible(needle.isEmpty() || checkBox.getText().toLowerCase(Locale.ROOT).contains(needle));
        }
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private void confirm() {
        selected = new Vector<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selected.add(abilityNames.get(i));
            }
        }
        dispose();
    }

    private void cancel() {
        cancelled = true;
        dispose();
    }

    /**
     * @return the chosen ability lookup names; only meaningful when {@link #wasCancelled()} is {@code false}
     */
    public @Nonnull Vector<String> getSelected() {
        return selected;
    }

    /**
     * @return {@code true} if the user dismissed the dialog without confirming
     */
    public boolean wasCancelled() {
        return cancelled;
    }
}
