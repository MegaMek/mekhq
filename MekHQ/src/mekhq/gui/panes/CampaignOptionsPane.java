/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.panes;

import megamek.client.ui.baseComponents.MMButton;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.baseComponents.JDisableablePanel;

import javax.swing.*;
import java.awt.*;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {

    //region Initialization
    //region Modern Initialization
    //region Personnel Tab
    private JScrollPane createPersonnelTab() {
        final JPanel personnelPanel = new JPanel(new GridBagLayout());
        personnelPanel.setName("personnelPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        personnelPanel.add(createGeneralPersonnelPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createExpandedPersonnelInformationPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createMedicalPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createPrisonerPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createPersonnelRandomizationPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createFamilyPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        personnelPanel.add(createSalaryPanel(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        personnelPanel.add(createMarriagePanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createDivorcePanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        personnelPanel.add(createProcreationPanel(), gbc);

        gbc.gridx++;
        personnelPanel.add(createDeathPanel(), gbc);

        final JScrollPane scrollPersonnel = new JScrollPane(personnelPanel);
        scrollPersonnel.setPreferredSize(new Dimension(500, 400));

        return scrollPersonnel;
    }

    private JPanel createGeneralPersonnelPanel() {
        // Create Panel Components
        chkUseTactics = new JCheckBox(resources.getString("chkUseTactics.text"));
        chkUseTactics.setToolTipText(resources.getString("chkUseTactics.toolTipText"));
        chkUseTactics.setName("chkUseTactics");

        chkUseInitiativeBonus = new JCheckBox(resources.getString("chkUseInitiativeBonus.text"));
        chkUseInitiativeBonus.setToolTipText(resources.getString("chkUseInitiativeBonus.toolTipText"));
        chkUseInitiativeBonus.setName("chkUseInitiativeBonus");

        chkUseToughness = new JCheckBox(resources.getString("chkUseToughness.text"));
        chkUseToughness.setToolTipText(resources.getString("chkUseToughness.toolTipText"));
        chkUseToughness.setName("chkUseToughness");

        chkUseArtillery = new JCheckBox(resources.getString("chkUseArtillery.text"));
        chkUseArtillery.setToolTipText(resources.getString("chkUseArtillery.toolTipText"));
        chkUseArtillery.setName("chkUseArtillery");

        chkUseAbilities = new JCheckBox(resources.getString("chkUseAbilities.text"));
        chkUseAbilities.setToolTipText(resources.getString("chkUseAbilities.toolTipText"));
        chkUseAbilities.setName("chkUseAbilities");

        chkUseEdge = new JCheckBox(resources.getString("chkUseEdge.text"));
        chkUseEdge.setToolTipText(resources.getString("chkUseEdge.toolTipText"));
        chkUseEdge.setName("chkUseEdge");
        chkUseEdge.addActionListener(evt -> chkUseSupportEdge.setEnabled(chkUseEdge.isSelected()));

        chkUseSupportEdge = new JCheckBox(resources.getString("chkUseSupportEdge.text"));
        chkUseSupportEdge.setToolTipText(resources.getString("chkUseSupportEdge.toolTipText"));
        chkUseSupportEdge.setName("chkUseSupportEdge");

        chkUseImplants = new JCheckBox(resources.getString("chkUseImplants.text"));
        chkUseImplants.setToolTipText(resources.getString("chkUseImplants.toolTipText"));
        chkUseImplants.setName("chkUseImplants");

        chkUseAlternativeQualityAveraging = new JCheckBox(resources.getString("chkUseAlternativeQualityAveraging.text"));
        chkUseAlternativeQualityAveraging.setToolTipText(resources.getString("chkUseAlternativeQualityAveraging.toolTipText"));
        chkUseAlternativeQualityAveraging.setName("chkUseAlternativeQualityAveraging");

        chkUseTransfers = new JCheckBox(resources.getString("chkUseTransfers.text"));
        chkUseTransfers.setToolTipText(resources.getString("chkUseTransfers.toolTipText"));
        chkUseTransfers.setName("chkUseTransfers");

        chkUseExtendedTOEForceName = new JCheckBox(resources.getString("chkUseExtendedTOEForceName.text"));
        chkUseExtendedTOEForceName.setToolTipText(resources.getString("chkUseExtendedTOEForceName.toolTipText"));
        chkUseExtendedTOEForceName.setName("chkUseExtendedTOEForceName ");

        chkPersonnelLogSkillGain = new JCheckBox(resources.getString("chkPersonnelLogSkillGain.text"));
        chkPersonnelLogSkillGain.setToolTipText(resources.getString("chkPersonnelLogSkillGain.toolTipText"));
        chkPersonnelLogSkillGain.setName("chkPersonnelLogSkillGain");

        chkPersonnelLogAbilityGain = new JCheckBox(resources.getString("chkPersonnelLogAbilityGain.text"));
        chkPersonnelLogAbilityGain.setToolTipText(resources.getString("chkPersonnelLogAbilityGain.toolTipText"));
        chkPersonnelLogAbilityGain.setName("chkPersonnelLogAbilityGain");

        chkPersonnelLogEdgeGain = new JCheckBox(resources.getString("chkPersonnelLogEdgeGain.text"));
        chkPersonnelLogEdgeGain.setToolTipText(resources.getString("chkPersonnelLogEdgeGain.toolTipText"));
        chkPersonnelLogEdgeGain.setName("chkPersonnelLogEdgeGain");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setName("generalPersonnelPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
                        .addComponent(chkUseExtendedTOEForceName)
                        .addComponent(chkPersonnelLogSkillGain)
                        .addComponent(chkPersonnelLogAbilityGain)
                        .addComponent(chkPersonnelLogEdgeGain)
        );

        return panel;
    }

    private JPanel createExpandedPersonnelInformationPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblTimeInServiceDisplayFormat = new JLabel();
        final JLabel lblTimeInRankDisplayFormat = new JLabel();

        // Create Panel Components
        chkUseTimeInService = new JCheckBox(resources.getString("chkUseTimeInService.text"));
        chkUseTimeInService.setToolTipText(resources.getString("chkUseTimeInService.toolTipText"));
        chkUseTimeInService.setName("chkUseTimeInService");
        chkUseTimeInService.addActionListener(evt -> {
            lblTimeInServiceDisplayFormat.setEnabled(chkUseTimeInService.isSelected());
            comboTimeInServiceDisplayFormat.setEnabled(chkUseTimeInService.isSelected());
        });

        lblTimeInServiceDisplayFormat.setText(resources.getString("lblTimeInServiceDisplayFormat.text"));
        lblTimeInServiceDisplayFormat.setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));
        lblTimeInServiceDisplayFormat.setName("lblTimeInServiceDisplayFormat");

        comboTimeInServiceDisplayFormat = new JComboBox<>(TimeInDisplayFormat.values());
        comboTimeInServiceDisplayFormat.setToolTipText(resources.getString("lblTimeInServiceDisplayFormat.toolTipText"));
        comboTimeInServiceDisplayFormat.setName("comboTimeInServiceDisplayFormat");

        chkUseTimeInRank = new JCheckBox(resources.getString("chkUseTimeInRank.text"));
        chkUseTimeInRank.setToolTipText(resources.getString("chkUseTimeInRank.toolTipText"));
        chkUseTimeInRank.setName("chkUseTimeInRank");
        chkUseTimeInRank.addActionListener(evt -> {
            lblTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
            comboTimeInRankDisplayFormat.setEnabled(chkUseTimeInRank.isSelected());
        });

        lblTimeInRankDisplayFormat.setText(resources.getString("lblTimeInRankDisplayFormat.text"));
        lblTimeInRankDisplayFormat.setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));
        lblTimeInRankDisplayFormat.setName("lblTimeInRankDisplayFormat");

        comboTimeInRankDisplayFormat = new JComboBox<>(TimeInDisplayFormat.values());
        comboTimeInRankDisplayFormat.setToolTipText(resources.getString("lblTimeInRankDisplayFormat.toolTipText"));
        comboTimeInRankDisplayFormat.setName("comboTimeInRankDisplayFormat");

        chkUseRetirementDateTracking = new JCheckBox(resources.getString("chkUseRetirementDateTracking.text"));
        chkUseRetirementDateTracking.setToolTipText(resources.getString("chkUseRetirementDateTracking.toolTipText"));
        chkUseRetirementDateTracking.setName("chkUseRetirementDateTracking");

        chkTrackTotalEarnings = new JCheckBox(resources.getString("chkTrackTotalEarnings.text"));
        chkTrackTotalEarnings.setToolTipText(resources.getString("chkTrackTotalEarnings.toolTipText"));
        chkTrackTotalEarnings.setName("chkTrackTotalEarnings");

        chkTrackTotalXPEarnings = new JCheckBox(resources.getString("chkTrackTotalXPEarnings.text"));
        chkTrackTotalXPEarnings.setToolTipText(resources.getString("chkTrackTotalXPEarnings.toolTipText"));
        chkTrackTotalXPEarnings.setName("chkTrackTotalXPEarnings");

        chkShowOriginFaction = new JCheckBox(resources.getString("chkShowOriginFaction.text"));
        chkShowOriginFaction.setToolTipText(resources.getString("chkShowOriginFaction.toolTipText"));
        chkShowOriginFaction.setName("chkShowOriginFaction");

        // Programmatically Assign Accessibility Labels
        lblTimeInServiceDisplayFormat.setLabelFor(comboTimeInServiceDisplayFormat);
        lblTimeInRankDisplayFormat.setLabelFor(comboTimeInRankDisplayFormat);

        // Disable Panel Portions by Default
        chkUseTimeInService.setSelected(true);
        chkUseTimeInService.doClick();
        chkUseTimeInRank.setSelected(true);
        chkUseTimeInRank.doClick();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("expandedPersonnelInformationPanel.title")));
        panel.setName("expandedPersonnelInformationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRetirementDateTracking)
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseTimeInService)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInServiceDisplayFormat)
                                .addComponent(comboTimeInServiceDisplayFormat))
                        .addComponent(chkUseTimeInRank)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblTimeInRankDisplayFormat)
                                .addComponent(comboTimeInRankDisplayFormat))
                        .addComponent(chkUseRetirementDateTracking)
                        .addComponent(chkTrackTotalEarnings)
                        .addComponent(chkTrackTotalXPEarnings)
                        .addComponent(chkShowOriginFaction)
        );

        return panel;
    }

    private JPanel createMedicalPanel() {
        // Create Panel Components
        chkUseAdvancedMedical = new JCheckBox(resources.getString("chkUseAdvancedMedical.text"));
        chkUseAdvancedMedical.setToolTipText(resources.getString("chkUseAdvancedMedical.toolTipText"));
        chkUseAdvancedMedical.setName("chkUseAdvancedMedical");

        final JLabel lblHealWaitingPeriod = new JLabel(resources.getString("lblHealWaitingPeriod.text"));
        lblHealWaitingPeriod.setToolTipText(resources.getString("lblHealWaitingPeriod.toolTipText"));
        lblHealWaitingPeriod.setName("lblHealWaitingPeriod");

        spnHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
        spnHealWaitingPeriod.setToolTipText(resources.getString("lblHealWaitingPeriod.toolTipText"));
        spnHealWaitingPeriod.setName("spnHealWaitingPeriod");

        final JLabel lblNaturalHealWaitingPeriod = new JLabel(resources.getString("lblNaturalHealWaitingPeriod.text"));
        lblNaturalHealWaitingPeriod.setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        lblNaturalHealWaitingPeriod.setName("lblNaturalHealWaitingPeriod");

        spnNaturalHealWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        spnNaturalHealWaitingPeriod.setToolTipText(resources.getString("lblNaturalHealWaitingPeriod.toolTipText"));
        spnNaturalHealWaitingPeriod.setName("spnNaturalHealWaitingPeriod");

        final JLabel lblMinimumHitsForVehicles = new JLabel(resources.getString("lblMinimumHitsForVehicles.text"));
        lblMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        lblMinimumHitsForVehicles.setName("lblMinimumHitsForVehicles");

        spnMinimumHitsForVehicles = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        spnMinimumHitsForVehicles.setToolTipText(resources.getString("lblMinimumHitsForVehicles.toolTipText"));
        spnMinimumHitsForVehicles.setName("spnMinimumHitsForVehicles");
        ((JSpinner.DefaultEditor) spnMinimumHitsForVehicles.getEditor()).getTextField().setEditable(false);

        chkUseRandomHitsForVehicles = new JCheckBox(resources.getString("chkUseRandomHitsForVehicles.text"));
        chkUseRandomHitsForVehicles.setToolTipText(resources.getString("chkUseRandomHitsForVehicles.toolTipText"));
        chkUseRandomHitsForVehicles.setName("chkUseRandomHitsForVehicles");

        chkUseTougherHealing = new JCheckBox(resources.getString("chkUseTougherHealing.text"));
        chkUseTougherHealing.setToolTipText(resources.getString("chkUseTougherHealing.toolTipText"));
        chkUseTougherHealing.setName("chkUseTougherHealing");

        // Programmatically Assign Accessibility Labels
        lblHealWaitingPeriod.setLabelFor(spnHealWaitingPeriod);
        lblNaturalHealWaitingPeriod.setLabelFor(spnNaturalHealWaitingPeriod);
        lblMinimumHitsForVehicles.setLabelFor(spnMinimumHitsForVehicles);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("medicalPanel.title")));
        panel.setName("medicalPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseAdvancedMedical)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblHealWaitingPeriod)
                                .addComponent(spnHealWaitingPeriod, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblNaturalHealWaitingPeriod)
                                .addComponent(spnNaturalHealWaitingPeriod, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMinimumHitsForVehicles)
                                .addComponent(spnMinimumHitsForVehicles, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomHitsForVehicles)
                        .addComponent(chkUseTougherHealing)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseAdvancedMedical)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblHealWaitingPeriod)
                                .addComponent(spnHealWaitingPeriod))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNaturalHealWaitingPeriod)
                                .addComponent(spnNaturalHealWaitingPeriod))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumHitsForVehicles)
                                .addComponent(spnMinimumHitsForVehicles))
                        .addComponent(chkUseRandomHitsForVehicles)
                        .addComponent(chkUseTougherHealing)
        );

        return panel;
    }

    private JPanel createPrisonerPanel() {
        // Create Panel Components
        final JLabel lblPrisonerCaptureStyle = new JLabel(resources.getString("lblPrisonerCaptureStyle.text"));
        lblPrisonerCaptureStyle.setToolTipText(resources.getString("lblPrisonerCaptureStyle.toolTipText"));
        lblPrisonerCaptureStyle.setName("lblPrisonerCaptureStyle");

        comboPrisonerCaptureStyle = new JComboBox<>(PrisonerCaptureStyle.values());
        comboPrisonerCaptureStyle.setName("comboPrisonerCaptureStyle");
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(((PrisonerCaptureStyle) value).getToolTip());
                }
                return this;
            }
        });

        final JLabel lblPrisonerStatus = new JLabel(resources.getString("lblPrisonerStatus.text"));
        lblPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));
        lblPrisonerStatus.setName("lblPrisonerStatus");

        final DefaultComboBoxModel<PrisonerStatus> prisonerStatusModel = new DefaultComboBoxModel<>(PrisonerStatus.values());
        prisonerStatusModel.removeElement(PrisonerStatus.FREE); // we don't want this as a standard use case for prisoners
        comboPrisonerStatus = new JComboBox<>(prisonerStatusModel);
        comboPrisonerStatus.setToolTipText(resources.getString("lblPrisonerStatus.toolTipText"));
        comboPrisonerStatus.setName("comboPrisonerStatus");

        chkPrisonerBabyStatus = new JCheckBox(resources.getString("chkPrisonerBabyStatus.text"));
        chkPrisonerBabyStatus.setToolTipText(resources.getString("chkPrisonerBabyStatus.toolTipText"));
        chkPrisonerBabyStatus.setName("chkPrisonerBabyStatus");

        chkAtBPrisonerDefection = new JCheckBox(resources.getString("chkAtBPrisonerDefection.text"));
        chkAtBPrisonerDefection.setToolTipText(resources.getString("chkAtBPrisonerDefection.toolTipText"));
        chkAtBPrisonerDefection.setName("chkAtBPrisonerDefection");

        chkAtBPrisonerRansom = new JCheckBox(resources.getString("chkAtBPrisonerRansom.text"));
        chkAtBPrisonerRansom.setToolTipText(resources.getString("chkAtBPrisonerRansom.toolTipText"));
        chkAtBPrisonerRansom.setName("chkAtBPrisonerRansom");

        // Programmatically Assign Accessibility Labels
        lblPrisonerCaptureStyle.setLabelFor(comboPrisonerCaptureStyle);
        lblPrisonerStatus.setLabelFor(comboPrisonerStatus);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("prisonerPanel.title")));
        panel.setName("prisonerPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus, GroupLayout.Alignment.LEADING))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerCaptureStyle)
                                .addComponent(comboPrisonerCaptureStyle))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPrisonerStatus)
                                .addComponent(comboPrisonerStatus))
                        .addComponent(chkPrisonerBabyStatus)
                        .addComponent(chkAtBPrisonerDefection)
                        .addComponent(chkAtBPrisonerRansom)
        );

        return panel;
    }

    private JPanel createPersonnelRandomizationPanel() {
        // Create Panel Components
        chkUseDylansRandomXP = new JCheckBox(resources.getString("chkUseDylansRandomXP.text"));
        chkUseDylansRandomXP.setToolTipText(resources.getString("chkUseDylansRandomXP.toolTipText"));
        chkUseDylansRandomXP.setName("chkUseDylansRandomXP");

        final JPanel randomOriginPanel = createRandomOriginPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelRandomizationPanel.title")));
        panel.setName("personnelRandomizationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseDylansRandomXP)
                        .addComponent(randomOriginPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseDylansRandomXP)
                        .addComponent(randomOriginPanel)
        );

        return panel;
    }

    private JPanel createRandomOriginPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblOriginSearchRadius = new JLabel();
        final JLabel lblOriginDistanceScale = new JLabel();

        // Create Panel Components
        chkRandomizeOrigin = new JCheckBox(resources.getString("chkRandomizeOrigin.text"));
        chkRandomizeOrigin.setToolTipText(resources.getString("chkRandomizeOrigin.toolTipText"));
        chkRandomizeOrigin.setName("chkRandomizeOrigin");
        chkRandomizeOrigin.addActionListener(evt -> {
            final boolean selected = chkRandomizeOrigin.isSelected();
            chkRandomizeDependentsOrigin.setEnabled(selected);
            lblOriginSearchRadius.setEnabled(selected);
            spnOriginSearchRadius.setEnabled(selected);
            chkExtraRandomOrigin.setEnabled(selected);
            lblOriginDistanceScale.setEnabled(selected);
            spnOriginDistanceScale.setEnabled(selected);
        });

        chkRandomizeDependentsOrigin = new JCheckBox(resources.getString("chkRandomizeDependentsOrigin.text"));
        chkRandomizeDependentsOrigin.setToolTipText(resources.getString("chkRandomizeDependentsOrigin.toolTipText"));
        chkRandomizeDependentsOrigin.setName("chkRandomizeDependentsOrigin");

        lblOriginSearchRadius.setText(resources.getString("lblOriginSearchRadius.text"));
        lblOriginSearchRadius.setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        lblOriginSearchRadius.setName("lblOriginSearchRadius");

        spnOriginSearchRadius = new JSpinner(new SpinnerNumberModel(50, 10, 250, 10));
        spnOriginSearchRadius.setToolTipText(resources.getString("lblOriginSearchRadius.toolTipText"));
        spnOriginSearchRadius.setName("spnOriginSearchRadius");

        chkExtraRandomOrigin = new JCheckBox(resources.getString("chkExtraRandomOrigin.text"));
        chkExtraRandomOrigin.setToolTipText(resources.getString("chkExtraRandomOrigin.toolTipText"));
        chkExtraRandomOrigin.setName("chkExtraRandomOrigin");

        lblOriginDistanceScale.setText(resources.getString("lblOriginDistanceScale.text"));
        lblOriginDistanceScale.setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        lblOriginDistanceScale.setName("lblOriginDistanceScale");

        spnOriginDistanceScale = new JSpinner(new SpinnerNumberModel(0.6, 0.1, 2.0, 0.1));
        spnOriginDistanceScale.setToolTipText(resources.getString("lblOriginDistanceScale.toolTipText"));
        spnOriginDistanceScale.setName("spnOriginDistanceScale");

        // Programmatically Assign Accessibility Labels
        lblOriginSearchRadius.setLabelFor(spnOriginSearchRadius);
        lblOriginDistanceScale.setLabelFor(spnOriginDistanceScale);

        // Disable Panel by Default
        chkRandomizeOrigin.setSelected(true);
        chkRandomizeOrigin.doClick();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomOriginPanel.title")));
        panel.setName("randomOriginPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkRandomizeOrigin)
                        .addComponent(chkRandomizeDependentsOrigin)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(spnOriginSearchRadius, GroupLayout.Alignment.LEADING))
                        .addComponent(chkExtraRandomOrigin)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(spnOriginDistanceScale, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkRandomizeOrigin)
                        .addComponent(chkRandomizeDependentsOrigin)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginSearchRadius)
                                .addComponent(spnOriginSearchRadius))
                        .addComponent(chkExtraRandomOrigin)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblOriginDistanceScale)
                                .addComponent(spnOriginDistanceScale))
        );

        return panel;
    }

    private JPanel createFamilyPanel() {
        // Create Panel Components
        final JLabel lblDisplayFamilyLevel = new JLabel(resources.getString("lblDisplayFamilyLevel.text"));
        lblDisplayFamilyLevel.setToolTipText(resources.getString("lblDisplayFamilyLevel.toolTipText"));
        lblDisplayFamilyLevel.setName("lblDisplayFamilyLevel");

        comboDisplayFamilyLevel = new JComboBox<>(FamilialRelationshipDisplayLevel.values());
        comboDisplayFamilyLevel.setToolTipText(resources.getString("lblDisplayFamilyLevel.toolTipText"));
        comboDisplayFamilyLevel.setName("comboDisplayFamilyLevel");

        // Programmatically Assign Accessibility Labels
        lblDisplayFamilyLevel.setLabelFor(comboDisplayFamilyLevel);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("familyPanel.title")));
        panel.setName("familyPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblDisplayFamilyLevel)
                                .addComponent(comboDisplayFamilyLevel, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDisplayFamilyLevel)
                                .addComponent(comboDisplayFamilyLevel))
        );

        return panel;
    }

    private JPanel createSalaryPanel() {
        // Create Panel Components
        final JPanel salaryMultiplierPanel = createSalaryMultiplierPanel();

        final JPanel salaryExperienceModifierPanel = createSalaryExperienceMultiplierPanel();

        final JPanel baseSalaryPanel = createBaseSalaryPanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryPanel.title")));
        panel.setName("salaryPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(salaryMultiplierPanel)
                        .addComponent(salaryExperienceModifierPanel)
                        .addComponent(baseSalaryPanel)
        );

        return panel;
    }

    private JPanel createSalaryMultiplierPanel() {
        // Create Panel Components
        final JLabel lblCommissionedSalary = new JLabel(resources.getString("lblCommissionedSalary.text"));
        lblCommissionedSalary.setToolTipText(resources.getString("lblCommissionedSalary.toolTipText"));
        lblCommissionedSalary.setName("lblCommissionedSalary");

        spnCommissionedSalary = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
        spnCommissionedSalary.setToolTipText(resources.getString("lblCommissionedSalary.toolTipText"));
        spnCommissionedSalary.setName("spnCommissionedSalary");

        final JLabel lblEnlistedSalary = new JLabel(resources.getString("lblEnlistedSalary.text"));
        lblEnlistedSalary.setToolTipText(resources.getString("lblEnlistedSalary.toolTipText"));
        lblEnlistedSalary.setName("lblEnlistedSalary");

        spnEnlistedSalary = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
        spnEnlistedSalary.setToolTipText(resources.getString("lblEnlistedSalary.toolTipText"));
        spnEnlistedSalary.setName("spnEnlistedSalary");

        final JLabel lblAntiMekSalary = new JLabel(resources.getString("lblAntiMekSalary.text"));
        lblAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        lblAntiMekSalary.setName("lblAntiMekSalary");

        spnAntiMekSalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnAntiMekSalary.setToolTipText(resources.getString("lblAntiMekSalary.toolTipText"));
        spnAntiMekSalary.setName("spnAntiMekSalary");

        final JLabel lblSpecialistInfantrySalary = new JLabel(resources.getString("lblSpecialistInfantrySalary.text"));
        lblSpecialistInfantrySalary.setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        lblSpecialistInfantrySalary.setName("lblSpecialistInfantrySalary");

        spnSpecialistInfantrySalary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.05));
        spnSpecialistInfantrySalary.setToolTipText(resources.getString("lblSpecialistInfantrySalary.toolTipText"));
        spnSpecialistInfantrySalary.setName("spnSpecialistInfantrySalary");

        // Programmatically Assign Accessibility Labels
        lblCommissionedSalary.setLabelFor(spnCommissionedSalary);
        lblEnlistedSalary.setLabelFor(spnEnlistedSalary);
        lblAntiMekSalary.setLabelFor(spnAntiMekSalary);
        lblSpecialistInfantrySalary.setLabelFor(spnSpecialistInfantrySalary);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryMultiplierPanel.title")));
        panel.setToolTipText(resources.getString("salaryMultiplierPanel.toolTipText"));
        panel.setName("salaryMultiplierPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCommissionedSalary)
                                .addComponent(spnCommissionedSalary)
                                .addComponent(lblEnlistedSalary)
                                .addComponent(spnEnlistedSalary)
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCommissionedSalary)
                                .addComponent(spnCommissionedSalary)
                                .addComponent(lblEnlistedSalary)
                                .addComponent(spnEnlistedSalary)
                                .addComponent(lblAntiMekSalary)
                                .addComponent(spnAntiMekSalary)
                                .addComponent(lblSpecialistInfantrySalary)
                                .addComponent(spnSpecialistInfantrySalary))
        );

        return panel;
    }

    private JPanel createSalaryExperienceMultiplierPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 10));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("salaryExperienceMultiplierPanel.title")));
        panel.setToolTipText(resources.getString("salaryExperienceMultiplierPanel.toolTipText"));
        panel.setName("salaryExperienceMultiplierPanel");

        spnSalaryExperienceMultipliers = new JSpinner[5];
        for (int i = 0; i < 5; i++) {
            final String skillLevel = SkillType.getExperienceLevelName(i);
            final String toolTipText = String.format(resources.getString("lblSalaryExperienceMultiplier.toolTipText"), skillLevel);

            final JLabel label = new JLabel(skillLevel);
            label.setToolTipText(toolTipText);
            label.setName("lbl" + skillLevel);
            panel.add(label);

            spnSalaryExperienceMultipliers[i] = new JSpinner(new SpinnerNumberModel(0, 0, 10, 0.05));
            spnSalaryExperienceMultipliers[i].setToolTipText(toolTipText);
            spnSalaryExperienceMultipliers[i].setName("spn" + skillLevel);
            panel.add(spnSalaryExperienceMultipliers[i]);

            label.setLabelFor(spnSalaryExperienceMultipliers[i]);
        }

        return panel;
    }

    private JPanel createBaseSalaryPanel() {
        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        final JPanel panel = new JPanel(new GridLayout((int) Math.ceil((double) (personnelRoles.length - 1) / 3.0), 6));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("baseSalaryPanel.title")));
        panel.setPreferredSize(new Dimension(200, 200));

        spnBaseSalary = new JSpinner[personnelRoles.length];
        for (final PersonnelRole personnelRole : personnelRoles) {
            // Create Reused Values
            final String toolTipText = String.format(resources.getString("lblBaseSalary.toolTipText"), personnelRole.toString());

            // Create Panel Components
            final JLabel label = new JLabel(personnelRole.toString());
            label.setToolTipText(toolTipText);
            label.setName("lbl" + personnelRole.toString());
            panel.add(label);

            final JSpinner salarySpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 10.0));
            salarySpinner.setToolTipText(toolTipText);
            salarySpinner.setName("spn" + personnelRole.toString());
            panel.add(salarySpinner);

            // Programmatically Assign Accessibility Labels
            label.setLabelFor(salarySpinner);

            // Component Tracking Assignment
            spnBaseSalary[personnelRole.ordinal()] = salarySpinner;
        }

        return panel;
    }

    private JPanel createMarriagePanel() {
        // Create Panel Components
        chkUseManualMarriages = new JCheckBox(resources.getString("chkUseManualMarriages.text"));
        chkUseManualMarriages.setToolTipText(resources.getString("chkUseManualMarriages.toolTipText"));
        chkUseManualMarriages.setName("chkUseManualMarriages");

        chkUseClannerMarriages = new JCheckBox(resources.getString("chkUseClannerMarriages.text"));
        chkUseClannerMarriages.setToolTipText(resources.getString("chkUseClannerMarriages.toolTipText"));
        chkUseClannerMarriages.setName("chkUseClannerMarriages");
        chkUseClannerMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerMarriages.setEnabled(!method.isNone() && chkUseClannerMarriages.isSelected());
        });

        chkUsePrisonerMarriages = new JCheckBox(resources.getString("chkUsePrisonerMarriages.text"));
        chkUsePrisonerMarriages.setToolTipText(resources.getString("chkUsePrisonerMarriages.toolTipText"));
        chkUsePrisonerMarriages.setName("chkUsePrisonerMarriages");
        chkUsePrisonerMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerMarriages.setEnabled(!method.isNone() && chkUsePrisonerMarriages.isSelected());
        });

        final JLabel lblMinimumMarriageAge = new JLabel(resources.getString("lblMinimumMarriageAge.text"));
        lblMinimumMarriageAge.setToolTipText(resources.getString("lblMinimumMarriageAge.toolTipText"));
        lblMinimumMarriageAge.setName("lblMinimumMarriageAge");

        spnMinimumMarriageAge = new JSpinner(new SpinnerNumberModel(16, 14, null, 1));
        spnMinimumMarriageAge.setToolTipText(resources.getString("lblMinimumMarriageAge.toolTipText"));
        spnMinimumMarriageAge.setName("spnMinimumMarriageAge");

        final JLabel lblCheckMutualAncestorsDepth = new JLabel(resources.getString("lblCheckMutualAncestorsDepth.text"));
        lblCheckMutualAncestorsDepth.setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        lblCheckMutualAncestorsDepth.setName("lblCheckMutualAncestorsDepth");

        spnCheckMutualAncestorsDepth = new JSpinner(new SpinnerNumberModel(4, 0, 20, 1));
        spnCheckMutualAncestorsDepth.setToolTipText(resources.getString("lblCheckMutualAncestorsDepth.toolTipText"));
        spnCheckMutualAncestorsDepth.setName("spnCheckMutualAncestorsDepth");

        chkLogMarriageNameChanges = new JCheckBox(resources.getString("chkLogMarriageNameChanges.text"));
        chkLogMarriageNameChanges.setToolTipText(resources.getString("chkLogMarriageNameChanges.toolTipText"));
        chkLogMarriageNameChanges.setName("chkLogMarriageNameChanges");

        final JPanel marriageSurnameWeightsPanel = createMarriageSurnameWeightsPanel();

        final JPanel randomMarriagePanel = createRandomMarriagePanel();

        // Programmatically Assign Accessibility Labels
        lblMinimumMarriageAge.setLabelFor(spnMinimumMarriageAge);
        lblCheckMutualAncestorsDepth.setLabelFor(spnCheckMutualAncestorsDepth);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("marriagePanel.title")));
        panel.setName("marriagePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualMarriages)
                        .addComponent(chkUseClannerMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMinimumMarriageAge)
                                .addComponent(spnMinimumMarriageAge, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth, GroupLayout.Alignment.LEADING))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualMarriages)
                        .addComponent(chkUseClannerMarriages)
                        .addComponent(chkUsePrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumMarriageAge)
                                .addComponent(spnMinimumMarriageAge))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCheckMutualAncestorsDepth)
                                .addComponent(spnCheckMutualAncestorsDepth))
                        .addComponent(chkLogMarriageNameChanges)
                        .addComponent(marriageSurnameWeightsPanel)
                        .addComponent(randomMarriagePanel)
        );

        return panel;
    }

    private JPanel createMarriageSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("marriageSurnameWeightsPanel.title")));
        panel.setToolTipText(resources.getString("marriageSurnameWeightsPanel.toolTipText"));
        panel.setName("marriageSurnameWeightsPanel");

        spnMarriageSurnameWeights = new HashMap<>();
        for (final MergingSurnameStyle style : MergingSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            final JLabel label = new JLabel(style.toString());
            label.setToolTipText(style.getToolTipText());
            label.setName("lbl" + style);
            panel.add(label);

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
            spinner.setToolTipText(style.getToolTipText());
            spinner.setName("spn" + style);
            spnMarriageSurnameWeights.put(style, spinner);
            panel.add(spinner);

            label.setLabelFor(spinner);
        }

        return panel;
    }

    private JPanel createRandomMarriagePanel() {
        // Initialize Components Used in ActionListeners
        final JLabel lblRandomMarriageAgeRange = new JLabel();
        final JPanel percentageRandomMarriagePanel = new JDisableablePanel("percentageRandomMarriagePanel");

        // Create Panel Components
        final JLabel lblRandomMarriageMethod = new JLabel(resources.getString("lblRandomMarriageMethod.text"));
        lblRandomMarriageMethod.setToolTipText(resources.getString("lblRandomMarriageMethod.toolTipText"));
        lblRandomMarriageMethod.setName("lblRandomMarriageMethod");

        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod", RandomMarriageMethod.values());
        comboRandomMarriageMethod.setToolTipText(resources.getString("lblRandomMarriageMethod.toolTipText"));
        comboRandomMarriageMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomMarriageMethod) {
                    list.setToolTipText(((RandomMarriageMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomMarriageMethod.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean sameSexEnabled = enabled && chkUseRandomSameSexMarriages.isSelected();
            final boolean percentageEnabled = method.isPercentage();
            chkUseRandomSameSexMarriages.setEnabled(enabled);
            chkUseRandomClannerMarriages.setEnabled(enabled && chkUseClannerMarriages.isSelected());
            chkUseRandomPrisonerMarriages.setEnabled(enabled && chkUsePrisonerMarriages.isSelected());
            lblRandomMarriageAgeRange.setEnabled(enabled);
            spnRandomMarriageAgeRange.setEnabled(enabled);
            percentageRandomMarriagePanel.setEnabled(percentageEnabled);
            lblPercentageRandomMarriageSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
            spnPercentageRandomMarriageSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
        });

        chkUseRandomSameSexMarriages = new JCheckBox(resources.getString("chkUseRandomSameSexMarriages.text"));
        chkUseRandomSameSexMarriages.setToolTipText(resources.getString("chkUseRandomSameSexMarriages.toolTipText"));
        chkUseRandomSameSexMarriages.setName("chkUseRandomSameSexMarriages");
        chkUseRandomSameSexMarriages.addActionListener(evt -> {
            final RandomMarriageMethod method = comboRandomMarriageMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean sameSexEnabled = chkUseRandomSameSexMarriages.isEnabled()
                    && chkUseRandomSameSexMarriages.isSelected();
            final boolean percentageEnabled = sameSexEnabled && method.isPercentage();
            lblPercentageRandomMarriageSameSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomMarriageSameSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerMarriages = new JCheckBox(resources.getString("chkUseRandomClannerMarriages.text"));
        chkUseRandomClannerMarriages.setToolTipText(resources.getString("chkUseRandomClannerMarriages.toolTipText"));
        chkUseRandomClannerMarriages.setName("chkUseRandomClannerMarriages");

        chkUseRandomPrisonerMarriages = new JCheckBox(resources.getString("chkUseRandomPrisonerMarriages.text"));
        chkUseRandomPrisonerMarriages.setToolTipText(resources.getString("chkUseRandomPrisonerMarriages.toolTipText"));
        chkUseRandomPrisonerMarriages.setName("chkUseRandomPrisonerMarriages");

        lblRandomMarriageAgeRange.setText(resources.getString("lblRandomMarriageAgeRange.text"));
        lblRandomMarriageAgeRange.setToolTipText(resources.getString("lblRandomMarriageAgeRange.toolTipText"));
        lblRandomMarriageAgeRange.setName("lblRandomMarriageAgeRange");

        spnRandomMarriageAgeRange = new JSpinner(new SpinnerNumberModel(10, 0, null, 1.0));
        spnRandomMarriageAgeRange.setToolTipText(resources.getString("lblRandomMarriageAgeRange.toolTipText"));
        spnRandomMarriageAgeRange.setName("spnRandomMarriageAgeRange");

        createPercentageRandomMarriagePanel(percentageRandomMarriagePanel);

        // Programmatically Assign Accessibility Labels
        lblRandomMarriageMethod.setLabelFor(comboRandomMarriageMethod);
        lblRandomMarriageAgeRange.setLabelFor(spnRandomMarriageAgeRange);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomMarriagePanel.title")));
        panel.setName("randomMarriagePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomSameSexMarriages)
                        .addComponent(chkUseRandomClannerMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange, GroupLayout.Alignment.LEADING))
                        .addComponent(percentageRandomMarriagePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageMethod)
                                .addComponent(comboRandomMarriageMethod))
                        .addComponent(chkUseRandomSameSexMarriages)
                        .addComponent(chkUseRandomClannerMarriages)
                        .addComponent(chkUseRandomPrisonerMarriages)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomMarriageAgeRange)
                                .addComponent(spnRandomMarriageAgeRange))
                        .addComponent(percentageRandomMarriagePanel)
        );

        return panel;
    }

    private void createPercentageRandomMarriagePanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblPercentageRandomMarriageOppositeSexChance = new JLabel(resources.getString("lblPercentageRandomMarriageOppositeSexChance.text"));
        lblPercentageRandomMarriageOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageOppositeSexChance.toolTipText"));
        lblPercentageRandomMarriageOppositeSexChance.setName("lblPercentageRandomMarriageOppositeSexChance");

        spnPercentageRandomMarriageOppositeSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomMarriageOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageOppositeSexChance.toolTipText"));
        spnPercentageRandomMarriageOppositeSexChance.setName("spnPercentageRandomMarriageOppositeSexChance");

        lblPercentageRandomMarriageSameSexChance = new JLabel(resources.getString("lblPercentageRandomMarriageSameSexChance.text"));
        lblPercentageRandomMarriageSameSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageSameSexChance.toolTipText"));
        lblPercentageRandomMarriageSameSexChance.setName("lblPercentageRandomMarriageSameSexChance");

        spnPercentageRandomMarriageSameSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomMarriageSameSexChance.setToolTipText(resources.getString("lblPercentageRandomMarriageSameSexChance.toolTipText"));
        spnPercentageRandomMarriageSameSexChance.setName("spnPercentageRandomMarriageSameSexChance");

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomMarriageOppositeSexChance.setLabelFor(spnPercentageRandomMarriageOppositeSexChance);
        lblPercentageRandomMarriageSameSexChance.setLabelFor(spnPercentageRandomMarriageSameSexChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomMarriagePanel.title")));
        panel.setToolTipText(RandomMarriageMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomMarriageOppositeSexChance)
                                .addComponent(spnPercentageRandomMarriageOppositeSexChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomMarriageSameSexChance)
                                .addComponent(spnPercentageRandomMarriageSameSexChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomMarriageOppositeSexChance)
                                .addComponent(spnPercentageRandomMarriageOppositeSexChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomMarriageSameSexChance)
                                .addComponent(spnPercentageRandomMarriageSameSexChance))
        );
    }

    private JPanel createDivorcePanel() {
        // Create Panel Components
        chkUseManualDivorce = new JCheckBox(resources.getString("chkUseManualDivorce.text"));
        chkUseManualDivorce.setToolTipText(resources.getString("chkUseManualDivorce.toolTipText"));
        chkUseManualDivorce.setName("chkUseManualDivorce");

        chkUseClannerDivorce = new JCheckBox(resources.getString("chkUseClannerDivorce.text"));
        chkUseClannerDivorce.setToolTipText(resources.getString("chkUseClannerDivorce.toolTipText"));
        chkUseClannerDivorce.setName("chkUseClannerDivorce");
        chkUseClannerDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerDivorce.setEnabled(!method.isNone() && chkUseClannerDivorce.isSelected());
        });

        chkUsePrisonerDivorce = new JCheckBox(resources.getString("chkUsePrisonerDivorce.text"));
        chkUsePrisonerDivorce.setToolTipText(resources.getString("chkUsePrisonerDivorce.toolTipText"));
        chkUsePrisonerDivorce.setName("chkUsePrisonerDivorce");
        chkUsePrisonerDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerDivorce.setEnabled(!method.isNone() && chkUsePrisonerDivorce.isSelected());
        });

        final JPanel divorceSurnameWeightsPanel = createDivorceSurnameWeightsPanel();

        final JPanel randomDivorcePanel = createRandomDivorcePanel();

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("divorcePanel.title")));
        panel.setName("divorcePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClannerDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualDivorce)
                        .addComponent(chkUseClannerDivorce)
                        .addComponent(chkUsePrisonerDivorce)
                        .addComponent(divorceSurnameWeightsPanel)
                        .addComponent(randomDivorcePanel)
        );

        return panel;
    }

    private JPanel createDivorceSurnameWeightsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("divorceSurnameWeightsPanel.title")));
        panel.setToolTipText(resources.getString("divorceSurnameWeightsPanel.toolTipText"));
        panel.setName("divorceSurnameWeightsPanel");

        spnDivorceSurnameWeights = new HashMap<>();
        for (final SplittingSurnameStyle style : SplittingSurnameStyle.values()) {
            if (style.isWeighted()) {
                continue;
            }
            final JLabel label = new JLabel(style.toString());
            label.setToolTipText(style.getToolTipText());
            label.setName("lbl" + style);
            panel.add(label);

            final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
            spinner.setToolTipText(style.getToolTipText());
            spinner.setName("spn" + style);
            spnDivorceSurnameWeights.put(style, spinner);
            panel.add(spinner);

            label.setLabelFor(spinner);
        }

        return panel;
    }

    private JPanel createRandomDivorcePanel() {
        // Initialize Components Used in ActionListeners
        final JPanel percentageRandomDivorcePanel = new JDisableablePanel("percentageRandomDivorcePanel");

        // Create Panel Components
        final JLabel lblRandomDivorceMethod = new JLabel(resources.getString("lblRandomDivorceMethod.text"));
        lblRandomDivorceMethod.setToolTipText(resources.getString("lblRandomDivorceMethod.toolTipText"));
        lblRandomDivorceMethod.setName("lblRandomDivorceMethod");

        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        comboRandomDivorceMethod.setToolTipText(resources.getString("lblRandomDivorceMethod.toolTipText"));
        comboRandomDivorceMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomDivorceMethod) {
                    list.setToolTipText(((RandomDivorceMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomDivorceMethod.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean oppositeSexEnabled = enabled && chkUseRandomOppositeSexDivorce.isSelected();
            final boolean sameSexEnabled = enabled && chkUseRandomSameSexDivorce.isSelected();
            final boolean percentageEnabled = method.isPercentage();
            chkUseRandomOppositeSexDivorce.setEnabled(enabled);
            chkUseRandomSameSexDivorce.setEnabled(enabled);
            chkUseRandomClannerDivorce.setEnabled(enabled && chkUseClannerDivorce.isSelected());
            chkUseRandomPrisonerDivorce.setEnabled(enabled && chkUsePrisonerDivorce.isSelected());
            percentageRandomDivorcePanel.setEnabled(percentageEnabled);
            lblPercentageRandomDivorceOppositeSexChance.setEnabled(oppositeSexEnabled && percentageEnabled);
            spnPercentageRandomDivorceOppositeSexChance.setEnabled(oppositeSexEnabled && percentageEnabled);
            lblPercentageRandomDivorceSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
            spnPercentageRandomDivorceSameSexChance.setEnabled(sameSexEnabled && percentageEnabled);
        });

        chkUseRandomOppositeSexDivorce = new JCheckBox(resources.getString("chkUseRandomOppositeSexDivorce.text"));
        chkUseRandomOppositeSexDivorce.setToolTipText(resources.getString("chkUseRandomOppositeSexDivorce.toolTipText"));
        chkUseRandomOppositeSexDivorce.setName("chkUseRandomOppositeSexDivorce");
        chkUseRandomOppositeSexDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean selected = chkUseRandomOppositeSexDivorce.isEnabled()
                    && chkUseRandomOppositeSexDivorce.isSelected();
            final boolean percentageEnabled = selected && method.isPercentage();
            lblPercentageRandomDivorceOppositeSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomDivorceOppositeSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomSameSexDivorce = new JCheckBox(resources.getString("chkUseRandomSameSexDivorce.text"));
        chkUseRandomSameSexDivorce.setToolTipText(resources.getString("chkUseRandomSameSexDivorce.toolTipText"));
        chkUseRandomSameSexDivorce.setName("chkUseRandomSameSexDivorce");
        chkUseRandomSameSexDivorce.addActionListener(evt -> {
            final RandomDivorceMethod method = comboRandomDivorceMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean selected = chkUseRandomSameSexDivorce.isEnabled()
                    && chkUseRandomSameSexDivorce.isSelected();
            final boolean percentageEnabled = selected && method.isPercentage();
            lblPercentageRandomDivorceSameSexChance.setEnabled(percentageEnabled);
            spnPercentageRandomDivorceSameSexChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerDivorce = new JCheckBox(resources.getString("chkUseRandomClannerDivorce.text"));
        chkUseRandomClannerDivorce.setToolTipText(resources.getString("chkUseRandomClannerDivorce.toolTipText"));
        chkUseRandomClannerDivorce.setName("chkUseRandomClannerDivorce");

        chkUseRandomPrisonerDivorce = new JCheckBox(resources.getString("chkUseRandomPrisonerDivorce.text"));
        chkUseRandomPrisonerDivorce.setToolTipText(resources.getString("chkUseRandomPrisonerDivorce.toolTipText"));
        chkUseRandomPrisonerDivorce.setName("chkUseRandomPrisonerDivorce");

        createPercentageRandomDivorcePanel(percentageRandomDivorcePanel);

        // Programmatically Assign Accessibility Labels
        lblRandomDivorceMethod.setLabelFor(comboRandomDivorceMethod);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomDivorcePanel.title")));
        panel.setName("randomDivorcePanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClannerDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomDivorceMethod)
                                .addComponent(comboRandomDivorceMethod))
                        .addComponent(chkUseRandomOppositeSexDivorce)
                        .addComponent(chkUseRandomSameSexDivorce)
                        .addComponent(chkUseRandomClannerDivorce)
                        .addComponent(chkUseRandomPrisonerDivorce)
                        .addComponent(percentageRandomDivorcePanel)
        );

        return panel;
    }

    private void createPercentageRandomDivorcePanel(final JPanel panel) {
        // Create Panel Components
        lblPercentageRandomDivorceOppositeSexChance = new JLabel(resources.getString("lblPercentageRandomDivorceOppositeSexChance.text"));
        lblPercentageRandomDivorceOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceOppositeSexChance.toolTipText"));
        lblPercentageRandomDivorceOppositeSexChance.setName("lblPercentageRandomDivorceOppositeSexChance");

        spnPercentageRandomDivorceOppositeSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.00001));
        spnPercentageRandomDivorceOppositeSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceOppositeSexChance.toolTipText"));
        spnPercentageRandomDivorceOppositeSexChance.setName("spnPercentageRandomDivorceOppositeSexChance");
        spnPercentageRandomDivorceOppositeSexChance.setEditor(new JSpinner.NumberEditor(spnPercentageRandomDivorceOppositeSexChance, "0.00000"));

        lblPercentageRandomDivorceSameSexChance = new JLabel(resources.getString("lblPercentageRandomDivorceSameSexChance.text"));
        lblPercentageRandomDivorceSameSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceSameSexChance.toolTipText"));
        lblPercentageRandomDivorceSameSexChance.setName("lblPercentageRandomDivorceSameSexChance");

        spnPercentageRandomDivorceSameSexChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.00001));
        spnPercentageRandomDivorceSameSexChance.setToolTipText(resources.getString("lblPercentageRandomDivorceSameSexChance.toolTipText"));
        spnPercentageRandomDivorceSameSexChance.setName("spnPercentageRandomDivorceSameSexChance");
        spnPercentageRandomDivorceSameSexChance.setEditor(new JSpinner.NumberEditor(spnPercentageRandomDivorceSameSexChance, "0.00000"));

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomDivorceOppositeSexChance.setLabelFor(spnPercentageRandomDivorceOppositeSexChance);
        lblPercentageRandomDivorceSameSexChance.setLabelFor(spnPercentageRandomDivorceSameSexChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomDivorcePanel.title")));
        panel.setToolTipText(RandomDivorceMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomDivorceOppositeSexChance)
                                .addComponent(spnPercentageRandomDivorceOppositeSexChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomDivorceSameSexChance)
                                .addComponent(spnPercentageRandomDivorceSameSexChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomDivorceOppositeSexChance)
                                .addComponent(spnPercentageRandomDivorceOppositeSexChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomDivorceSameSexChance)
                                .addComponent(spnPercentageRandomDivorceSameSexChance))
        );
    }

    private JPanel createProcreationPanel() {
        // Create Panel Components
        chkUseManualProcreation = new JCheckBox(resources.getString("chkUseManualProcreation.text"));
        chkUseManualProcreation.setToolTipText(resources.getString("chkUseManualProcreation.toolTipText"));
        chkUseManualProcreation.setName("chkUseManualProcreation");

        chkUseClannerProcreation = new JCheckBox(resources.getString("chkUseClannerProcreation.text"));
        chkUseClannerProcreation.setToolTipText(resources.getString("chkUseClannerProcreation.toolTipText"));
        chkUseClannerProcreation.setName("chkUseClannerProcreation");
        chkUseClannerProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomClannerProcreation.setEnabled(!method.isNone() && chkUseClannerProcreation.isSelected());
        });

        chkUsePrisonerProcreation = new JCheckBox(resources.getString("chkUsePrisonerProcreation.text"));
        chkUsePrisonerProcreation.setToolTipText(resources.getString("chkUsePrisonerProcreation.toolTipText"));
        chkUsePrisonerProcreation.setName("chkUsePrisonerProcreation");
        chkUsePrisonerProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            chkUseRandomPrisonerProcreation.setEnabled(!method.isNone() && chkUsePrisonerProcreation.isSelected());
        });

        final JLabel lblMultiplePregnancyOccurrences = new JLabel(resources.getString("lblMultiplePregnancyOccurrences.text"));
        lblMultiplePregnancyOccurrences.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        lblMultiplePregnancyOccurrences.setName("lblMultiplePregnancyOccurrences");

        spnMultiplePregnancyOccurrences = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        spnMultiplePregnancyOccurrences.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        spnMultiplePregnancyOccurrences.setName("spnMultiplePregnancyOccurrences");

        final JLabel lblMultiplePregnancyOccurrencesEnd = new JLabel(resources.getString("lblMultiplePregnancyOccurrencesEnd.text"));
        lblMultiplePregnancyOccurrencesEnd.setToolTipText(resources.getString("lblMultiplePregnancyOccurrences.toolTipText"));
        lblMultiplePregnancyOccurrencesEnd.setName("lblMultiplePregnancyOccurrencesEnd");

        final JLabel lblBabySurnameStyle = new JLabel(resources.getString("lblBabySurnameStyle.text"));
        lblBabySurnameStyle.setToolTipText(resources.getString("lblBabySurnameStyle.toolTipText"));
        lblBabySurnameStyle.setName("lblBabySurnameStyle");

        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        comboBabySurnameStyle.setToolTipText(resources.getString("lblBabySurnameStyle.toolTipText"));
        comboBabySurnameStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BabySurnameStyle) {
                    list.setToolTipText(((BabySurnameStyle) value).getToolTipText());
                }
                return this;
            }
        });

        chkAssignNonPrisonerBabiesFounderTag = new JCheckBox(resources.getString("chkAssignNonPrisonerBabiesFounderTag.text"));
        chkAssignNonPrisonerBabiesFounderTag.setToolTipText(resources.getString("chkAssignNonPrisonerBabiesFounderTag.toolTipText"));
        chkAssignNonPrisonerBabiesFounderTag.setName("chkAssignNonPrisonerBabiesFounderTag");

        chkAssignChildrenOfFoundersFounderTag = new JCheckBox(resources.getString("chkAssignChildrenOfFoundersFounderTag.text"));
        chkAssignChildrenOfFoundersFounderTag.setToolTipText(resources.getString("chkAssignChildrenOfFoundersFounderTag.toolTipText"));
        chkAssignChildrenOfFoundersFounderTag.setName("chkAssignChildrenOfFoundersFounderTag");

        chkDetermineFatherAtBirth = new JCheckBox(resources.getString("chkDetermineFatherAtBirth.text"));
        chkDetermineFatherAtBirth.setToolTipText(resources.getString("chkDetermineFatherAtBirth.toolTipText"));
        chkDetermineFatherAtBirth.setName("chkDetermineFatherAtBirth");

        chkDisplayTrueDueDate = new JCheckBox(resources.getString("chkDisplayTrueDueDate.text"));
        chkDisplayTrueDueDate.setToolTipText(resources.getString("chkDisplayTrueDueDate.toolTipText"));
        chkDisplayTrueDueDate.setName("chkDisplayTrueDueDate");

        chkLogProcreation = new JCheckBox(resources.getString("chkLogProcreation.text"));
        chkLogProcreation.setToolTipText(resources.getString("chkLogProcreation.toolTipText"));
        chkLogProcreation.setName("chkLogProcreation");

        final JPanel randomProcreationPanel = createRandomProcreationPanel();

        // Programmatically Assign Accessibility Labels
        lblMultiplePregnancyOccurrences.setLabelFor(spnMultiplePregnancyOccurrences);
        lblBabySurnameStyle.setLabelFor(comboBabySurnameStyle);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("procreationPanel.title")));
        panel.setName("procreationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseManualProcreation)
                        .addComponent(chkUseClannerProcreation)
                        .addComponent(chkUsePrisonerProcreation)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMultiplePregnancyOccurrences)
                                .addComponent(spnMultiplePregnancyOccurrences)
                                .addComponent(lblMultiplePregnancyOccurrencesEnd, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle, GroupLayout.Alignment.LEADING))
                        .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                        .addComponent(chkAssignChildrenOfFoundersFounderTag)
                        .addComponent(chkDetermineFatherAtBirth)
                        .addComponent(chkDisplayTrueDueDate)
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseManualProcreation)
                        .addComponent(chkUseClannerProcreation)
                        .addComponent(chkUsePrisonerProcreation)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMultiplePregnancyOccurrences)
                                .addComponent(spnMultiplePregnancyOccurrences)
                                .addComponent(lblMultiplePregnancyOccurrencesEnd))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBabySurnameStyle)
                                .addComponent(comboBabySurnameStyle))
                        .addComponent(chkAssignNonPrisonerBabiesFounderTag)
                        .addComponent(chkAssignChildrenOfFoundersFounderTag)
                        .addComponent(chkDetermineFatherAtBirth)
                        .addComponent(chkDisplayTrueDueDate)
                        .addComponent(chkLogProcreation)
                        .addComponent(randomProcreationPanel)
        );

        return panel;
    }

    private JPanel createRandomProcreationPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel percentageRandomProcreationPanel = new JDisableablePanel("percentageRandomProcreationPanel");

        // Create Panel Components
        final JLabel lblRandomProcreationMethod = new JLabel(resources.getString("lblRandomProcreationMethod.text"));
        lblRandomProcreationMethod.setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
        lblRandomProcreationMethod.setName("lblRandomProcreationMethod");

        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod", RandomProcreationMethod.values());
        comboRandomProcreationMethod.setToolTipText(resources.getString("lblRandomProcreationMethod.toolTipText"));
        comboRandomProcreationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RandomProcreationMethod) {
                    list.setToolTipText(((RandomProcreationMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboRandomProcreationMethod.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean enabled = !method.isNone();
            final boolean percentageEnabled = method.isPercentage();
            final boolean relationshiplessEnabled = enabled && chkUseRelationshiplessRandomProcreation.isSelected();
            chkUseRelationshiplessRandomProcreation.setEnabled(enabled);
            chkUseRandomClannerProcreation.setEnabled(enabled && chkUseClannerProcreation.isSelected());
            chkUseRandomPrisonerProcreation.setEnabled(enabled && chkUsePrisonerProcreation.isSelected());
            percentageRandomProcreationPanel.setEnabled(percentageEnabled);
            lblPercentageRandomProcreationRelationshiplessChance.setEnabled(relationshiplessEnabled && percentageEnabled);
            spnPercentageRandomProcreationRelationshiplessChance.setEnabled(relationshiplessEnabled && percentageEnabled);
        });

        chkUseRelationshiplessRandomProcreation = new JCheckBox(resources.getString("chkUseRelationshiplessRandomProcreation.text"));
        chkUseRelationshiplessRandomProcreation.setToolTipText(resources.getString("chkUseRelationshiplessRandomProcreation.toolTipText"));
        chkUseRelationshiplessRandomProcreation.setName("chkUseRelationshiplessRandomProcreation");
        chkUseRelationshiplessRandomProcreation.addActionListener(evt -> {
            final RandomProcreationMethod method = comboRandomProcreationMethod.getSelectedItem();
            if (method == null) {
                return;
            }
            final boolean sameSexEnabled = chkUseRelationshiplessRandomProcreation.isEnabled()
                    && chkUseRelationshiplessRandomProcreation.isSelected();
            final boolean percentageEnabled = sameSexEnabled && method.isPercentage();
            lblPercentageRandomProcreationRelationshiplessChance.setEnabled(percentageEnabled);
            spnPercentageRandomProcreationRelationshiplessChance.setEnabled(percentageEnabled);
        });

        chkUseRandomClannerProcreation = new JCheckBox(resources.getString("chkUseRandomClannerProcreation.text"));
        chkUseRandomClannerProcreation.setToolTipText(resources.getString("chkUseRandomClannerProcreation.toolTipText"));
        chkUseRandomClannerProcreation.setName("chkUseRandomClannerProcreation");

        chkUseRandomPrisonerProcreation = new JCheckBox(resources.getString("chkUseRandomPrisonerProcreation.text"));
        chkUseRandomPrisonerProcreation.setToolTipText(resources.getString("chkUseRandomPrisonerProcreation.toolTipText"));
        chkUseRandomPrisonerProcreation.setName("chkUseRandomPrisonerProcreation");

        createPercentageRandomProcreationPanel(percentageRandomProcreationPanel);

        // Programmatically Assign Accessibility Labels
        lblRandomProcreationMethod.setLabelFor(comboRandomProcreationMethod);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("randomProcreationPanel.title")));
        panel.setName("randomProcreationPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClannerProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomProcreationMethod)
                                .addComponent(comboRandomProcreationMethod))
                        .addComponent(chkUseRelationshiplessRandomProcreation)
                        .addComponent(chkUseRandomClannerProcreation)
                        .addComponent(chkUseRandomPrisonerProcreation)
                        .addComponent(percentageRandomProcreationPanel)
        );

        return panel;
    }

    private void createPercentageRandomProcreationPanel(final JPanel panel) {
        // Create Panel Components
        final JLabel lblPercentageRandomProcreationRelationshipChance = new JLabel(resources.getString("lblPercentageRandomProcreationRelationshipChance.text"));
        lblPercentageRandomProcreationRelationshipChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshipChance.toolTipText"));
        lblPercentageRandomProcreationRelationshipChance.setName("lblPercentageRandomProcreationRelationshipChance");

        spnPercentageRandomProcreationRelationshipChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomProcreationRelationshipChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshipChance.toolTipText"));
        spnPercentageRandomProcreationRelationshipChance.setName("spnChanceProcreation");

        lblPercentageRandomProcreationRelationshiplessChance = new JLabel(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.text"));
        lblPercentageRandomProcreationRelationshiplessChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.toolTipText"));
        lblPercentageRandomProcreationRelationshiplessChance.setName("lblPercentageRandomProcreationRelationshiplessChance");

        spnPercentageRandomProcreationRelationshiplessChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.001));
        spnPercentageRandomProcreationRelationshiplessChance.setToolTipText(resources.getString("lblPercentageRandomProcreationRelationshiplessChance.toolTipText"));
        spnPercentageRandomProcreationRelationshiplessChance.setName("spnPercentageRandomProcreationRelationshiplessChance");

        // Programmatically Assign Accessibility Labels
        lblPercentageRandomProcreationRelationshipChance.setLabelFor(spnPercentageRandomProcreationRelationshipChance);
        lblPercentageRandomProcreationRelationshiplessChance.setLabelFor(spnPercentageRandomProcreationRelationshiplessChance);

        // Layout the Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("percentageRandomProcreationPanel.title")));
        panel.setToolTipText(RandomProcreationMethod.PERCENTAGE.getToolTipText());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomProcreationRelationshipChance)
                                .addComponent(spnPercentageRandomProcreationRelationshipChance, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPercentageRandomProcreationRelationshiplessChance)
                                .addComponent(spnPercentageRandomProcreationRelationshiplessChance, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomProcreationRelationshipChance)
                                .addComponent(spnPercentageRandomProcreationRelationshipChance))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPercentageRandomProcreationRelationshiplessChance)
                                .addComponent(spnPercentageRandomProcreationRelationshiplessChance))
        );
    }

    private JPanel createDeathPanel() {
        // Create Panel Components
        chkKeepMarriedNameUponSpouseDeath = new JCheckBox(resources.getString("chkKeepMarriedNameUponSpouseDeath.text"));
        chkKeepMarriedNameUponSpouseDeath.setToolTipText(resources.getString("chkKeepMarriedNameUponSpouseDeath.toolTipText"));
        chkKeepMarriedNameUponSpouseDeath.setName("chkKeepMarriedNameUponSpouseDeath");

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("deathPanel.title")));
        panel.setName("deathPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkKeepMarriedNameUponSpouseDeath)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkKeepMarriedNameUponSpouseDeath)
        );

        return panel;
    }
    //endregion Personnel Tab

    //region Finances Tab
    private JPanel createPriceModifiersPanel() {
        // Create Panel Components
        final JLabel lblCommonPartPriceMultiplier = new JLabel(resources.getString("lblCommonPartPriceMultiplier.text"));
        lblCommonPartPriceMultiplier.setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        lblCommonPartPriceMultiplier.setName("lblCommonPartPriceMultiplier");

        spnCommonPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnCommonPartPriceMultiplier.setToolTipText(resources.getString("lblCommonPartPriceMultiplier.toolTipText"));
        spnCommonPartPriceMultiplier.setName("spnCommonPartPriceMultiplier");

        final JLabel lblInnerSphereUnitPriceMultiplier = new JLabel(resources.getString("lblInnerSphereUnitPriceMultiplier.text"));
        lblInnerSphereUnitPriceMultiplier.setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        lblInnerSphereUnitPriceMultiplier.setName("lblInnerSphereUnitPriceMultiplier");

        spnInnerSphereUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSphereUnitPriceMultiplier.setToolTipText(resources.getString("lblInnerSphereUnitPriceMultiplier.toolTipText"));
        spnInnerSphereUnitPriceMultiplier.setName("spnInnerSphereUnitPriceMultiplier");

        final JLabel lblInnerSpherePartPriceMultiplier = new JLabel(resources.getString("lblInnerSpherePartPriceMultiplier.text"));
        lblInnerSpherePartPriceMultiplier.setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        lblInnerSpherePartPriceMultiplier.setName("lblInnerSpherePartPriceMultiplier");

        spnInnerSpherePartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnInnerSpherePartPriceMultiplier.setToolTipText(resources.getString("lblInnerSpherePartPriceMultiplier.toolTipText"));
        spnInnerSpherePartPriceMultiplier.setName("spnInnerSpherePartPriceMultiplier");

        final JLabel lblClanUnitPriceMultiplier = new JLabel(resources.getString("lblClanUnitPriceMultiplier.text"));
        lblClanUnitPriceMultiplier.setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        lblClanUnitPriceMultiplier.setName("lblClanUnitPriceMultiplier");

        spnClanUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanUnitPriceMultiplier.setToolTipText(resources.getString("lblClanUnitPriceMultiplier.toolTipText"));
        spnClanUnitPriceMultiplier.setName("spnClanUnitPriceMultiplier");

        final JLabel lblClanPartPriceMultiplier = new JLabel(resources.getString("lblClanPartPriceMultiplier.text"));
        lblClanPartPriceMultiplier.setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        lblClanPartPriceMultiplier.setName("lblClanPartPriceMultiplier");

        spnClanPartPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnClanPartPriceMultiplier.setToolTipText(resources.getString("lblClanPartPriceMultiplier.toolTipText"));
        spnClanPartPriceMultiplier.setName("spnClanPartPriceMultiplier");

        final JLabel lblMixedTechUnitPriceMultiplier = new JLabel(resources.getString("lblMixedTechUnitPriceMultiplier.text"));
        lblMixedTechUnitPriceMultiplier.setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        lblMixedTechUnitPriceMultiplier.setName("lblMixedTechUnitPriceMultiplier");

        spnMixedTechUnitPriceMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.1, null, 0.1));
        spnMixedTechUnitPriceMultiplier.setToolTipText(resources.getString("lblMixedTechUnitPriceMultiplier.toolTipText"));
        spnMixedTechUnitPriceMultiplier.setName("spnMixedTechUnitPriceMultiplier");

        final JPanel usedPartsValueMultipliersPanel = createUsedPartsValueMultipliersPanel();

        final JLabel lblDamagedPartsValueMultiplier = new JLabel(resources.getString("lblDamagedPartsValueMultiplier.text"));
        lblDamagedPartsValueMultiplier.setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        lblDamagedPartsValueMultiplier.setName("lblDamagedPartsValueMultiplier");

        spnDamagedPartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.33, 0.00, 1.00, 0.05));
        spnDamagedPartsValueMultiplier.setToolTipText(resources.getString("lblDamagedPartsValueMultiplier.toolTipText"));
        spnDamagedPartsValueMultiplier.setName("spnDamagedPartsValueMultiplier");
        spnDamagedPartsValueMultiplier.setEditor(new JSpinner.NumberEditor(spnDamagedPartsValueMultiplier, "0.00"));

        final JLabel lblUnrepairablePartsValueMultiplier = new JLabel(resources.getString("lblUnrepairablePartsValueMultiplier.text"));
        lblUnrepairablePartsValueMultiplier.setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        lblUnrepairablePartsValueMultiplier.setName("lblUnrepairablePartsValueMultiplier");

        spnUnrepairablePartsValueMultiplier = new JSpinner(new SpinnerNumberModel(0.10, 0.00, 1.00, 0.05));
        spnUnrepairablePartsValueMultiplier.setToolTipText(resources.getString("lblUnrepairablePartsValueMultiplier.toolTipText"));
        spnUnrepairablePartsValueMultiplier.setName("spnUnrepairablePartsValueMultiplier");
        spnUnrepairablePartsValueMultiplier.setEditor(new JSpinner.NumberEditor(spnUnrepairablePartsValueMultiplier, "0.00"));

        final JLabel lblCancelledOrderRefundMultiplier = new JLabel(resources.getString("lblCancelledOrderRefundMultiplier.text"));
        lblCancelledOrderRefundMultiplier.setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        lblCancelledOrderRefundMultiplier.setName("lblCancelledOrderRefundMultiplier");

        spnCancelledOrderRefundMultiplier = new JSpinner(new SpinnerNumberModel(0.50, 0.00, 1.00, 0.05));
        spnCancelledOrderRefundMultiplier.setToolTipText(resources.getString("lblCancelledOrderRefundMultiplier.toolTipText"));
        spnCancelledOrderRefundMultiplier.setName("spnCancelledOrderRefundMultiplier");
        spnCancelledOrderRefundMultiplier.setEditor(new JSpinner.NumberEditor(spnCancelledOrderRefundMultiplier, "0.00"));

        // Programmatically Assign Accessibility Labels
        lblCommonPartPriceMultiplier.setLabelFor(spnCommonPartPriceMultiplier);
        lblInnerSphereUnitPriceMultiplier.setLabelFor(spnInnerSphereUnitPriceMultiplier);
        lblInnerSpherePartPriceMultiplier.setLabelFor(spnInnerSpherePartPriceMultiplier);
        lblClanUnitPriceMultiplier.setLabelFor(spnClanUnitPriceMultiplier);
        lblClanPartPriceMultiplier.setLabelFor(spnClanPartPriceMultiplier);
        lblMixedTechUnitPriceMultiplier.setLabelFor(spnMixedTechUnitPriceMultiplier);
        lblDamagedPartsValueMultiplier.setLabelFor(spnDamagedPartsValueMultiplier);
        lblUnrepairablePartsValueMultiplier.setLabelFor(spnUnrepairablePartsValueMultiplier);
        lblCancelledOrderRefundMultiplier.setLabelFor(spnCancelledOrderRefundMultiplier);

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("priceMultipliersPanel.title")));
        panel.setName("priceMultipliersPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCommonPartPriceMultiplier)
                                .addComponent(spnCommonPartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInnerSphereUnitPriceMultiplier)
                                .addComponent(spnInnerSphereUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInnerSpherePartPriceMultiplier)
                                .addComponent(spnInnerSpherePartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblClanUnitPriceMultiplier)
                                .addComponent(spnClanUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblClanPartPriceMultiplier)
                                .addComponent(spnClanPartPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMixedTechUnitPriceMultiplier)
                                .addComponent(spnMixedTechUnitPriceMultiplier, GroupLayout.Alignment.LEADING))
                        .addComponent(usedPartsValueMultipliersPanel)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblDamagedPartsValueMultiplier)
                                .addComponent(spnDamagedPartsValueMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnrepairablePartsValueMultiplier)
                                .addComponent(spnUnrepairablePartsValueMultiplier, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCancelledOrderRefundMultiplier)
                                .addComponent(spnCancelledOrderRefundMultiplier, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCommonPartPriceMultiplier)
                                .addComponent(spnCommonPartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInnerSphereUnitPriceMultiplier)
                                .addComponent(spnInnerSphereUnitPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInnerSpherePartPriceMultiplier)
                                .addComponent(spnInnerSpherePartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblClanUnitPriceMultiplier)
                                .addComponent(spnClanUnitPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblClanPartPriceMultiplier)
                                .addComponent(spnClanPartPriceMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMixedTechUnitPriceMultiplier)
                                .addComponent(spnMixedTechUnitPriceMultiplier))
                        .addComponent(usedPartsValueMultipliersPanel)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDamagedPartsValueMultiplier)
                                .addComponent(spnDamagedPartsValueMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnrepairablePartsValueMultiplier)
                                .addComponent(spnUnrepairablePartsValueMultiplier))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCancelledOrderRefundMultiplier)
                                .addComponent(spnCancelledOrderRefundMultiplier))
        );

        return panel;
    }

    private JPanel createUsedPartsValueMultipliersPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("usedPartsValueMultipliersPanel.title")));
        panel.setName("usedPartsValueMultipliersPanel");

        spnUsedPartPriceMultipliers = new JSpinner[Part.QUALITY_F + 1];
        for (int i = Part.QUALITY_A; i <= Part.QUALITY_F; i++) {
            final String qualityLevel = Part.getQualityName(i, false);

            final JLabel label = new JLabel(qualityLevel);
            label.setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            label.setName("lbl" + qualityLevel);
            panel.add(label);

            spnUsedPartPriceMultipliers[i] = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 1.00, 0.05));
            spnUsedPartPriceMultipliers[i].setToolTipText(resources.getString("lblUsedPartPriceMultiplier.toolTipText"));
            spnUsedPartPriceMultipliers[i].setName("spn" + qualityLevel);
            spnUsedPartPriceMultipliers[i].setEditor(new JSpinner.NumberEditor(spnUsedPartPriceMultipliers[i], "0.00"));
            panel.add(spnUsedPartPriceMultipliers[i]);

            label.setLabelFor(spnUsedPartPriceMultipliers[i]);
        }

        return panel;
    }
    //endregion Finances Tab

    //region Rank Systems Tab
    private JScrollPane createRankSystemsTab(final JFrame frame, final Campaign campaign) {
        rankSystemsPane = new RankSystemsPane(frame, campaign);
        return rankSystemsPane;
    }
    //endregion Rank Systems Tab

    //region Markets Tab
    private JScrollPane createMarketsTab() {
        final JPanel marketsPanel = new JPanel(new GridBagLayout());
        marketsPanel.setName("marketsPanel");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        marketsPanel.add(createPersonnelMarketPanel(), gbc);

        gbc.gridx++;
        marketsPanel.add(createContractMarketPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        marketsPanel.add(createUnitMarketPanel(), gbc);

        JScrollPane scrollMarkets = new JScrollPane(marketsPanel);
        scrollMarkets.setPreferredSize(new Dimension(500, 400));

        return scrollMarkets;
    }

    private JPanel createPersonnelMarketPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblPersonnelMarketRandomEliteRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomVeteranRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomRegularRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomGreenRemoval = new JLabel();
        final JLabel lblPersonnelMarketRandomUltraGreenRemoval = new JLabel();
        final JLabel lblPersonnelMarketDylansWeight = new JLabel();

        // Create Panel Components
        final JLabel lblPersonnelMarketType = new JLabel(resources.getString("lblPersonnelMarket.text"));
        lblPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        lblPersonnelMarketType.setName("lblPersonnelMarketType");

        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance().getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        comboPersonnelMarketType = new JComboBox<>(personnelMarketTypeModel);
        comboPersonnelMarketType.setToolTipText(resources.getString("lblPersonnelMarketType.toolTipText"));
        comboPersonnelMarketType.setName("comboPersonnelMarketType");
        comboPersonnelMarketType.addActionListener(evt -> {
            final boolean isDylan = new PersonnelMarketDylan().getModuleName().equals(comboPersonnelMarketType.getSelectedItem());
            final boolean enabled = isDylan || new PersonnelMarketRandom().getModuleName().equals(comboPersonnelMarketType.getSelectedItem());
            lblPersonnelMarketRandomEliteRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomEliteRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomVeteranRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomVeteranRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomRegularRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomRegularRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomGreenRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomGreenRemoval.setEnabled(enabled);
            lblPersonnelMarketRandomUltraGreenRemoval.setEnabled(enabled);
            spnPersonnelMarketRandomUltraGreenRemoval.setEnabled(enabled);
            lblPersonnelMarketDylansWeight.setEnabled(isDylan);
            spnPersonnelMarketDylansWeight.setEnabled(isDylan);
        });

        chkPersonnelMarketReportRefresh = new JCheckBox(resources.getString("chkPersonnelMarketReportRefresh.text"));
        chkPersonnelMarketReportRefresh.setToolTipText(resources.getString("chkPersonnelMarketReportRefresh.toolTipText"));
        chkPersonnelMarketReportRefresh.setName("chkPersonnelMarketReportRefresh");

        lblPersonnelMarketRandomEliteRemoval.setText(resources.getString("lblPersonnelMarketRandomEliteRemoval.text"));
        lblPersonnelMarketRandomEliteRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomEliteRemoval.toolTipText"));
        lblPersonnelMarketRandomEliteRemoval.setName("lblPersonnelMarketRandomEliteRemoval");

        spnPersonnelMarketRandomEliteRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomEliteRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomEliteRemoval.toolTipText"));
        spnPersonnelMarketRandomEliteRemoval.setName("spnPersonnelMarketRandomEliteRemoval");

        lblPersonnelMarketRandomVeteranRemoval.setText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.text"));
        lblPersonnelMarketRandomVeteranRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.toolTipText"));
        lblPersonnelMarketRandomVeteranRemoval.setName("lblPersonnelMarketRandomVeteranRemoval");

        spnPersonnelMarketRandomVeteranRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomVeteranRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomVeteranRemoval.toolTipText"));
        spnPersonnelMarketRandomVeteranRemoval.setName("spnPersonnelMarketRandomVeteranRemoval");

        lblPersonnelMarketRandomRegularRemoval.setText(resources.getString("lblPersonnelMarketRandomRegularRemoval.text"));
        lblPersonnelMarketRandomRegularRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomRegularRemoval.toolTipText"));
        lblPersonnelMarketRandomRegularRemoval.setName("lblPersonnelMarketRandomRegularRemoval");

        spnPersonnelMarketRandomRegularRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomRegularRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomRegularRemoval.toolTipText"));
        spnPersonnelMarketRandomRegularRemoval.setName("spnPersonnelMarketRandomRegularRemoval");

        lblPersonnelMarketRandomGreenRemoval.setText(resources.getString("lblPersonnelMarketRandomGreenRemoval.text"));
        lblPersonnelMarketRandomGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomGreenRemoval.toolTipText"));
        lblPersonnelMarketRandomGreenRemoval.setName("lblPersonnelMarketRandomGreenRemoval");

        spnPersonnelMarketRandomGreenRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomGreenRemoval.toolTipText"));
        spnPersonnelMarketRandomGreenRemoval.setName("spnPersonnelMarketRandomGreenRemoval");

        lblPersonnelMarketRandomUltraGreenRemoval.setText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.text"));
        lblPersonnelMarketRandomUltraGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.toolTipText"));
        lblPersonnelMarketRandomUltraGreenRemoval.setName("lblPersonnelMarketRandomUltraGreenRemoval");

        spnPersonnelMarketRandomUltraGreenRemoval = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        spnPersonnelMarketRandomUltraGreenRemoval.setToolTipText(resources.getString("lblPersonnelMarketRandomUltraGreenRemoval.toolTipText"));
        spnPersonnelMarketRandomUltraGreenRemoval.setName("spnPersonnelMarketRandomUltraGreenRemoval");

        lblPersonnelMarketDylansWeight.setText(resources.getString("lblPersonnelMarketDylansWeight.text"));
        lblPersonnelMarketDylansWeight.setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        lblPersonnelMarketDylansWeight.setName("lblPersonnelMarketDylansWeight");

        spnPersonnelMarketDylansWeight = new JSpinner(new SpinnerNumberModel(0.3, 0, 1, 0.1));
        spnPersonnelMarketDylansWeight.setToolTipText(resources.getString("lblPersonnelMarketDylansWeight.toolTipText"));
        spnPersonnelMarketDylansWeight.setName("spnPersonnelMarketDylansWeight");

        // Programmatically Assign Accessibility Labels
        lblPersonnelMarketType.setLabelFor(comboPersonnelMarketType);
        lblPersonnelMarketRandomEliteRemoval.setLabelFor(spnPersonnelMarketRandomEliteRemoval);
        lblPersonnelMarketRandomVeteranRemoval.setLabelFor(spnPersonnelMarketRandomVeteranRemoval);
        lblPersonnelMarketRandomRegularRemoval.setLabelFor(spnPersonnelMarketRandomRegularRemoval);
        lblPersonnelMarketRandomGreenRemoval.setLabelFor(spnPersonnelMarketRandomGreenRemoval);
        lblPersonnelMarketRandomUltraGreenRemoval.setLabelFor(spnPersonnelMarketRandomUltraGreenRemoval);
        lblPersonnelMarketDylansWeight.setLabelFor(spnPersonnelMarketDylansWeight);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelMarketPanel.title")));
        panel.setName("personnelMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType, GroupLayout.Alignment.LEADING))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomEliteRemoval)
                                .addComponent(spnPersonnelMarketRandomEliteRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomVeteranRemoval)
                                .addComponent(spnPersonnelMarketRandomVeteranRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomRegularRemoval)
                                .addComponent(spnPersonnelMarketRandomRegularRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomGreenRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketRandomUltraGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomUltraGreenRemoval, GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketType)
                                .addComponent(comboPersonnelMarketType))
                        .addComponent(chkPersonnelMarketReportRefresh)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomEliteRemoval)
                                .addComponent(spnPersonnelMarketRandomEliteRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomVeteranRemoval)
                                .addComponent(spnPersonnelMarketRandomVeteranRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomRegularRemoval)
                                .addComponent(spnPersonnelMarketRandomRegularRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomGreenRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketRandomUltraGreenRemoval)
                                .addComponent(spnPersonnelMarketRandomUltraGreenRemoval))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPersonnelMarketDylansWeight)
                                .addComponent(spnPersonnelMarketDylansWeight))
        );

        return panel;
    }

    private JPanel createUnitMarketPanel() {
        // Create Panel Components
        final JLabel lblUnitMarketMethod = new JLabel(resources.getString("lblUnitMarketMethod.text"));
        lblUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        lblUnitMarketMethod.setName("lblUnitMarketMethod");

        comboUnitMarketMethod = new JComboBox<>(UnitMarketMethod.values());
        comboUnitMarketMethod.setToolTipText(resources.getString("lblUnitMarketMethod.toolTipText"));
        comboUnitMarketMethod.setName("comboUnitMarketMethod");
        comboUnitMarketMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UnitMarketMethod) {
                    list.setToolTipText(((UnitMarketMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboUnitMarketMethod.addActionListener(evt -> {
            final boolean enabled = !((UnitMarketMethod) Objects.requireNonNull(comboUnitMarketMethod.getSelectedItem())).isNone();
            chkUnitMarketRegionalMechVariations.setEnabled(enabled);
            chkInstantUnitMarketDelivery.setEnabled(enabled);
            chkUnitMarketReportRefresh.setEnabled(enabled);
        });

        chkUnitMarketRegionalMechVariations = new JCheckBox(resources.getString("chkUnitMarketRegionalMechVariations.text"));
        chkUnitMarketRegionalMechVariations.setToolTipText(resources.getString("chkUnitMarketRegionalMechVariations.toolTipText"));
        chkUnitMarketRegionalMechVariations.setName("chkUnitMarketRegionalMechVariations");

        chkInstantUnitMarketDelivery = new JCheckBox(resources.getString("chkInstantUnitMarketDelivery.text"));
        chkInstantUnitMarketDelivery.setToolTipText(resources.getString("chkInstantUnitMarketDelivery.toolTipText"));
        chkInstantUnitMarketDelivery.setName("chkInstantUnitMarketDelivery");

        chkUnitMarketReportRefresh = new JCheckBox(resources.getString("chkUnitMarketReportRefresh.text"));
        chkUnitMarketReportRefresh.setToolTipText(resources.getString("chkUnitMarketReportRefresh.toolTipText"));
        chkUnitMarketReportRefresh.setName("chkUnitMarketReportRefresh");

        // Programmatically Assign Accessibility Labels
        lblUnitMarketMethod.setLabelFor(comboUnitMarketMethod);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitMarketPanel.title")));
        panel.setName("unitMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkUnitMarketRegionalMechVariations)
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUnitMarketMethod)
                                .addComponent(comboUnitMarketMethod))
                        .addComponent(chkUnitMarketRegionalMechVariations)
                        .addComponent(chkInstantUnitMarketDelivery)
                        .addComponent(chkUnitMarketReportRefresh)
        );

        return panel;
    }

    private JPanel createContractMarketPanel() {
        // Create Panel Components
        final JLabel lblContractMarketMethod = new JLabel(resources.getString("lblContractMarketMethod.text"));
        lblContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        lblContractMarketMethod.setName("lblContractMarketMethod");
        lblContractMarketMethod.setVisible(false); // TODO : AbstractContractMarket : Remove

        comboContractMarketMethod = new JComboBox<>(ContractMarketMethod.values());
        comboContractMarketMethod.setToolTipText(resources.getString("lblContractMarketMethod.toolTipText"));
        comboContractMarketMethod.setName("comboContractMarketMethod");
        comboContractMarketMethod.setVisible(false); // TODO : AbstractContractMarket : Remove
        /*
        comboContractMarketMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContractMarketMethod) {
                    list.setToolTipText(((ContractMarketMethod) value).getToolTipText());
                }
                return this;
            }
        });
        comboContractMarketMethod.addActionListener(evt -> {
            final boolean enabled = !((ContractMarketMethod) Objects.requireNonNull(comboContractMarketMethod.getSelectedItem())).isNone();
            chkContractMarketReportRefresh.setEnabled(enabled);
        });
         */

        chkContractMarketReportRefresh = new JCheckBox(resources.getString("chkContractMarketReportRefresh.text"));
        chkContractMarketReportRefresh.setToolTipText(resources.getString("chkContractMarketReportRefresh.toolTipText"));
        chkContractMarketReportRefresh.setName("chkContractMarketReportRefresh");

        // Programmatically Assign Accessibility Labels
        lblContractMarketMethod.setLabelFor(comboContractMarketMethod);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("contractMarketPanel.title")));
        panel.setName("contractMarketPanel");

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod, GroupLayout.Alignment.LEADING))
                        .addComponent(chkContractMarketReportRefresh)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblContractMarketMethod)
                                .addComponent(comboContractMarketMethod))
                        .addComponent(chkContractMarketReportRefresh)
        );

        return panel;
    }
    //endregion Markets Tab

    //region RATs Tab
    private JScrollPane createRATTab() {
        // Initialize Components Used in ActionListeners
        final JDisableablePanel traditionalRATPanel = new JDisableablePanel("traditionalRATPanel");

        // Initialize Parsing Variables
        final ButtonGroup group = new ButtonGroup();

        // Create the Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("ratPanel");

        // Create Panel Components
        btnUseRATGenerator = new JRadioButton(resources.getString("btnUseRATGenerator.text"));
        btnUseRATGenerator.setToolTipText(resources.getString("btnUseRATGenerator.tooltip"));
        btnUseRATGenerator.setName("btnUseRATGenerator");
        group.add(btnUseRATGenerator);
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        panel.add(btnUseRATGenerator, gbc);

        btnUseStaticRATs = new JRadioButton(resources.getString("btnUseStaticRATs.text"));
        btnUseStaticRATs.setToolTipText(resources.getString("btnUseStaticRATs.tooltip"));
        btnUseStaticRATs.setName("btnUseStaticRATs");
        btnUseStaticRATs.addItemListener(ev -> traditionalRATPanel.setEnabled(btnUseStaticRATs.isSelected()));
        group.add(btnUseStaticRATs);
        gbc.gridy++;
        panel.add(btnUseStaticRATs, gbc);

        createTraditionalRATPanel(traditionalRATPanel);
        gbc.gridy++;
        panel.add(traditionalRATPanel, gbc);

        // Disable Panel Portions by Default
        btnUseStaticRATs.setSelected(true);
        btnUseStaticRATs.doClick();

        return new JScrollPane(panel);
    }

    private void createTraditionalRATPanel(final JDisableablePanel panel) {
        // Initialize Components Used in ActionListeners
        final JList<String> chosenRATs = new JList<>();

        // Create Panel Components
        final JTextArea txtRATInstructions = new JTextArea(resources.getString("txtRATInstructions.text"));
        txtRATInstructions.setEditable(false);
        txtRATInstructions.setLineWrap(true);
        txtRATInstructions.setWrapStyleWord(true);

        final JLabel lblAvailableRATs = new JLabel(resources.getString("lblAvailableRATs.text"));

        availableRATModel = new DefaultListModel<>();
        for (final String rat : RATManager.getAllRATCollections().keySet()) {
            final List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
                final StringBuilder displayName = new StringBuilder(rat);
                if (!eras.isEmpty()) {
                    displayName.append(" (").append(eras.get(0));
                    if (eras.size() > 1) {
                        displayName.append("-").append(eras.get(eras.size() - 1));
                    }
                    displayName.append(")");
                }
                availableRATModel.addElement(displayName.toString());
            }
        }
        final JList<String> availableRATs = new JList<>(availableRATModel);
        availableRATs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JButton btnAddRAT = new MMButton("btnAddRAT", resources, "btnAddRAT.text",
                "btnAddRAT.toolTipText", evt -> {
            final int selectedIndex = availableRATs.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            chosenRATModel.addElement(availableRATs.getSelectedValue());
            availableRATModel.removeElementAt(availableRATs.getSelectedIndex());
            availableRATs.setSelectedIndex(Math.min(selectedIndex, availableRATModel.size() - 1));
        });

        final JButton btnRemoveRAT = new MMButton("btnRemoveRAT", resources, "btnRemoveRAT.text",
                "btnRemoveRAT.toolTipText", evt -> {
            final int selectedIndex = chosenRATs.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            availableRATModel.addElement(chosenRATs.getSelectedValue());
            chosenRATModel.removeElementAt(chosenRATs.getSelectedIndex());
            chosenRATs.setSelectedIndex(Math.min(selectedIndex, chosenRATModel.size() - 1));
        });

        final JButton btnMoveRATUp = new MMButton("btnMoveRATUp", resources, "btnMoveRATUp.text",
                "btnMoveRATUp.toolTipText", evt ->{
            final int selectedIndex = chosenRATs.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            final String element = chosenRATModel.getElementAt(selectedIndex);
            chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex - 1), selectedIndex);
            chosenRATModel.setElementAt(element, selectedIndex - 1);
            chosenRATs.setSelectedIndex(selectedIndex - 1);
        });

        final JButton btnMoveRATDown = new MMButton("btnMoveRATDown", resources, "btnMoveRATDown.text",
                "btnMoveRATDown.toolTipText", evt -> {
            final int selectedIndex = chosenRATs.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }
            final String element = chosenRATModel.getElementAt(selectedIndex);
            chosenRATModel.setElementAt(chosenRATModel.getElementAt(selectedIndex + 1), selectedIndex);
            chosenRATModel.setElementAt(element, selectedIndex + 1);
            chosenRATs.setSelectedIndex(selectedIndex + 1);
        });

        final JLabel lblChosenRATs = new JLabel(resources.getString("lblChosenRATs.text"));

        chosenRATModel = new DefaultListModel<>();
        chosenRATs.setModel(chosenRATModel);
        chosenRATs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chosenRATs.addListSelectionListener(evt -> {
            btnRemoveRAT.setEnabled(chosenRATs.getSelectedIndex() >= 0);
            btnMoveRATUp.setEnabled(chosenRATs.getSelectedIndex() > 0);
            btnMoveRATDown.setEnabled(chosenRATModel.size() > chosenRATs.getSelectedIndex() + 1);
        });

        chkIgnoreRATEra = new JCheckBox(resources.getString("chkIgnoreRATEra.text"));
        chkIgnoreRATEra.setToolTipText(resources.getString("chkIgnoreRATEra.toolTipText"));
        chkIgnoreRATEra.setName("chkIgnoreRATEra");

        // Add Previously Impossible Listeners
        availableRATs.addListSelectionListener(evt -> btnAddRAT.setEnabled(availableRATs.getSelectedIndex() >= 0));

        // Programmatically Assign Accessibility Labels
        lblAvailableRATs.setLabelFor(availableRATs);
        lblChosenRATs.setLabelFor(chosenRATs);

        // Layout the UI
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("traditionalRATPanel.title")));
        panel.setLayout(new BorderLayout());

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(txtRATInstructions)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(availableRATs))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(btnAddRAT)
                                        .addComponent(btnRemoveRAT)
                                        .addComponent(btnMoveRATUp)
                                        .addComponent(btnMoveRATDown))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblChosenRATs)
                                        .addComponent(chosenRATs)))
                        .addComponent(chkIgnoreRATEra)
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(txtRATInstructions)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAvailableRATs)
                                        .addComponent(lblChosenRATs))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(availableRATs)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnAddRAT)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnRemoveRAT)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATUp)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnMoveRATDown))
                                        .addComponent(chosenRATs))
                                .addComponent(chkIgnoreRATEra))
        );
    }
    //endregion RATs Tab

    //region Against the Bot Tab
    //endregion Against the Bot Tab
    //endregion Modern Initialization
    //endregion Initialization
}
