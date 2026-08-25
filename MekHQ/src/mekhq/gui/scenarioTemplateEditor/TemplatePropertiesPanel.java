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
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.ScenarioTemplate.BattlefieldControlType;
import mekhq.campaign.mission.scenarios.ScenarioType;

/**
 * Editor panel for the top-level properties of a {@link ScenarioTemplate}: name, briefings, scenario type, battlefield
 * control, and the facility and ambush/patrol suitability flags.
 *
 * <p>This is the first section extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). It is a {@link JPanel} -
 * not a window - so it can be constructed and exercised in a headless test, unlike the dialog. The {@link #load} /
 * {@link #writeInto} pair is the testable model-to-UI contract.
 */
public class TemplatePropertiesPanel extends JPanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private final JTextField txtName = new JTextField(80);
    private final JTextArea txtShortBriefing = new JTextArea(3, 80);
    private final JTextArea txtLongBriefing = new JTextArea(5, 80);
    private final JComboBox<ScenarioType> cboScenarioType = new JComboBox<>(ScenarioType.values());
    private final JComboBox<BattlefieldControlType> cboBattlefieldControl =
          new JComboBox<>(BattlefieldControlType.values());
    private final JCheckBox chkHostileFacility = new JCheckBox(
          getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.hostileFacility.label"));
    private final JCheckBox chkAlliedFacility = new JCheckBox(
          getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.alliedFacility.label"));
    private final JCheckBox chkSuitedForAmbushes = new JCheckBox(
          getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.suitedForAmbushes.label"));
    private final JCheckBox chkSuitedForBungledPatrols = new JCheckBox(
          getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.suitedForBungledPatrols.label"));

    public TemplatePropertiesPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        txtShortBriefing.setLineWrap(true);
        txtLongBriefing.setLineWrap(true);

        applyTooltips();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;

        int padding = scaleForGUI(5);
        constraints.insets = new Insets(padding, padding, padding, padding);

        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.scenarioName.label")), constraints);
        constraints.gridy++;
        add(txtName, constraints);

        constraints.gridy++;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.shortBriefing.label")), constraints);
        constraints.gridy++;
        add(new FastJScrollPane(txtShortBriefing), constraints);

        constraints.gridy++;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.detailedBriefing.label")), constraints);
        constraints.gridy++;
        add(new FastJScrollPane(txtLongBriefing), constraints);

        JPanel typeRow = new JPanel();
        typeRow.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.scenarioType.label")));
        typeRow.add(cboScenarioType);
        typeRow.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "TemplatePropertiesPanel.battlefieldControl.label")));
        typeRow.add(cboBattlefieldControl);
        constraints.gridy++;
        add(typeRow, constraints);

        JPanel flagRow = new JPanel();
        flagRow.add(chkHostileFacility);
        flagRow.add(chkAlliedFacility);
        flagRow.add(chkSuitedForAmbushes);
        flagRow.add(chkSuitedForBungledPatrols);
        constraints.gridy++;
        add(flagRow, constraints);
    }

    /** Applies word-wrapped, behavior-accurate tooltips to the controls. */
    private void applyTooltips() {
        setTip(txtName, "TemplatePropertiesPanel.scenarioName.tooltip");
        setTip(txtShortBriefing, "TemplatePropertiesPanel.shortBriefing.tooltip");
        setTip(txtLongBriefing, "TemplatePropertiesPanel.detailedBriefing.tooltip");
        setTip(cboScenarioType, "TemplatePropertiesPanel.scenarioType.tooltip");
        setTip(cboBattlefieldControl, "TemplatePropertiesPanel.battlefieldControl.tooltip");
        setTip(chkHostileFacility, "TemplatePropertiesPanel.hostileFacility.tooltip");
        setTip(chkAlliedFacility, "TemplatePropertiesPanel.alliedFacility.tooltip");
        setTip(chkSuitedForAmbushes, "TemplatePropertiesPanel.suitedForAmbushes.tooltip");
        setTip(chkSuitedForBungledPatrols, "TemplatePropertiesPanel.suitedForBungledPatrols.tooltip");
    }

    /** Sets a word-wrapped tooltip (from the resource bundle) on a control. */
    private static void setTip(JComponent component, String tooltipKey) {
        component.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, tooltipKey)));
    }

    /**
     * Populates the controls from the given template.
     */
    public void load(ScenarioTemplate template) {
        txtName.setText(template.name);
        txtShortBriefing.setText(template.shortBriefing);
        txtLongBriefing.setText(template.detailedBriefing);
        cboScenarioType.setSelectedItem(template.getStratConScenarioType());
        cboBattlefieldControl.setSelectedItem(template.getBattlefieldControl());
        chkHostileFacility.setSelected(template.isHostileFacility);
        chkAlliedFacility.setSelected(template.isAlliedFacility);
        chkSuitedForAmbushes.setSelected(template.isSuitedForAmbushes());
        chkSuitedForBungledPatrols.setSelected(template.isSuitedForBungledPatrols());
    }

    /**
     * Writes the control values back into the given template.
     */
    public void writeInto(ScenarioTemplate template) {
        template.name = txtName.getText();
        template.shortBriefing = txtShortBriefing.getText();
        template.detailedBriefing = txtLongBriefing.getText();

        ScenarioType selectedScenarioType = (ScenarioType) cboScenarioType.getSelectedItem();
        if (selectedScenarioType != null) {
            template.setStratConScenarioType(selectedScenarioType.name());
        }

        BattlefieldControlType selectedBattlefieldControl = (BattlefieldControlType) cboBattlefieldControl.getSelectedItem();
        if (selectedBattlefieldControl != null) {
            template.battlefieldControl = selectedBattlefieldControl;
        }

        template.isHostileFacility = chkHostileFacility.isSelected();
        template.isAlliedFacility = chkAlliedFacility.isSelected();
        template.isSuitedForAmbushes = chkSuitedForAmbushes.isSelected();
        template.isSuitedForBungledPatrols = chkSuitedForBungledPatrols.isSelected();
    }
}
