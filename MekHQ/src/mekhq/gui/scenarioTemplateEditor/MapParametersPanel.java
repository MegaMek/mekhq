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
package mekhq.gui.scenarioTemplateEditor;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import megamek.codeUtilities.MathUtility;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;

/**
 * Editor panel for a scenario template's {@link ScenarioMapParameters}: base and scaled dimensions, additional map
 * sheets, rotation and AtB sizing, the map-location choice, and the allowed terrain types.
 *
 * <p>Extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). The available terrain types are supplied by the
 * caller rather than read from {@code StratConBiomeManifest}, so the panel has no data-file dependency and can be
 * exercised headlessly. {@link #load}/{@link #validateInput}/{@link #writeInto} are the testable contract.
 */
public class MapParametersPanel extends JPanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private final Dimension spinnerSize = new Dimension(55, 25);

    private final JTextField txtBaseWidth = new JTextField(4);
    private final JTextField txtBaseHeight = new JTextField(4);
    private final JTextField txtWidthIncrement = new JTextField(4);
    private final JTextField txtHeightIncrement = new JTextField(4);
    private final JSpinner spnAdditionalMapSheetWide = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JSpinner spnAdditionalMapSheetTall = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JCheckBox chkAllowRotation = new JCheckBox();
    private final JCheckBox chkUseAtBSizing = new JCheckBox();
    private final JRadioButton btnAllowAllMapTypes = new JRadioButton(
          getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.anyGroundMap.label"));
    private final JRadioButton btnUseSpaceMap = new JRadioButton(
          getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.useSpaceMap.label"));
    private final JRadioButton btnUseLowAtmosphereMap = new JRadioButton(
          getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.useLowAtmoMap.label"));
    private final JRadioButton btnUseSpecificMapTypes = new JRadioButton(
          getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.specificMapTypes.label"));
    private final JList<String> lstAllowedTerrainTypes = new JList<>();

    /**
     * @param availableTerrainTypes the terrain-type keys to offer, in display order (supplied by the caller so this
     *                              panel does not depend on the biome manifest data files)
     */
    public MapParametersPanel(List<String> availableTerrainTypes) {
        super(new GridBagLayout());
        DefaultListModel<String> terrainModel = new DefaultListModel<>();
        availableTerrainTypes.forEach(terrainModel::addElement);
        lstAllowedTerrainTypes.setModel(terrainModel);
        initComponents();
    }

    private void initComponents() {
        spnAdditionalMapSheetWide.setPreferredSize(spinnerSize);
        spnAdditionalMapSheetTall.setPreferredSize(spinnerSize);

        chkUseAtBSizing.addItemListener(evt -> atbSizingChanged());
        btnAllowAllMapTypes.addItemListener(evt -> mapTypeChanged());
        btnUseSpaceMap.addItemListener(evt -> mapTypeChanged());
        btnUseLowAtmosphereMap.addItemListener(evt -> mapTypeChanged());
        btnUseSpecificMapTypes.addItemListener(evt -> mapTypeChanged());

        ButtonGroup mapTypeGroup = new ButtonGroup();
        mapTypeGroup.add(btnAllowAllMapTypes);
        mapTypeGroup.add(btnUseSpaceMap);
        mapTypeGroup.add(btnUseLowAtmosphereMap);
        mapTypeGroup.add(btnUseSpecificMapTypes);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridwidth = 1;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.title")), constraints);

        addLabeledField(constraints, "MapParametersPanel.baseWidth.label", txtBaseWidth);
        addLabeledField(constraints, "MapParametersPanel.baseHeight.label", txtBaseHeight);
        addLabeledField(constraints, "MapParametersPanel.widthIncrement.label", txtWidthIncrement);
        addLabeledField(constraints, "MapParametersPanel.heightIncrement.label", txtHeightIncrement);
        addLabeledSpinner(constraints, "MapParametersPanel.additionalMapSheetWide.label",
              "MapParametersPanel.additionalMapSheetWide.tooltip", spnAdditionalMapSheetWide);
        addLabeledSpinner(constraints, "MapParametersPanel.additionalMapSheetTall.label",
              "MapParametersPanel.additionalMapSheetTall.tooltip", spnAdditionalMapSheetTall);
        addLabeledField(constraints, "MapParametersPanel.allowRotation.label", chkAllowRotation);

        constraints.gridx = 0;
        constraints.gridy++;
        JLabel lblUseAtBSizing = new JLabel(getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.useAtBSizing.label"));
        lblUseAtBSizing.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.useAtBSizing.tooltip")));
        add(lblUseAtBSizing, constraints);
        constraints.gridx = 1;
        add(chkUseAtBSizing, constraints);

        // map type / terrain columns
        constraints.gridx = 2;
        constraints.gridy = 1;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "MapParametersPanel.allowedMapTypes.label")), constraints);
        constraints.gridy++;
        add(btnAllowAllMapTypes, constraints);
        constraints.gridy++;
        add(btnUseSpaceMap, constraints);
        constraints.gridy++;
        add(btnUseLowAtmosphereMap, constraints);
        constraints.gridy++;
        add(btnUseSpecificMapTypes, constraints);

        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridheight = GridBagConstraints.RELATIVE;
        add(new FastJScrollPane(lstAllowedTerrainTypes), constraints);

        applyTooltips();
    }

    /** Applies word-wrapped, behavior-accurate tooltips to the controls. */
    private void applyTooltips() {
        setTip(txtBaseWidth, "MapParametersPanel.baseWidth.tooltip");
        setTip(txtBaseHeight, "MapParametersPanel.baseHeight.tooltip");
        setTip(txtWidthIncrement, "MapParametersPanel.widthIncrement.tooltip");
        setTip(txtHeightIncrement, "MapParametersPanel.heightIncrement.tooltip");
        setTip(spnAdditionalMapSheetWide, "MapParametersPanel.additionalMapSheetWide.tooltip");
        setTip(spnAdditionalMapSheetTall, "MapParametersPanel.additionalMapSheetTall.tooltip");
        setTip(chkAllowRotation, "MapParametersPanel.allowRotation.tooltip");
        setTip(chkUseAtBSizing, "MapParametersPanel.useAtBSizing.tooltip");
        setTip(btnAllowAllMapTypes, "MapParametersPanel.anyGroundMap.tooltip");
        setTip(btnUseSpaceMap, "MapParametersPanel.useSpaceMap.tooltip");
        setTip(btnUseLowAtmosphereMap, "MapParametersPanel.useLowAtmoMap.tooltip");
        setTip(btnUseSpecificMapTypes, "MapParametersPanel.specificMapTypes.tooltip");
        setTip(lstAllowedTerrainTypes, "MapParametersPanel.allowedTerrainTypes.tooltip");
    }

    /** Sets a word-wrapped tooltip (from the resource bundle) on a control. */
    private static void setTip(JComponent component, String tooltipKey) {
        component.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
    }

    private void addLabeledField(GridBagConstraints gbc, String labelKey, java.awt.Component field) {
        gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey)), gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }

    private void addLabeledSpinner(GridBagConstraints gbc, String labelKey, String tooltipKey, JSpinner spinner) {
        gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        label.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
        add(label, gbc);
        gbc.gridx = 1;
        add(spinner, gbc);
    }

    /**
     * When AtB sizing is on, the base dimensions are ignored, so they are disabled.
     */
    private void atbSizingChanged() {
        txtBaseWidth.setEnabled(!chkUseAtBSizing.isSelected());
        txtBaseHeight.setEnabled(!chkUseAtBSizing.isSelected());
    }

    /**
     * The terrain list only applies when specific map types are selected.
     */
    private void mapTypeChanged() {
        lstAllowedTerrainTypes.setEnabled(btnUseSpecificMapTypes.isSelected());
    }

    /**
     * Populates the controls from the given map parameters.
     */
    public void load(ScenarioMapParameters mapParameters) {
        txtBaseWidth.setText(String.valueOf(mapParameters.getBaseWidth()));
        txtBaseHeight.setText(String.valueOf(mapParameters.getBaseHeight()));
        txtWidthIncrement.setText(String.valueOf(mapParameters.getWidthScalingIncrement()));
        txtHeightIncrement.setText(String.valueOf(mapParameters.getHeightScalingIncrement()));
        spnAdditionalMapSheetWide.setValue(mapParameters.getAdditionalMapSheetWide());
        spnAdditionalMapSheetTall.setValue(mapParameters.getAdditionalMapSheetTall());
        chkAllowRotation.setSelected(mapParameters.isAllowRotation());
        chkUseAtBSizing.setSelected(mapParameters.isUseStandardAtBSizing());

        if (mapParameters.getMapLocation() != null) {
            switch (mapParameters.getMapLocation()) {
                case AllGroundTerrain -> btnAllowAllMapTypes.setSelected(true);
                case Space -> btnUseSpaceMap.setSelected(true);
                case LowAtmosphere -> btnUseLowAtmosphereMap.setSelected(true);
                case SpecificGroundTerrain -> btnUseSpecificMapTypes.setSelected(true);
            }
        }

        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < lstAllowedTerrainTypes.getModel().getSize(); i++) {
            if (mapParameters.getAllowedTerrainType().contains(lstAllowedTerrainTypes.getModel().getElementAt(i))) {
                selectedIndices.add(i);
            }
        }
        lstAllowedTerrainTypes.setSelectedIndices(selectedIndices.stream().mapToInt(Integer::intValue).toArray());

        atbSizingChanged();
        mapTypeChanged();
    }

    /**
     * Validates the free-text dimension fields.
     *
     * @return an empty string if valid, otherwise a newline-separated list of problems to show the user
     */
    public String validateInput() {
        return MapDimensionInput.validate(txtBaseWidth.getText(),
              txtBaseHeight.getText(),
              txtWidthIncrement.getText(),
              txtHeightIncrement.getText());
    }

    /**
     * Writes the control values into the given map parameters. Call {@link #validateInput()} first.
     */
    public void writeInto(ScenarioMapParameters mapParameters) {
        mapParameters.setBaseWidth(MathUtility.parseInt(txtBaseWidth.getText().trim()));
        mapParameters.setBaseHeight(MathUtility.parseInt(txtBaseHeight.getText().trim()));
        mapParameters.setWidthScalingIncrement(MathUtility.parseInt(txtWidthIncrement.getText().trim()));
        mapParameters.setHeightScalingIncrement(MathUtility.parseInt(txtHeightIncrement.getText().trim()));
        mapParameters.setAdditionalMapSheetWide((int) spnAdditionalMapSheetWide.getValue());
        mapParameters.setAdditionalMapSheetTall((int) spnAdditionalMapSheetTall.getValue());
        mapParameters.setAllowRotation(chkAllowRotation.isSelected());
        mapParameters.setUseStandardAtBSizing(chkUseAtBSizing.isSelected());

        if (btnAllowAllMapTypes.isSelected()) {
            mapParameters.setMapLocation(MapLocation.AllGroundTerrain);
        } else if (btnUseSpaceMap.isSelected()) {
            mapParameters.setMapLocation(MapLocation.Space);
        } else if (btnUseLowAtmosphereMap.isSelected()) {
            mapParameters.setMapLocation(MapLocation.LowAtmosphere);
        } else if (btnUseSpecificMapTypes.isSelected()) {
            mapParameters.setMapLocation(MapLocation.SpecificGroundTerrain);
        }

        mapParameters.allowedTerrainTypes.clear();
        mapParameters.allowedTerrainTypes.addAll(lstAllowedTerrainTypes.getSelectedValuesList());
    }
}
