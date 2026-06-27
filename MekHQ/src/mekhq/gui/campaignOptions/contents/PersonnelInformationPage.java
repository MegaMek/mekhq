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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;

/**
 * The {@code PersonnelInformationPage} class builds and manages the Personnel Information leaf page of the Campaign
 * Options dialog. It owns the widgets for displayed personnel information (time in service/rank and their display
 * formats, earnings tracking, origin faction) as well as the personnel-log toggles (transfers, skill/ability/edge log
 * entries, and the various record displays), and synchronises them with a shared {@link PersonnelOptionsModel}.
 *
 * <p>This view is a sub-component of {@link PersonnelPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code PersonnelPages}, while this class is responsible only for constructing the Personnel Information
 * panel and copying its values to and from the model. The page is built lazily; until
 * {@link #createPanel(PersonnelOptionsModel)} is called, {@link #readFromModel(PersonnelOptionsModel)} and
 * {@link #writeToModel(PersonnelOptionsModel)} are no-ops.</p>
 */
class PersonnelInformationPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel personnelInformationHeader;
    private JCheckBox chkUseTimeInService;
    private JLabel lblTimeInServiceDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private JLabel lblTimeInRankDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkTrackTotalXPEarnings;
    private JCheckBox chkShowOriginFaction;

    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseExtendedTOEForceName;
    private JCheckBox chkPersonnelLogSkillGain;
    private JCheckBox chkPersonnelLogAbilityGain;
    private JCheckBox chkPersonnelLogEdgeGain;
    private JCheckBox chkDisplayPersonnelLog;
    private JCheckBox chkDisplayScenarioLog;
    private JCheckBox chkDisplayKillRecord;
    private JCheckBox chkDisplayMedicalRecord;
    private JCheckBox chkDisplayPatientRecord;
    private JCheckBox chkDisplayAssignmentRecord;
    private JCheckBox chkDisplayPerformanceRecord;

    private boolean created;

    /**
     * Builds the Personnel Information page, populates its controls from the supplied model, and returns the assembled
     * panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Personnel Information Page
     */
    @Nonnull JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_rasalhague_dominion.png";
        personnelInformationHeader = new CampaignOptionsHeaderPanel("PersonnelInformation", imageAddress);

        // Contents
        comboTimeInServiceDisplayFormat = new MMComboBox<>("comboTimeInServiceDisplayFormat",
                TimeInDisplayFormat.values());
        comboTimeInRankDisplayFormat = new MMComboBox<>("comboTimeInRankDisplayFormat",
                TimeInDisplayFormat.values());

        chkUseTimeInService = new CampaignOptionsCheckBox("UseTimeInService");
        chkUseTimeInService.addMouseListener(createTipPanelUpdater("UseTimeInService"));
        lblTimeInServiceDisplayFormat = new CampaignOptionsLabel("TimeInServiceDisplayFormat");
        lblTimeInServiceDisplayFormat.addMouseListener(createTipPanelUpdater("TimeInServiceDisplayFormat"));
        chkUseTimeInRank = new CampaignOptionsCheckBox("UseTimeInRank");
        chkUseTimeInRank.addMouseListener(createTipPanelUpdater("UseTimeInRank"));
        lblTimeInRankDisplayFormat = new CampaignOptionsLabel("TimeInRankDisplayFormat");
        lblTimeInRankDisplayFormat.addMouseListener(createTipPanelUpdater("TimeInRankDisplayFormat"));
        chkTrackTotalEarnings = new CampaignOptionsCheckBox("TrackTotalEarnings");
        chkTrackTotalEarnings
                .addMouseListener(createTipPanelUpdater("TrackTotalEarnings"));
        chkTrackTotalXPEarnings = new CampaignOptionsCheckBox("TrackTotalXPEarnings");
        chkTrackTotalXPEarnings.addMouseListener(createTipPanelUpdater("TrackTotalXPEarnings"));
        chkShowOriginFaction = new CampaignOptionsCheckBox("ShowOriginFaction");
        chkShowOriginFaction
                .addMouseListener(createTipPanelUpdater("ShowOriginFaction"));

        JPanel pnlPersonnelLogs = createPersonnelLogsPanel();

        // Layout the Panel
        final CampaignOptionsFormPanel personnelInformationPanel = new CampaignOptionsFormPanel(
                "PersonnelInformation",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        personnelInformationPanel.addCheckBoxGrid(2,
                chkUseTimeInService,
                chkUseTimeInRank);
        personnelInformationPanel.addRow(lblTimeInServiceDisplayFormat, comboTimeInServiceDisplayFormat);
        personnelInformationPanel.addRow(lblTimeInRankDisplayFormat, comboTimeInRankDisplayFormat);
        personnelInformationPanel.addCheckBoxGrid(2,
                chkTrackTotalEarnings,
                chkTrackTotalXPEarnings,
                chkShowOriginFaction);
        JPanel panel = CampaignOptionsPagePanel.builder("PersonnelInformation", "PersonnelInformation", imageAddress)
                .header(personnelInformationHeader)
                .quote("personnelInformationPage")
                .section("lblPersonnelInformation.text",
                        "lblPersonnelInformation.summary",
                        personnelInformationPanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .section("lblPersonnelLogsPanel.text", "lblPersonnelLogsPanel.summary", pnlPersonnelLogs)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates a sub-panel for managing personnel log settings within the Personnel Information Page.
     *
     * @return a {@link JPanel} containing log settings for personnel activities
     */
    private @Nonnull JPanel createPersonnelLogsPanel() {
        // Contents
        chkUseTransfers = new CampaignOptionsCheckBox("UseTransfers",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.RECOMMENDED));
        chkUseTransfers.addMouseListener(createTipPanelUpdater("UseTransfers"));
        chkUseExtendedTOEForceName = new CampaignOptionsCheckBox("UseExtendedTOEForceName");
        chkUseExtendedTOEForceName.addMouseListener(createTipPanelUpdater("UseExtendedTOEForceName"));
        chkPersonnelLogSkillGain = new CampaignOptionsCheckBox("PersonnelLogSkillGain");
        chkPersonnelLogSkillGain.addMouseListener(createTipPanelUpdater("PersonnelLogSkillGain"));
        chkPersonnelLogAbilityGain = new CampaignOptionsCheckBox("PersonnelLogAbilityGain");
        chkPersonnelLogAbilityGain.addMouseListener(createTipPanelUpdater("PersonnelLogAbilityGain"));
        chkPersonnelLogEdgeGain = new CampaignOptionsCheckBox("PersonnelLogEdgeGain");
        chkPersonnelLogEdgeGain.addMouseListener(createTipPanelUpdater("PersonnelLogEdgeGain"));
        chkDisplayPersonnelLog = new CampaignOptionsCheckBox("DisplayPersonnelLog");
        chkDisplayPersonnelLog.addMouseListener(createTipPanelUpdater("DisplayPersonnelLog"));
        chkDisplayScenarioLog = new CampaignOptionsCheckBox("DisplayScenarioLog");
        chkDisplayScenarioLog
                .addMouseListener(createTipPanelUpdater("DisplayScenarioLog"));
        chkDisplayKillRecord = new CampaignOptionsCheckBox("DisplayKillRecord");
        chkDisplayKillRecord
                .addMouseListener(createTipPanelUpdater("DisplayKillRecord"));
        chkDisplayMedicalRecord = new CampaignOptionsCheckBox("DisplayMedicalRecord");
        chkDisplayMedicalRecord.addMouseListener(createTipPanelUpdater("DisplayMedicalRecord"));
        chkDisplayPatientRecord = new CampaignOptionsCheckBox("DisplayPatientRecord",
                getMetadata(MILESTONE_BEFORE_METADATA));
        chkDisplayPatientRecord.addMouseListener(createTipPanelUpdater("DisplayPatientRecord"));
        chkDisplayAssignmentRecord = new CampaignOptionsCheckBox("DisplayAssignmentRecord");
        chkDisplayAssignmentRecord.addMouseListener(createTipPanelUpdater("DisplayAssignmentRecord"));
        chkDisplayPerformanceRecord = new CampaignOptionsCheckBox("DisplayPerformanceRecord");
        chkDisplayPerformanceRecord.addMouseListener(createTipPanelUpdater("DisplayPerformanceRecord"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelLogsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseTransfers,
                chkUseExtendedTOEForceName,
                chkPersonnelLogSkillGain,
                chkPersonnelLogAbilityGain,
                chkPersonnelLogEdgeGain,
                chkDisplayPersonnelLog,
                chkDisplayScenarioLog,
                chkDisplayKillRecord,
                chkDisplayMedicalRecord,
                chkDisplayPatientRecord,
                chkDisplayAssignmentRecord,
                chkDisplayPerformanceRecord);

        return panel;
    }

    /**
     * Copies personnel information and log values from the shared model into this page's controls. This is a no-op
     * until the page has been built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseTransfers.setSelected(model.useTransfers);
        chkUseExtendedTOEForceName.setSelected(model.useExtendedTOEForceName);
        chkPersonnelLogSkillGain.setSelected(model.personnelLogSkillGain);
        chkPersonnelLogAbilityGain.setSelected(model.personnelLogAbilityGain);
        chkPersonnelLogEdgeGain.setSelected(model.personnelLogEdgeGain);
        chkDisplayPersonnelLog.setSelected(model.displayPersonnelLog);
        chkDisplayScenarioLog.setSelected(model.displayScenarioLog);
        chkDisplayKillRecord.setSelected(model.displayKillRecord);
        chkDisplayMedicalRecord.setSelected(model.displayMedicalRecord);
        chkDisplayPatientRecord.setSelected(model.displayPatientRecord);
        chkDisplayAssignmentRecord.setSelected(model.displayAssignmentRecord);
        chkDisplayPerformanceRecord.setSelected(model.displayPerformanceRecord);
        chkUseTimeInService.setSelected(model.useTimeInService);
        comboTimeInServiceDisplayFormat.setSelectedItem(model.timeInServiceDisplayFormat);
        chkUseTimeInRank.setSelected(model.useTimeInRank);
        comboTimeInRankDisplayFormat.setSelectedItem(model.timeInRankDisplayFormat);
        chkTrackTotalEarnings.setSelected(model.trackTotalEarnings);
        chkTrackTotalXPEarnings.setSelected(model.trackTotalXPEarnings);
        chkShowOriginFaction.setSelected(model.showOriginFaction);
    }

    /**
     * Copies personnel information and log values from this page's controls into the shared model. This is a no-op
     * until the page has been built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useTransfers = chkUseTransfers.isSelected();
        model.useExtendedTOEForceName = chkUseExtendedTOEForceName.isSelected();
        model.personnelLogSkillGain = chkPersonnelLogSkillGain.isSelected();
        model.personnelLogAbilityGain = chkPersonnelLogAbilityGain.isSelected();
        model.personnelLogEdgeGain = chkPersonnelLogEdgeGain.isSelected();
        model.displayPersonnelLog = chkDisplayPersonnelLog.isSelected();
        model.displayScenarioLog = chkDisplayScenarioLog.isSelected();
        model.displayKillRecord = chkDisplayKillRecord.isSelected();
        model.displayMedicalRecord = chkDisplayMedicalRecord.isSelected();
        model.displayPatientRecord = chkDisplayPatientRecord.isSelected();
        model.displayAssignmentRecord = chkDisplayAssignmentRecord.isSelected();
        model.displayPerformanceRecord = chkDisplayPerformanceRecord.isSelected();
        model.useTimeInService = chkUseTimeInService.isSelected();
        model.timeInServiceDisplayFormat = comboTimeInServiceDisplayFormat.getSelectedItem();
        model.useTimeInRank = chkUseTimeInRank.isSelected();
        model.timeInRankDisplayFormat = comboTimeInRankDisplayFormat.getSelectedItem();
        model.trackTotalEarnings = chkTrackTotalEarnings.isSelected();
        model.trackTotalXPEarnings = chkTrackTotalXPEarnings.isSelected();
        model.showOriginFaction = chkShowOriginFaction.isSelected();
    }
}
