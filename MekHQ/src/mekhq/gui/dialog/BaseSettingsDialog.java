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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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

import megamek.client.ui.util.UIUtil;
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

    private static final String RESOURCE_BUNDLE = "mekhq.resources.BaseSettingsDialog";

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
        setTitle(existingBase == null
                       ? getTextAt(RESOURCE_BUNDLE, "title.newBase.text")
                       : getTextAt(RESOURCE_BUNDLE, "title.editBase.text"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        LocalDate today = campaign.getLocalDate();

        txtDisplayName = new JTextField(30);
        txtDisplayType = new JTextField(30);

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
            Object selected = cboSystem.getSelectedItem();
            planetBaseModel.removeAllElements();
            planetFilterModel.setFilter(null);
            clearEditorText(cboPlanet);

            if (!(selected instanceof PlanetarySystem system)) {
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
            PlanetarySystem system = existingBase.getCurrentSystem();
            if (system != null) {
                cboSystem.setSelectedItem(system);
                if (existingBase.getPlanetId() != null) {
                    Planet planet = system.getPlanetById(existingBase.getPlanetId());
                    if (planet != null) {
                        cboPlanet.setSelectedItem(planet);
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

        JButton btnOk = new JButton(getTextAt(RESOURCE_BUNDLE, "button.ok.text"));
        btnOk.addActionListener(e -> onOk());
        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "button.cancel.text"));
        btnCancel.addActionListener(e -> dispose());

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        int pad = UIUtil.scaleForGUI(4);
        gbc.insets = new Insets(pad, pad, pad, pad);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "label.displayName.text")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(txtDisplayName, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "label.displayType.text")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(txtDisplayType, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "label.system.text")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        content.add(cboSystem, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "label.planet.text")), gbc);
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
        // Must be declared before the editor and document listener so both closures share it.
        // setItem sets this flag before calling editorField.setText() to prevent the document
        // listener from re-triggering filterModel.setFilter() while the document lock is held,
        // which would cause "Attempt to mutate in notification" (IllegalStateException).
        boolean[] updating = { false };
        combo.setEditor(new ComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return editorField;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setItem(Object anObject) {
                // If updating is already true, onTextChanged is running (we're inside a document
                // notification). The document write lock is held — any setText() call here would
                // throw "Attempt to mutate in notification". The user's typed text is already
                // correct, so there is nothing to update.
                if (updating[0]) {
                    return;
                }
                updating[0] = true;
                try {
                    if (anObject == null) {
                        editorField.setText("");
                    } else if (anObject instanceof String string) {
                        editorField.setText(string);
                    } else {
                        editorField.setText(displayName.apply((T) anObject));
                    }
                } finally {
                    updating[0] = false;
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object getItem() {
                // When focus leaves an editable combo, Swing calls getItem() and compares the
                // result with the currently selected object. If they differ it calls
                // setSelectedItem(getItem()), replacing a real T in the model with a plain
                // String — which then breaks any instanceof T check in action listeners.
                // Return the real selected item whenever the editor text matches its display
                // name, so Swing sees no change and leaves the model alone.
                String text = editorField.getText();
                Object current = combo.getModel().getSelectedItem();
                if (current != null && !(current instanceof String)) {
                    if (displayName.apply((T) current).equals(text)) {
                        return current;
                    }
                }
                return text;
            }

            @Override
            public void selectAll() {
                editorField.selectAll();
            }

            @Override
            public void addActionListener(ActionListener listener) {
                editorField.addActionListener(listener);
            }

            @Override
            public void removeActionListener(ActionListener listener) {
                editorField.removeActionListener(listener);
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

        // When the editor loses focus, restore the text to the selected item's display name
        // (if one is selected). This handles the case where the user typed a partial search,
        // did not pick from the popup, and clicked away — the editor snaps back to the real
        // selection so the combo always looks consistent.
        editorField.addFocusListener(new FocusAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void focusLost(FocusEvent evt) {
                if (evt.isTemporary()) {
                    return; // popup opening/closing — do not interfere
                }
                Object current = combo.getModel().getSelectedItem();
                if (current != null && !(current instanceof String)) {
                    String expected = displayName.apply((T) current);
                    if (!expected.equals(editorField.getText())) {
                        updating[0] = true;
                        try {
                            editorField.setText(expected);
                        } finally {
                            updating[0] = false;
                        }
                    }
                } else if (current == null) {
                    if (!editorField.getText().isEmpty()) {
                        updating[0] = true;
                        try {
                            editorField.setText("");
                        } finally {
                            updating[0] = false;
                        }
                    }
                }
            }
        });

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
        if (editor instanceof JTextField textField) {
            textField.setText("");
        }
    }

    private void onOk() {
        String displayName = txtDisplayName.getText().trim();
        if (displayName.isEmpty()) {
            txtDisplayName.requestFocusInWindow();
            return;
        }

        Object selectedSystem = cboSystem.getSelectedItem();
        if (!(selectedSystem instanceof PlanetarySystem system)) {
            cboSystem.requestFocusInWindow();
            return;
        }

        Planet selectedPlanet = cboPlanet.getSelectedItem() instanceof Planet p ? p : null;

        FixedLocation fixedLocation = new FixedLocation(system);
        PlayerBase base = existingBase != null ? existingBase : new PlayerBase(fixedLocation);
        if (existingBase != null) {
            base.setParent(fixedLocation);
        }

        base.setDisplayName(displayName);
        String displayType = txtDisplayType.getText().trim();
        base.setDisplayType(displayType.isEmpty() ? null : displayType);
        base.setPlanetId(selectedPlanet != null ? selectedPlanet.getId() : null);

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
