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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.LandMass;
import mekhq.campaign.universe.LifeForm;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Satellite;
import mekhq.campaign.universe.SourceableValue;
import mekhq.campaign.universe.enums.PlanetaryType;

/**
 * Form panel for editing low-risk static fields on a planetary system and its currently selected planet. Used by
 * {@link PlanetarySystemEditorDialog}.
 */
final class PlanetarySystemPropertiesPanel extends JPanel {
    private static final int PADDING = UIUtil.scaleForGUI(6);

    /** Sentinel placeholder for combo "no value". */
    private static final String NO_VALUE = "";

    private final ResourceBundle resources;
    private final Runnable onChange;
    private final java.util.function.Supplier<String> defaultSourceSupplier;
    private final java.util.function.Supplier<LocalDate> currentDateSupplier;

    private PlanetarySystem currentSystem;
    private Planet currentPlanet;
    private boolean populating;

    // Planet fields
    private final JTextField txtPlanetName = new JTextField(20);
    private final JComboBox<Object> cboPlanetType = makeNullableEnumCombo(PlanetaryType.values());
    private final JTextField txtGravity = new JTextField(8);
    private final JTextField txtDiameter = new JTextField(8);
    private final JTextField txtDayLength = new JTextField(8);
    private final JTextField txtYearLength = new JTextField(8);
    private final JTextField txtTemperature = new JTextField(8);
    private final JComboBox<Object> cboPressure =
          makeNullableEnumCombo(megamek.common.planetaryConditions.Atmosphere.values());
    private final JComboBox<Object> cboAtmosphere = makeNullableEnumCombo(Atmosphere.values());
    private final JTextField txtComposition = new JTextField(20);
    private final JTextField txtPercentWater = new JTextField(8);
    private final JComboBox<Object> cboLifeForm = makeNullableEnumCombo(LifeForm.values());
    private final JTextField txtSmallMoons = new JTextField(8);
    private final JCheckBox chkRing = new JCheckBox();
    private final JTextArea txtDescription = new JTextArea(4, 30);

    // Landmass editor
    private static final String[] SATELLITE_SIZES = { "small", "medium", "large", "giant" };
    private final LandMassTableModel landMassModel;
    private final JTable tblLandMasses;
    private final JButton btnAddLandMass;
    private final JButton btnRemoveLandMass;

    // Satellite editor
    private final SatelliteTableModel satelliteModel;
    private final JTable tblSatellites;
    private final JButton btnAddSatellite;
    private final JButton btnRemoveSatellite;

    // Source/version fields
    private final JTextField txtSource = new JTextField(20);
    private final JTextField txtVersion = new JTextField(10);

    private final JButton btnApply;
    private final JButton btnRevertForm;

    PlanetarySystemPropertiesPanel(ResourceBundle resources, java.util.function.Supplier<LocalDate> currentDateSupplier,
          java.util.function.Supplier<String> defaultSourceSupplier, Runnable onChange) {
        super(new BorderLayout(PADDING, PADDING));
        this.resources = resources;
        this.currentDateSupplier = currentDateSupplier;
        this.defaultSourceSupplier = defaultSourceSupplier;
        this.onChange = onChange;
        setName("pnlPlanetarySystemProperties");

        // Construct table models / JTables AFTER `resources` is set: JTable's constructor
        // invokes setModel → tableChanged → addColumn → getColumnName(...), which reads the
        // outer-class `resources` field. Initialising these as field initialisers (which run
        // before the constructor body) caused a NullPointerException when the dialog was
        // opened from the Interstellar Map / GM menu.
        this.landMassModel = new LandMassTableModel();
        this.tblLandMasses = new JTable(landMassModel);
        this.satelliteModel = new SatelliteTableModel();
        this.tblSatellites = new JTable(satelliteModel);

        txtPlanetName.setName("txtPlanetaryPropertyPlanetName");
        txtGravity.setName("txtPlanetaryPropertyGravity");
        tblLandMasses.setName("tblPlanetaryPropertyLandMasses");
        tblSatellites.setName("tblPlanetaryPropertySatellites");
        txtSource.setName("txtPlanetaryPropertySource");
        txtVersion.setName("txtPlanetaryPropertyVersion");

        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);

        btnAddLandMass = new MMButton("btnAddLandMass",
              resources.getString("PlanetarySystemEditorDialog.properties.landmass.add"),
              resources.getString("PlanetarySystemEditorDialog.properties.landmass.add.toolTipText"),
              evt -> addLandMass());
        btnRemoveLandMass = new MMButton("btnRemoveLandMass",
              resources.getString("PlanetarySystemEditorDialog.properties.landmass.remove"),
              resources.getString("PlanetarySystemEditorDialog.properties.landmass.remove.toolTipText"),
              evt -> removeSelectedLandMass());
        btnAddSatellite = new MMButton("btnAddSatellite",
              resources.getString("PlanetarySystemEditorDialog.properties.satellite.add"),
              resources.getString("PlanetarySystemEditorDialog.properties.satellite.add.toolTipText"),
              evt -> addSatellite());
        btnRemoveSatellite = new MMButton("btnRemoveSatellite",
              resources.getString("PlanetarySystemEditorDialog.properties.satellite.remove"),
              resources.getString("PlanetarySystemEditorDialog.properties.satellite.remove.toolTipText"),
              evt -> removeSelectedSatellite());

        tblLandMasses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLandMasses.setRowHeight(UIUtil.scaleForGUI(22));
        tblSatellites.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSatellites.setRowHeight(UIUtil.scaleForGUI(22));
        TableColumn sizeColumn = tblSatellites.getColumnModel().getColumn(SatelliteTableModel.COL_SIZE);
        JComboBox<String> sizeCombo = new JComboBox<>(SATELLITE_SIZES);
        sizeColumn.setCellEditor(new DefaultCellEditor(sizeCombo));
        tblLandMasses.getSelectionModel().addListSelectionListener(
              evt -> btnRemoveLandMass.setEnabled(tblLandMasses.getSelectedRow() >= 0));
        tblSatellites.getSelectionModel().addListSelectionListener(
              evt -> btnRemoveSatellite.setEnabled(tblSatellites.getSelectedRow() >= 0));

        JPanel formStack = new JPanel();
        formStack.setLayout(new javax.swing.BoxLayout(formStack, javax.swing.BoxLayout.Y_AXIS));
        addStacked(formStack, buildGameplaySection());
        addStacked(formStack, buildFlavorSection());
        addStacked(formStack, buildLandMassesSection());
        addStacked(formStack, buildSatellitesSection());
        add(new FastJScrollPane(formStack), BorderLayout.CENTER);

        JPanel sourcePanel = buildSourcePanel();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, PADDING, 0));
        btnApply = new MMButton("btnApplyPlanetaryProperties",
              resources.getString("PlanetarySystemEditorDialog.properties.apply"),
              resources.getString("PlanetarySystemEditorDialog.properties.apply.toolTipText"),
              evt -> applyChanges());
        btnRevertForm = new MMButton("btnReloadPlanetaryProperties",
              resources.getString("PlanetarySystemEditorDialog.properties.reload"),
              resources.getString("PlanetarySystemEditorDialog.properties.reload.toolTipText"),
              evt -> reloadForm());
        buttonPanel.add(btnRevertForm);
        buttonPanel.add(btnApply);

        JPanel south = new JPanel(new BorderLayout());
        south.add(sourcePanel, BorderLayout.CENTER);
        south.add(buttonPanel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        setComponentsEnabled(false);
    }

    private static JComboBox<Object> makeNullableEnumCombo(Object[] values) {
        Object[] withBlank = new Object[values.length + 1];
        withBlank[0] = NO_VALUE;
        System.arraycopy(values, 0, withBlank, 1, values.length);
        return new JComboBox<>(withBlank);
    }

    private JPanel buildGameplaySection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.properties.gameplaySection")));
        int row = 0;
        GridBagConstraints warningConstraints = new GridBagConstraints();
        warningConstraints.gridx = 0;
        warningConstraints.gridy = row++;
        warningConstraints.gridwidth = 2;
        warningConstraints.fill = GridBagConstraints.HORIZONTAL;
        warningConstraints.insets = new Insets(0, PADDING, PADDING, PADDING);
        JLabel warning = new JLabel(resources.getString("PlanetarySystemEditorDialog.properties.gameplayWarning"));
        warning.setForeground(java.awt.Color.RED);
        panel.add(warning, warningConstraints);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.gravity", txtGravity);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.temperature", txtTemperature);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.pressure", cboPressure);
        addRow(panel, row, "PlanetarySystemEditorDialog.properties.atmosphere", cboAtmosphere);
        return panel;
    }

    private JPanel buildFlavorSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.properties.flavorSection")));
        int row = 0;
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.planetName", txtPlanetName);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.planetType", cboPlanetType);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.diameter", txtDiameter);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.dayLength", txtDayLength);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.yearLength", txtYearLength);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.composition", txtComposition);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.percentWater", txtPercentWater);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.lifeForm", cboLifeForm);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.smallMoons", txtSmallMoons);
        addRow(panel, row++, "PlanetarySystemEditorDialog.properties.ring", chkRing);
        addRow(panel, row, "PlanetarySystemEditorDialog.properties.description", new FastJScrollPane(txtDescription));
        return panel;
    }

    private JPanel buildLandMassesSection() {
        JPanel panel = new JPanel(new BorderLayout(PADDING, PADDING));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.properties.landmassSection")));
        FastJScrollPane scroll = new FastJScrollPane(tblLandMasses);
        scroll.setPreferredSize(new Dimension(0, UIUtil.scaleForGUI(110)));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        buttons.add(btnAddLandMass);
        buttons.add(btnRemoveLandMass);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSatellitesSection() {
        JPanel panel = new JPanel(new BorderLayout(PADDING, PADDING));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.properties.satelliteSection")));
        FastJScrollPane scroll = new FastJScrollPane(tblSatellites);
        scroll.setPreferredSize(new Dimension(0, UIUtil.scaleForGUI(110)));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        buttons.add(btnAddSatellite);
        buttons.add(btnRemoveSatellite);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static void addStacked(JPanel container, JPanel section) {
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(section);
        container.add(javax.swing.Box.createVerticalStrut(PADDING));
    }

    private JPanel buildSourcePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(
              "PlanetarySystemEditorDialog.properties.sourceSection")));
        addRow(panel, 0, "PlanetarySystemEditorDialog.eventEditor.source", txtSource);
        addRow(panel, 1, "PlanetarySystemEditorDialog.eventEditor.version", txtVersion);
        return panel;
    }

    private void addRow(JPanel panel, int row, String labelKey, Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHEAST;
        labelConstraints.insets = new Insets(0, PADDING, PADDING, PADDING);
        panel.add(new JLabel(resources.getString(labelKey)), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(0, 0, PADDING, PADDING);
        panel.add(component, fieldConstraints);
    }

    void setSelection(PlanetarySystem system, Planet planet) {
        this.currentSystem = system;
        this.currentPlanet = planet;
        reloadForm();
        setComponentsEnabled(system != null);
    }

    private void reloadForm() {
        populating = true;
        try {
            if (currentPlanet == null) {
                clearPlanetFields();
                landMassModel.setLandMasses(List.of());
                satelliteModel.setSatellites(List.of());
            } else {
                LocalDate when = currentDateSupplier.get();
                txtPlanetName.setText(stringValue(currentPlanet.getSourcedName(when)));
                setComboValue(cboPlanetType, sourcedValue(currentPlanet.getSourcedPlanetType()));
                txtGravity.setText(numericValue(currentPlanet.getSourcedGravity()));
                txtDiameter.setText(numericValue(currentPlanet.getSourcedDiameter()));
                txtDayLength.setText(numericValue(currentPlanet.getSourcedDayLength(when)));
                txtYearLength.setText(numericValue(currentPlanet.getSourcedYearLength()));
                txtTemperature.setText(intValue(currentPlanet.getSourcedTemperature(when)));
                setComboValue(cboPressure, sourcedValue(currentPlanet.getSourcedPressure(when)));
                setComboValue(cboAtmosphere, sourcedValue(currentPlanet.getSourcedAtmosphere(when)));
                txtComposition.setText(stringValue(currentPlanet.getSourcedComposition(when)));
                txtPercentWater.setText(intValue(currentPlanet.getSourcedPercentWater(when)));
                setComboValue(cboLifeForm, sourcedValue(currentPlanet.getSourcedLifeForm(when)));
                txtSmallMoons.setText(intValue(currentPlanet.getSourcedSmallMoons()));
                chkRing.setSelected(Boolean.TRUE.equals(sourcedValue(currentPlanet.getSourcedRing())));
                txtDescription.setText(currentPlanet.getDescription() == null ? "" : currentPlanet.getDescription());
                txtDescription.setCaretPosition(0);
                List<LandMass> landMasses = currentPlanet.getLandMasses();
                landMassModel.setLandMasses(landMasses == null ? List.of() : landMasses);
                List<Satellite> satellites = currentPlanet.getSatellites();
                satelliteModel.setSatellites(satellites == null ? List.of() : satellites);
            }

            txtSource.setText(defaultSourceSupplier.get());
            txtVersion.setText("");
        } finally {
            populating = false;
        }
    }

    @SuppressWarnings("deprecation")
    private static <T> T sourcedValue(SourceableValue<T> source) {
        return source == null ? null : source.getValue();
    }

    private static String stringValue(SourceableValue<String> source) {
        Object value = sourcedValue(source);
        return value == null ? "" : String.valueOf(value);
    }

    private static String numericValue(SourceableValue<Double> source) {
        Double value = sourcedValue(source);
        return value == null ? "" : String.format(Locale.ROOT, "%s", value);
    }

    private static String intValue(SourceableValue<Integer> source) {
        Integer value = sourcedValue(source);
        return value == null ? "" : String.valueOf(value);
    }

    private static void setComboValue(JComboBox<Object> combo, Object value) {
        if (value == null) {
            combo.setSelectedItem(NO_VALUE);
            return;
        }
        combo.setSelectedItem(value);
    }

    private void clearPlanetFields() {
        txtPlanetName.setText("");
        cboPlanetType.setSelectedItem(NO_VALUE);
        txtGravity.setText("");
        txtDiameter.setText("");
        txtDayLength.setText("");
        txtYearLength.setText("");
        txtTemperature.setText("");
        cboPressure.setSelectedItem(NO_VALUE);
        cboAtmosphere.setSelectedItem(NO_VALUE);
        txtComposition.setText("");
        txtPercentWater.setText("");
        cboLifeForm.setSelectedItem(NO_VALUE);
        txtSmallMoons.setText("");
        chkRing.setSelected(false);
        txtDescription.setText("");
    }

    private void setComponentsEnabled(boolean enabled) {
        boolean planetEnabled = enabled && currentPlanet != null;
        txtPlanetName.setEnabled(planetEnabled);
        cboPlanetType.setEnabled(planetEnabled);
        txtGravity.setEnabled(planetEnabled);
        txtDiameter.setEnabled(planetEnabled);
        txtDayLength.setEnabled(planetEnabled);
        txtYearLength.setEnabled(planetEnabled);
        txtTemperature.setEnabled(planetEnabled);
        cboPressure.setEnabled(planetEnabled);
        cboAtmosphere.setEnabled(planetEnabled);
        txtComposition.setEnabled(planetEnabled);
        txtPercentWater.setEnabled(planetEnabled);
        cboLifeForm.setEnabled(planetEnabled);
        txtSmallMoons.setEnabled(planetEnabled);
        chkRing.setEnabled(planetEnabled);
        txtDescription.setEnabled(planetEnabled);
        tblLandMasses.setEnabled(planetEnabled);
        tblSatellites.setEnabled(planetEnabled);
        btnAddLandMass.setEnabled(planetEnabled);
        btnRemoveLandMass.setEnabled(planetEnabled && tblLandMasses.getSelectedRow() >= 0);
        btnAddSatellite.setEnabled(planetEnabled);
        btnRemoveSatellite.setEnabled(planetEnabled && tblSatellites.getSelectedRow() >= 0);
        txtSource.setEnabled(enabled);
        txtVersion.setEnabled(enabled);
        btnApply.setEnabled(enabled);
        btnRevertForm.setEnabled(enabled);
    }

    private void applyChanges() {
        if (populating || (currentSystem == null)) {
            return;
        }

        boolean changed = false;
        try {
            if (currentPlanet != null) {
                LocalDate when = currentDateSupplier.get();
                String source = blankToNull(txtSource.getText());
                String version = blankToNull(txtVersion.getText());
                changed |= applyStringSourceable(currentPlanet.getSourcedName(when), txtPlanetName.getText(),
                      currentPlanet::setSourcedName, source, version);
                changed |= applyEnumSourceable(currentPlanet.getSourcedPlanetType(), cboPlanetType.getSelectedItem(),
                      currentPlanet::setSourcedPlanetType, source, version);
                changed |= applyDoubleSourceable(currentPlanet.getSourcedGravity(), txtGravity.getText(),
                      currentPlanet::setSourcedGravity, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.gravity", 0.0, 10.0);
                changed |= applyDoubleSourceable(currentPlanet.getSourcedDiameter(), txtDiameter.getText(),
                      currentPlanet::setSourcedDiameter, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.diameter", 1.0, 1_000_000.0);
                changed |= applyDoubleSourceable(currentPlanet.getSourcedDayLength(when), txtDayLength.getText(),
                      currentPlanet::setSourcedDayLength, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.dayLength", 0.001, 100_000.0);
                changed |= applyDoubleSourceable(currentPlanet.getSourcedYearLength(), txtYearLength.getText(),
                      currentPlanet::setSourcedYearLength, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.yearLength", 0.001, 100_000.0);
                changed |= applyIntegerSourceable(currentPlanet.getSourcedTemperature(when), txtTemperature.getText(),
                      currentPlanet::setSourcedTemperature, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.temperature", -300, 1000);
                changed |= applyEnumSourceable(currentPlanet.getSourcedPressure(when), cboPressure.getSelectedItem(),
                      currentPlanet::setSourcedPressure, source, version);
                changed |= applyEnumSourceable(currentPlanet.getSourcedAtmosphere(when),
                      cboAtmosphere.getSelectedItem(), currentPlanet::setSourcedAtmosphere, source, version);
                changed |= applyStringSourceable(currentPlanet.getSourcedComposition(when), txtComposition.getText(),
                      currentPlanet::setSourcedComposition, source, version);
                Integer percentWater = parsePercent(txtPercentWater.getText());
                changed |= applyValueSourceable(sourcedValue(currentPlanet.getSourcedPercentWater(when)), percentWater,
                      currentPlanet::setSourcedPercentWater, source, version);
                changed |= applyEnumSourceable(currentPlanet.getSourcedLifeForm(when), cboLifeForm.getSelectedItem(),
                      currentPlanet::setSourcedLifeForm, source, version);
                changed |= applyIntegerSourceable(currentPlanet.getSourcedSmallMoons(), txtSmallMoons.getText(),
                      currentPlanet::setSourcedSmallMoons, source, version,
                      "PlanetarySystemEditorDialog.properties.invalid.smallMoons", 0, 1000);
                Boolean ringValue = chkRing.isSelected() ? Boolean.TRUE : null;
                changed |= applyValueSourceable(sourcedValue(currentPlanet.getSourcedRing()), ringValue,
                      currentPlanet::setSourcedRing, source, version);
                String desc = blankToNull(txtDescription.getText());
                if (!java.util.Objects.equals(desc, currentPlanet.getDescription())) {
                    currentPlanet.setDescription(desc);
                    changed = true;
                }
            }

            if (changed && (onChange != null)) {
                onChange.run();
            }
            if (!changed) {
                JOptionPane.showMessageDialog(this, resources.getString(
                            "PlanetarySystemEditorDialog.properties.noChanges"),
                      resources.getString("PlanetarySystemEditorDialog.properties.apply"),
                      JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                  resources.getString("PlanetarySystemEditorDialog.properties.invalid.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean applyStringSourceable(SourceableValue<String> existing, String input,
          java.util.function.Consumer<SourceableValue<String>> setter, String source, String version) {
        String parsed = blankToNull(input);
        return applyValueSourceable(existing == null ? null : existing.getValue(), parsed, setter, source, version);
    }

    private boolean applyDoubleSourceable(SourceableValue<Double> existing, String input,
          java.util.function.Consumer<SourceableValue<Double>> setter, String source, String version,
          String errorKey, double min, double max) {
        Double parsed = parseDouble(input, errorKey, min, max);
        return applyValueSourceable(existing == null ? null : existing.getValue(), parsed, setter, source, version);
    }

    private boolean applyIntegerSourceable(SourceableValue<Integer> existing, String input,
          java.util.function.Consumer<SourceableValue<Integer>> setter, String source, String version,
          String errorKey, int min, int max) {
        Integer parsed = parseInteger(input, errorKey, min, max);
        return applyValueSourceable(existing == null ? null : existing.getValue(), parsed, setter, source, version);
    }

    private <T extends Enum<?>> boolean applyEnumSourceable(SourceableValue<T> existing, Object selected,
          java.util.function.Consumer<SourceableValue<T>> setter, String source, String version) {
        @SuppressWarnings("unchecked")
        T parsed = (selected == null) || NO_VALUE.equals(selected) ? null : (T) selected;
        return applyValueSourceable(existing == null ? null : existing.getValue(), parsed, setter, source, version);
    }

    private <T> boolean applyValueSourceable(T existingValue, T parsedValue,
          java.util.function.Consumer<SourceableValue<T>> setter, String source, String version) {
        if (java.util.Objects.equals(existingValue, parsedValue)) {
            return false;
        }
        setter.accept(parsedValue == null ? null : SourceableValue.of(source, version, parsedValue));
        return true;
    }

    private Double parseDouble(String text, String errorKey, double min, double max) {
        String value = blankToNull(text);
        if (value == null) {
            return null;
        }
        double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(errorKey), value), ex);
        }
        if (!Double.isFinite(parsed) || (parsed < min) || (parsed > max)) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(errorKey), value));
        }
        return parsed;
    }

    private Integer parseInteger(String text, String errorKey) {
        return parseInteger(text, errorKey, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private Integer parseInteger(String text, String errorKey, int min, int max) {
        String value = blankToNull(text);
        if (value == null) {
            return null;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(errorKey), value), ex);
        }
        if ((parsed < min) || (parsed > max)) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(errorKey), value));
        }
        return parsed;
    }

    private Integer parsePercent(String text) {
        return parseInteger(text, "PlanetarySystemEditorDialog.properties.invalid.percentWater", 0, 100);
    }

    private static String blankToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SourceableValue<String> currentSource(String value) {
        if (value == null) {
            return null;
        }
        String source = blankToNull(txtSource.getText());
        String version = blankToNull(txtVersion.getText());
        return SourceableValue.of(source, version, value);
    }

    private void notifyChange() {
        if (!populating && (onChange != null)) {
            onChange.run();
        }
    }

    private List<LandMass> mutableLandMasses() {
        List<LandMass> existing = currentPlanet.getLandMasses();
        return existing == null ? new ArrayList<>() : new ArrayList<>(existing);
    }

    private List<Satellite> mutableSatellites() {
        List<Satellite> existing = currentPlanet.getSatellites();
        return existing == null ? new ArrayList<>() : new ArrayList<>(existing);
    }

    private void addLandMass() {
        if (currentPlanet == null) {
            return;
        }
        List<LandMass> updated = mutableLandMasses();
        LandMass landMass = new LandMass();
        landMass.setSourcedName(currentSource(resources.getString(
              "PlanetarySystemEditorDialog.properties.landmass.defaultName")));
        updated.add(landMass);
        currentPlanet.setLandMasses(updated);
        landMassModel.setLandMasses(updated);
        int row = updated.size() - 1;
        tblLandMasses.setRowSelectionInterval(row, row);
        tblLandMasses.editCellAt(row, LandMassTableModel.COL_NAME);
        notifyChange();
    }

    private void removeSelectedLandMass() {
        int row = tblLandMasses.getSelectedRow();
        if ((currentPlanet == null) || (row < 0)) {
            return;
        }
        if (tblLandMasses.isEditing()) {
            tblLandMasses.getCellEditor().cancelCellEditing();
        }
        List<LandMass> updated = mutableLandMasses();
        if (row >= updated.size()) {
            return;
        }
        updated.remove(row);
        currentPlanet.setLandMasses(updated);
        landMassModel.setLandMasses(updated);
        notifyChange();
    }

    private void addSatellite() {
        if (currentPlanet == null) {
            return;
        }
        List<Satellite> updated = mutableSatellites();
        Satellite satellite = new Satellite();
        satellite.setSourcedName(currentSource(resources.getString(
              "PlanetarySystemEditorDialog.properties.satellite.defaultName")));
        satellite.setSourcedSize(currentSource("medium"));
        updated.add(satellite);
        currentPlanet.setSatellites(updated);
        satelliteModel.setSatellites(updated);
        int row = updated.size() - 1;
        tblSatellites.setRowSelectionInterval(row, row);
        tblSatellites.editCellAt(row, SatelliteTableModel.COL_NAME);
        notifyChange();
    }

    private void removeSelectedSatellite() {
        int row = tblSatellites.getSelectedRow();
        if ((currentPlanet == null) || (row < 0)) {
            return;
        }
        if (tblSatellites.isEditing()) {
            tblSatellites.getCellEditor().cancelCellEditing();
        }
        List<Satellite> updated = mutableSatellites();
        if (row >= updated.size()) {
            return;
        }
        updated.remove(row);
        currentPlanet.setSatellites(updated);
        satelliteModel.setSatellites(updated);
        notifyChange();
    }

    private final class LandMassTableModel extends AbstractTableModel {
        static final int COL_NAME = 0;
        static final int COL_CAPITAL = 1;

        private List<LandMass> data = List.of();

        void setLandMasses(List<LandMass> landMasses) {
            this.data = new ArrayList<>(landMasses);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return resources.getString(column == COL_NAME
                                             ? "PlanetarySystemEditorDialog.properties.landmass.name"
                                             : "PlanetarySystemEditorDialog.properties.landmass.capital");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return currentPlanet != null;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((rowIndex < 0) || (rowIndex >= data.size())) {
                return "";
            }
            LandMass landMass = data.get(rowIndex);
            if (landMass == null) {
                return "";
            }
            SourceableValue<String> source = (columnIndex == COL_NAME)
                                                   ? landMass.getSourcedName()
                                                   : landMass.getSourcedCapital();
            String value = (source == null) ? null : source.getValue();
            return value == null ? "" : value;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if ((currentPlanet == null) || (rowIndex < 0) || (rowIndex >= data.size())) {
                return;
            }
            String text = (value == null) ? null : blankToNull(value.toString());
            LandMass landMass = data.get(rowIndex);
            SourceableValue<String> wrapped = (text == null) ? null : currentSource(text);
            if (columnIndex == COL_NAME) {
                landMass.setSourcedName(wrapped);
            } else {
                landMass.setSourcedCapital(wrapped);
            }
            currentPlanet.setLandMasses(data);
            fireTableRowsUpdated(rowIndex, rowIndex);
            notifyChange();
        }
    }

    private final class SatelliteTableModel extends AbstractTableModel {
        static final int COL_NAME = 0;
        static final int COL_SIZE = 1;

        private List<Satellite> data = List.of();

        void setSatellites(List<Satellite> satellites) {
            this.data = new ArrayList<>(satellites);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return resources.getString(column == COL_NAME
                                             ? "PlanetarySystemEditorDialog.properties.satellite.name"
                                             : "PlanetarySystemEditorDialog.properties.satellite.size");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return currentPlanet != null;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if ((rowIndex < 0) || (rowIndex >= data.size())) {
                return "";
            }
            Satellite satellite = data.get(rowIndex);
            if (satellite == null) {
                return "";
            }
            if (columnIndex == COL_NAME) {
                SourceableValue<String> name = satellite.getSourcedName();
                String value = (name == null) ? null : name.getValue();
                return value == null ? "" : value;
            }
            // size
            SourceableValue<String> size = satellite.getSourcedSize();
            String value = (size == null) ? null : size.getValue();
            return value == null ? "medium" : value;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if ((currentPlanet == null) || (rowIndex < 0) || (rowIndex >= data.size())) {
                return;
            }
            String text = (value == null) ? null : blankToNull(value.toString());
            Satellite satellite = data.get(rowIndex);
            if (columnIndex == COL_NAME) {
                satellite.setSourcedName(text == null ? null : currentSource(text));
            } else {
                // Size cannot be null - default to "medium" if cleared.
                String size = (text == null) ? "medium" : text.toLowerCase(Locale.ROOT);
                satellite.setSourcedSize(currentSource(size));
            }
            currentPlanet.setSatellites(data);
            fireTableRowsUpdated(rowIndex, rowIndex);
            notifyChange();
        }
    }
}
