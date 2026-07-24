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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.ScenarioTemplate.BattlefieldControlType;
import mekhq.campaign.mission.enums.ScenarioType;

/**
 * Editor panel for the top-level properties of a {@link ScenarioTemplate}: name, briefings, scenario type, battlefield
 * control, and the facility and ambush/patrol suitability flags.
 *
 * <p>This is the first section extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). It is a {@link JPanel} -
 * not a window - so it can be constructed and exercised in a headless test, unlike the dialog. The {@link #load} /
 * {@link #writeInto} pair is the testable model-to-UI contract.
 */
public class TemplatePropertiesPanel extends JPanel {

    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(
          "mekhq.resources.ScenarioTemplateEditorDialog");

    private final JTextField txtName = new JTextField(80);
    private final JTextArea txtShortBriefing = new JTextArea(3, 80);
    private final JTextArea txtLongBriefing = new JTextArea(5, 80);
    private final JComboBox<ScenarioType> cboScenarioType = new JComboBox<>(ScenarioType.values());
    private final JComboBox<BattlefieldControlType> cboBattlefieldControl =
          new JComboBox<>(BattlefieldControlType.values());
    private final JCheckBox chkHostileFacility = new JCheckBox(
          RESOURCES.getString("TemplatePropertiesPanel.hostileFacility.label"));
    private final JCheckBox chkAlliedFacility = new JCheckBox(
          RESOURCES.getString("TemplatePropertiesPanel.alliedFacility.label"));
    private final JCheckBox chkSuitedForAmbushes = new JCheckBox(
          RESOURCES.getString("TemplatePropertiesPanel.suitedForAmbushes.label"));
    private final JCheckBox chkSuitedForBungledPatrols = new JCheckBox(
          RESOURCES.getString("TemplatePropertiesPanel.suitedForBungledPatrols.label"));

    public TemplatePropertiesPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        txtShortBriefing.setLineWrap(true);
        txtLongBriefing.setLineWrap(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);

        add(new JLabel(RESOURCES.getString("TemplatePropertiesPanel.scenarioName.label")), gbc);
        gbc.gridy++;
        add(txtName, gbc);

        gbc.gridy++;
        add(new JLabel(RESOURCES.getString("TemplatePropertiesPanel.shortBriefing.label")), gbc);
        gbc.gridy++;
        add(new FastJScrollPane(txtShortBriefing), gbc);

        gbc.gridy++;
        add(new JLabel(RESOURCES.getString("TemplatePropertiesPanel.detailedBriefing.label")), gbc);
        gbc.gridy++;
        add(new FastJScrollPane(txtLongBriefing), gbc);

        JPanel typeRow = new JPanel();
        typeRow.add(new JLabel(RESOURCES.getString("TemplatePropertiesPanel.scenarioType.label")));
        typeRow.add(cboScenarioType);
        typeRow.add(new JLabel(RESOURCES.getString("TemplatePropertiesPanel.battlefieldControl.label")));
        typeRow.add(cboBattlefieldControl);
        gbc.gridy++;
        add(typeRow, gbc);

        JPanel flagRow = new JPanel();
        flagRow.add(chkHostileFacility);
        flagRow.add(chkAlliedFacility);
        flagRow.add(chkSuitedForAmbushes);
        flagRow.add(chkSuitedForBungledPatrols);
        gbc.gridy++;
        add(flagRow, gbc);
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
        template.setStratConScenarioType(((ScenarioType) cboScenarioType.getSelectedItem()).name());
        template.battlefieldControl = (BattlefieldControlType) cboBattlefieldControl.getSelectedItem();
        template.isHostileFacility = chkHostileFacility.isSelected();
        template.isAlliedFacility = chkAlliedFacility.isSelected();
        template.isSuitedForAmbushes = chkSuitedForAmbushes.isSelected();
        template.isSuitedForBungledPatrols = chkSuitedForBungledPatrols.isSelected();
    }
}
