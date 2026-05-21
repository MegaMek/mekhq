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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.model.FilterableComboBoxModel;

/**
 * Dialog for creating or editing a {@link PlayerBase}.
 *
 * <p>Both the System and Planet dropdowns support type-to-search: typing filters the visible
 * options to those whose display name contains the typed text (case-insensitive). The Planet dropdown is only enabled
 * once a System is selected; changing the System resets the Planet.</p>
 */
public class BaseSettingsDialog extends JDialog {

    private final Campaign campaign;
    private final PlayerBase existingBase;

    private JTextField txtDisplayName;
    private JTextField txtDisplayType;
    private JComboBox<PlanetarySystem> cboSystem;
    private JComboBox<Planet> cboPlanet;
    private DefaultComboBoxModel<Planet> planetBaseModel;

    private PlayerBase result;

    public BaseSettingsDialog(JFrame parent, Campaign campaign) {
        this(parent, campaign, null, null, null);
    }

    public BaseSettingsDialog(JFrame parent, Campaign campaign,
          @Nullable PlanetarySystem defaultSystem,
          @Nullable Planet defaultPlanet) {
        this(parent, campaign, defaultSystem, defaultPlanet, null);
    }

    public BaseSettingsDialog(JFrame parent, Campaign campaign,
          @Nullable PlayerBase existingBase) {
        this(parent, campaign, null, null, existingBase);
    }

    private BaseSettingsDialog(JFrame parent, Campaign campaign,
          @Nullable PlanetarySystem defaultSystem,
          @Nullable Planet defaultPlanet,
          @Nullable PlayerBase existingBase) {
        super(parent, true);
        this.campaign = campaign;
        this.existingBase = existingBase;
        initComponents(defaultSystem, defaultPlanet);
        setLocationRelativeTo(parent);
    }

    private void initComponents(@Nullable PlanetarySystem defaultSystem,
          @Nullable Planet defaultPlanet) {
        setTitle(existingBase == null ? "New Base" : "Edit Base");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        LocalDate today = campaign.getLocalDate();

        txtDisplayName = new JTextField(20);
        txtDisplayType = new JTextField(20);

        // System combo — all systems sorted alphabetically, with type-to-search
        List<PlanetarySystem> systems = new ArrayList<>(campaign.getSystems());
        systems.sort(Comparator.comparing(s -> s.getPrintableName(today)));
        DefaultComboBoxModel<PlanetarySystem> systemBaseModel = new DefaultComboBoxModel<>();
        systems.forEach(systemBaseModel::addElement);
        FilterableComboBoxModel<PlanetarySystem> systemFilterModel =
              new FilterableComboBoxModel<>(systemBaseModel);

        cboSystem = new JComboBox<>(systemFilterModel);
        installTypeToSearch(cboSystem, systemFilterModel,
              s -> s.getPrintableName(today));

        // Planet combo — populated when system is selected, with type-to-search
        planetBaseModel = new DefaultComboBoxModel<>();
        FilterableComboBoxModel<Planet> planetFilterModel =
              new FilterableComboBoxModel<>(planetBaseModel);

        cboPlanet = new JComboBox<>(planetFilterModel);
        cboPlanet.setEnabled(false);
        installTypeToSearch(cboPlanet, planetFilterModel,
              p -> p.getPrintableName(today));

        // Cascade: selecting a system repopulates the planet combo
        cboSystem.addActionListener(e -> {
            Object sel = cboSystem.getSelectedItem();
            planetBaseModel.removeAllElements();
            planetFilterModel.setFilter(null);
            clearEditorText(cboPlanet);

            if (!(sel instanceof PlanetarySystem system)) {
                cboPlanet.setEnabled(false);
                return;
            }

            List<Planet> planets = new ArrayList<>(system.getPlanets());
            planets.sort(Comparator.comparing(p -> p.getPrintableName(today)));
            planets.forEach(planetBaseModel::addElement);
            cboPlanet.setEnabled(!planets.isEmpty());

            Planet primary = system.getPrimaryPlanet();
            if (primary != null) {
                cboPlanet.setSelectedItem(primary);
            }
        });

        // Pre-populate from existing base or supplied defaults
        if (existingBase != null) {
            txtDisplayName.setText(existingBase.getDisplayName());
            if (existingBase.getDisplayType() != null) {
                txtDisplayType.setText(existingBase.getDisplayType());
            }
            PlanetarySystem sys = existingBase.getCurrentSystem();
            if (sys != null) {
                cboSystem.setSelectedItem(sys);
                if (existingBase.getPlanetId() != null) {
                    Planet p = sys.getPlanetById(existingBase.getPlanetId());
                    if (p != null) {
                        cboPlanet.setSelectedItem(p);
                    }
                }
            }
        } else if (defaultSystem != null) {
            cboSystem.setSelectedItem(defaultSystem);
            if (defaultPlanet != null) {
                cboPlanet.setSelectedItem(defaultPlanet);
            }
        } else {
            cboSystem.setSelectedIndex(-1);
        }

        JButton btnOk = new JButton("OK");
        btnOk.addActionListener(e -> onOk());
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("Display Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(txtDisplayName, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("Display Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(txtDisplayType, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("System:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(cboSystem, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("Planet:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(cboPlanet, gbc);
        row++;

        JPanel buttons = new JPanel();
        buttons.add(btnOk);
        buttons.add(btnCancel);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        content.add(buttons, gbc);

        setContentPane(content);
        pack();
    }

    /**
     * Installs type-to-search behaviour on {@code combo}.
     *
     * <ul>
     *   <li>Makes the combo editable with a custom editor that renders items via
     *       {@code displayName} instead of {@link Object#toString()}.</li>
     *   <li>Attaches a {@link DocumentListener} that filters the model as the user types and
     *       auto-shows the popup.</li>
     * </ul>
     */
    private static <T> void installTypeToSearch(JComboBox<T> combo,
          FilterableComboBoxModel<T> filterModel,
          Function<T, String> displayName) {

        combo.setEditable(true);

        JTextField editorField = new JTextField();
        combo.setEditor(new ComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return editorField;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setItem(Object anObject) {
                if (anObject == null) {
                    editorField.setText("");
                } else if (anObject instanceof String s) {
                    editorField.setText(s);
                } else {
                    editorField.setText(displayName.apply((T) anObject));
                }
            }

            @Override
            public Object getItem() {
                return editorField.getText();
            }

            @Override
            public void selectAll() {
                editorField.selectAll();
            }

            @Override
            public void addActionListener(java.awt.event.ActionListener l) {
                editorField.addActionListener(l);
            }

            @Override
            public void removeActionListener(java.awt.event.ActionListener l) {
                editorField.removeActionListener(l);
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && !(value instanceof String)) {
                    setText(displayName.apply((T) value));
                }
                return this;
            }
        });

        boolean[] updating = { false };
        editorField.getDocument().addDocumentListener(new DocumentListener() {
            private void onTextChanged() {
                if (updating[0]) {
                    return;
                }
                updating[0] = true;
                try {
                    String text = editorField.getText().toLowerCase(Locale.ROOT);
                    filterModel.setFilter(text.isEmpty() ?
                                                null
                                                :
                                                item -> displayName.apply(item)
                                                              .toLowerCase(Locale.ROOT)
                                                              .contains(text));
                } finally {
                    updating[0] = false;
                }
                SwingUtilities.invokeLater(() -> {
                    if (!combo.isPopupVisible() && filterModel.getSize() > 0
                              && !editorField.getText().isEmpty()) {
                        combo.showPopup();
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    /** Clears the editor text field of an editable combo without firing a selection event. */
    private static void clearEditorText(JComboBox<?> combo) {
        Component editor = combo.getEditor().getEditorComponent();
        if (editor instanceof JTextField tf) {
            tf.setText("");
        }
    }

    private void onOk() {
        String displayName = txtDisplayName.getText().trim();
        if (displayName.isEmpty()) {
            txtDisplayName.requestFocusInWindow();
            return;
        }

        Object systemSel = cboSystem.getSelectedItem();
        if (!(systemSel instanceof PlanetarySystem system)) {
            cboSystem.requestFocusInWindow();
            return;
        }

        Object planetSel = cboPlanet.getSelectedItem();
        Planet planet = planetSel instanceof Planet p ? p : null;

        FixedLocation fixedLocation = new FixedLocation(system);
        PlayerBase base = existingBase != null ? existingBase : new PlayerBase(fixedLocation);
        if (existingBase != null) {
            base.setParent(fixedLocation);
        }

        base.setDisplayName(displayName);
        String displayType = txtDisplayType.getText().trim();
        base.setDisplayType(displayType.isEmpty() ? null : displayType);
        base.setPlanetId(planet != null ? planet.getId() : null);

        if (existingBase == null) {
            campaign.addPlayerBase(base);
        }

        result = base;
        dispose();
    }

    public Optional<PlayerBase> getResult() {
        return Optional.ofNullable(result);
    }
}
