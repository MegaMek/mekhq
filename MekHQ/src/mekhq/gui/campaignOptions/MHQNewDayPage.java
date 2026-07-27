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
package mekhq.gui.campaignOptions;

import static megamek.client.ui.util.FontHandler.symbolIcon;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.util.UIUtil;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.enums.FormationIconOperationalStatusStyle;

/**
 * The New Day page of the MekHQ Client Options dialog: the personnel-pool fill/no-release groups, the automated new-day
 * tasks, the monthly quick-train settings, and the formation-icon operational-status toggle. Owns its controls and
 * copies them back into the shared {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQNewDayPage extends MHQOptionsPage {
    // New Day - Personnel Pools (keyed by resource name, matching MHQOptionsModel.newDayPools)
    private final Map<String, CampaignOptionsCheckBox> poolCheckBoxes = new HashMap<>();

    // New Day - Automation
    private CampaignOptionsCheckBox chkNewDayAutoLogistics;
    private CampaignOptionsCheckBox chkNewDayMRMS;
    private CampaignOptionsCheckBox chkNewDayOptimizeMedicalAssignments;
    private CampaignOptionsCheckBox chkNewDayAutomaticallyAssignUnmaintainedUnits;
    private CampaignOptionsCheckBox chkSelfCorrectMaintenance;

    // New Day - Training
    private CampaignOptionsCheckBox chkNewMonthQuickTrain;
    private CampaignOptionsSpinner spinnerQuickTrainTarget;
    private CampaignOptionsCheckBox chkLevelArtillery;
    private CampaignOptionsCheckBox chkLevelScoutingSkills;
    private CampaignOptionsCheckBox chkLevelEscapeSkills;
    private CampaignOptionsCheckBox chkLevelLeadership;
    private CampaignOptionsCheckBox chkLevelTraining;
    private CampaignOptionsCheckBox chkLevelOtherCommandSkills;

    // New Day - Formation Icons
    private CampaignOptionsCheckBox chkNewDayFormationIconOperationalStatus;
    private MMComboBox<FormationIconOperationalStatusStyle> comboNewDayFormationIconOperationalStatusStyle;

    MHQNewDayPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        JComponent poolContent = createNewDayPoolSection();
        JComponent tasksContent = createNewDayTasksSection();
        JComponent trainingContent = createNewDayTrainingSection();
        JComponent formationContent = createNewDayFormationSection();
        // Register each section body's tips before the page is built; |= always evaluates its right side, so no
        // section's registration is skipped.
        boolean hasTooltips = registerDetailsTips(poolContent);
        hasTooltips |= registerDetailsTips(tasksContent);
        hasTooltips |= registerDetailsTips(trainingContent);
        hasTooltips |= registerDetailsTips(formationContent);
        Component page = pageBuilder("MHQNewDayPage", hasTooltips)
                     .section("lblMHQNewDayPoolSection.text", "lblMHQNewDayPoolSection.summary", poolContent)
                     .section("lblMHQNewDayTasksSection.text", "lblMHQNewDayTasksSection.summary", tasksContent)
                     .section("lblMHQNewDayTrainingSection.text", "lblMHQNewDayTrainingSection.summary",
                           trainingContent)
                     .section("lblMHQNewDayFormationSection.text", "lblMHQNewDayFormationSection.summary",
                           formationContent)
                     .build();
        created = true;
        return page;
    }

    private JPanel createNewDayPoolSection() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQNewDayPoolContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        // Two columns of parent/child groups: each "Fill X Pool" parent sits above its indented "Do Not Release
        // Surplus X" child (joined by a subdirectory-arrow connector), and the groups flow two-across so the section
        // stays compact instead of one tall scrolling column. The child is gated on the parent.
        panel.addComponentGrid(2,
              poolPair("chkNewDayAstechPoolFill", "chkNewDayAstechPoolNoRelease"),
              poolPair("chkNewDayMedicPoolFill", "chkNewDayMedicPoolNoRelease"),
              poolPair("chkNewDaySoldierPoolFill", "chkNewDaySoldierPoolNoRelease"),
              poolPair("chkNewDayBattleArmorPoolFill", "chkNewDayBattleArmorPoolNoRelease"),
              poolPair("chkNewDayVehicleCrewGroundPoolFill", "chkNewDayVehicleCrewGroundPoolNoRelease"),
              poolPair("chkNewDayVehicleCrewVTOLPoolFill", "chkNewDayVehicleCrewVTOLPoolNoRelease"),
              poolPair("chkNewDayVehicleCrewNavalPoolFill", "chkNewDayVehicleCrewNavalPoolNoRelease"),
              poolPair("chkNewDayVesselPilotPoolFill", "chkNewDayVesselPilotPoolNoRelease"),
              poolPair("chkNewDayVesselGunnerPoolFill", "chkNewDayVesselGunnerPoolNoRelease"),
              poolPair("chkNewDayVesselCrewPoolFill", "chkNewDayVesselCrewPoolNoRelease"));
        return panel;
    }

    private JPanel createNewDayTasksSection() {
        chkNewDayAutoLogistics = checkBox("chkNewDayAutoLogistics", model.newDayAutoLogistics);
        chkNewDayMRMS = checkBox("chkNewDayMRMS", model.newDayMRMS);
        chkNewDayOptimizeMedicalAssignments =
              checkBox("chkNewDayOptimizeMedicalAssignments", model.newDayOptimizeMedicalAssignments);
        chkNewDayAutomaticallyAssignUnmaintainedUnits =
              checkBox("chkNewDayAutomaticallyAssignUnmaintainedUnits",
                    model.newDayAutomaticallyAssignUnmaintainedUnits);
        chkSelfCorrectMaintenance = checkBox("chkSelfCorrectMaintenance", model.selfCorrectMaintenance);

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQNewDayTasksContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addCheckBoxGrid(2, chkNewDayAutoLogistics, chkNewDayMRMS, chkNewDayOptimizeMedicalAssignments,
              chkNewDayAutomaticallyAssignUnmaintainedUnits, chkSelfCorrectMaintenance);
        return panel;
    }

    private JPanel createNewDayTrainingSection() {
        chkNewMonthQuickTrain = checkBox("chkNewMonthQuickTrain", model.newMonthQuickTrain);

        CampaignOptionsLabel labelQuickTrainTarget = new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblQuickTrainTarget");
        spinnerQuickTrainTarget =
              new CampaignOptionsSpinner(RESOURCE_BUNDLE, "lblQuickTrainTarget", 5, 1, 10, 1);
        spinnerQuickTrainTarget.setValue(model.quickTrainTarget);

        chkLevelArtillery = checkBox("chkLevelArtillery", model.levelArtillery);
        chkLevelScoutingSkills = checkBox("chkLevelScoutingSkills", model.levelScouting);
        chkLevelEscapeSkills = checkBox("chkLevelEscapeSkills", model.levelEscape);
        chkLevelLeadership = checkBox("chkLevelLeadership", model.levelLeadership);
        chkLevelTraining = checkBox("chkLevelTraining", model.levelTraining);
        chkLevelOtherCommandSkills = checkBox("chkLevelOtherCommandSkills", model.levelOtherCommand);

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQNewDayTrainingContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addCheckBox(chkNewMonthQuickTrain);
        panel.addRow(labelQuickTrainTarget, spinnerQuickTrainTarget);
        panel.addCheckBoxGrid(2, chkLevelArtillery, chkLevelScoutingSkills, chkLevelEscapeSkills, chkLevelLeadership,
              chkLevelTraining, chkLevelOtherCommandSkills);
        return panel;
    }

    private JPanel createNewDayFormationSection() {
        chkNewDayFormationIconOperationalStatus =
              checkBox("chkNewDayFormationIconOperationalStatus", model.newDayFormationIconOperationalStatus);

        CampaignOptionsLabel labelStyle =
              new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblNewDayFormationIconOperationalStatusStyle");
        comboNewDayFormationIconOperationalStatusStyle = new MMComboBox<>(
              "comboNewDayFormationIconOperationalStatusStyle", FormationIconOperationalStatusStyle.values());
        comboNewDayFormationIconOperationalStatusStyle.setSelectedItem(
              model.newDayFormationIconOperationalStatusStyle);
        comboNewDayFormationIconOperationalStatusStyle.setToolTipText(getTextAt(RESOURCE_BUNDLE,
              "lblNewDayFormationIconOperationalStatusStyle.toolTipText"));
        comboNewDayFormationIconOperationalStatusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FormationIconOperationalStatusStyle style) {
                    list.setToolTipText(style.getToolTipText());
                }
                return this;
            }
        });

        // The style picker only matters when the status feature is on, so it tracks the check box's selected state.
        Runnable syncStyleEnabled = () -> {
            boolean enabled = chkNewDayFormationIconOperationalStatus.isSelected();
            labelStyle.setEnabled(enabled);
            comboNewDayFormationIconOperationalStatusStyle.setEnabled(enabled);
        };
        chkNewDayFormationIconOperationalStatus.addActionListener(evt -> syncStyleEnabled.run());
        syncStyleEnabled.run();

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQNewDayFormationContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addCheckBox(chkNewDayFormationIconOperationalStatus);
        panel.addRow(labelStyle, comboNewDayFormationIconOperationalStatusStyle);
        return panel;
    }

    /**
     * Builds a "Fill Pool" / "Do Not Release Surplus" parent/child group as a small vertical panel: the parent check
     * box above its indented child, joined by a Material "subdirectory_arrow_right" connector. Both boxes are
     * registered in {@link #poolCheckBoxes} under their resource names (matching {@link MHQOptionsModel#newDayPools}),
     * and the child is enabled only while the parent is selected. The caller lays the groups out two-across.
     */
    private JComponent poolPair(String fillKey, String noReleaseKey) {
        CampaignOptionsCheckBox fill = checkBox(fillKey, Boolean.TRUE.equals(model.newDayPools.get(fillKey)));
        CampaignOptionsCheckBox noRelease =
              checkBox(noReleaseKey, Boolean.TRUE.equals(model.newDayPools.get(noReleaseKey)));
        poolCheckBoxes.put(fillKey, fill);
        poolCheckBoxes.put(noReleaseKey, noRelease);

        JLabel connector = new JLabel();
        fill.addItemListener(evt -> syncPoolChild(fill, noRelease, connector));
        syncPoolChild(fill, noRelease, connector);

        // Indent the child under its parent, prefixed by the subdirectory-arrow connector, so the pair reads as one
        // parent-with-sub-option group.
        JPanel childRow = new JPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(4), 0));
        childRow.setOpaque(false);
        childRow.setBorder(BorderFactory.createEmptyBorder(0, UIUtil.scaleForGUI(12), 0, 0));
        childRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        childRow.add(connector);
        childRow.add(noRelease);

        JPanel pairPanel = new JPanel();
        pairPanel.setOpaque(false);
        pairPanel.setLayout(new BoxLayout(pairPanel, BoxLayout.PAGE_AXIS));
        fill.setAlignmentX(Component.LEFT_ALIGNMENT);
        pairPanel.add(fill);
        pairPanel.add(childRow);
        return pairPanel;
    }

    /**
     * Keeps a pool group's child in step with its parent: the "Do Not Release Surplus" child is enabled only while the
     * "Fill Pool" parent is selected, and the connector icon is tinted to match (muted while the child is disabled).
     */
    private void syncPoolChild(JCheckBox parent, JCheckBox child, JLabel connector) {
        boolean enabled = parent.isSelected();
        child.setEnabled(enabled);
        Color color = enabled
              ? child.getForeground()
              : Objects.requireNonNullElse(UIManager.getColor("Label.disabledForeground"), child.getForeground());
        connector.setIcon(symbolIcon(0xE5DA, child.getFont().getSize(), color));
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        poolCheckBoxes.forEach((key, checkBox) -> model.newDayPools.put(key, checkBox.isSelected()));

        model.newDayAutoLogistics = chkNewDayAutoLogistics.isSelected();
        model.newDayMRMS = chkNewDayMRMS.isSelected();
        model.newDayOptimizeMedicalAssignments = chkNewDayOptimizeMedicalAssignments.isSelected();
        model.newDayAutomaticallyAssignUnmaintainedUnits = chkNewDayAutomaticallyAssignUnmaintainedUnits.isSelected();
        model.selfCorrectMaintenance = chkSelfCorrectMaintenance.isSelected();

        model.newMonthQuickTrain = chkNewMonthQuickTrain.isSelected();
        model.quickTrainTarget = (int) spinnerQuickTrainTarget.getValue();
        model.levelArtillery = chkLevelArtillery.isSelected();
        model.levelScouting = chkLevelScoutingSkills.isSelected();
        model.levelEscape = chkLevelEscapeSkills.isSelected();
        model.levelLeadership = chkLevelLeadership.isSelected();
        model.levelTraining = chkLevelTraining.isSelected();
        model.levelOtherCommand = chkLevelOtherCommandSkills.isSelected();

        model.newDayFormationIconOperationalStatus = chkNewDayFormationIconOperationalStatus.isSelected();
        model.newDayFormationIconOperationalStatusStyle =
              Objects.requireNonNull(comboNewDayFormationIconOperationalStatusStyle.getSelectedItem());
    }
}
