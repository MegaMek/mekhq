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
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code RepairPage} class builds and manages the Repair leaf page of the Campaign Options dialog. It owns the
 * widgets for repair configuration - era modifications, technician assignment, equipment quirks, damage margins, and
 * destruction thresholds - and synchronises them with a shared {@link RepairAndMaintenanceOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RepairAndMaintenancePages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code RepairAndMaintenancePages}, while this class is responsible only for constructing the
 * Repair panel and copying repair values to and from the model. The page is built lazily; until
 * {@link #createPanel(RepairAndMaintenanceOptionsModel)} is called,
 * {@link #readFromModel(RepairAndMaintenanceOptionsModel)} and {@link #writeToModel(RepairAndMaintenanceOptionsModel)}
 * are no-ops.</p>
 */
class RepairPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private JCheckBox chkTechsUseAdministration;
    private JCheckBox chkUsefulAsTechs;
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JLabel lblDamageMargin;
    private JSpinner spnDamageMargin;
    private JLabel lblDestroyPartTarget;
    private JSpinner spnDestroyPartTarget;

    private boolean created;

    /**
     * Creates the panel for the Repair Page.
     * <p>
     * This page provides configurable options for managing repair rules, handling
     * quirks, setting margins for equipment
     * survival, and incorporating era modifications.
     * </p>
     *
     * @param model the shared repair and maintenance options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Repair Page.
     */
    @Nonnull JPanel createPanel(@Nullable RepairAndMaintenanceOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_clan_burrock.png";
        CampaignOptionsHeaderPanel repairHeader = new CampaignOptionsHeaderPanel("RepairPage", imageAddress);

        chkTechsUseAdministration = new CampaignOptionsCheckBox("TechsUseAdministration",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkTechsUseAdministration.addMouseListener(createTipPanelUpdater("TechsUseAdministration"));

        chkUsefulAsTechs = new CampaignOptionsCheckBox("UsefulAsTechs",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUsefulAsTechs.addMouseListener(createTipPanelUpdater("UsefulAsTechs"));

        useEraModsCheckBox = new CampaignOptionsCheckBox("UseEraModsCheckBox");
        useEraModsCheckBox.addMouseListener(createTipPanelUpdater("UseEraModsCheckBox"));

        assignedTechFirstCheckBox = new CampaignOptionsCheckBox("AssignedTechFirstCheckBox");
        assignedTechFirstCheckBox.addMouseListener(createTipPanelUpdater("AssignedTechFirstCheckBox"));
        resetToFirstTechCheckBox = new CampaignOptionsCheckBox("ResetToFirstTechCheckBox");
        resetToFirstTechCheckBox.addMouseListener(createTipPanelUpdater("ResetToFirstTechCheckBox"));

        useQuirksBox = new CampaignOptionsCheckBox("UseQuirksBox");
        useQuirksBox.addMouseListener(createTipPanelUpdater("UseQuirksBox"));

        useAeroSystemHitsBox = new CampaignOptionsCheckBox("UseAeroSystemHitsBox");
        useAeroSystemHitsBox.addMouseListener(createTipPanelUpdater("UseAeroSystemHitsBox"));

        useDamageMargin = new CampaignOptionsCheckBox("UseDamageMargin");
        useDamageMargin.addMouseListener(createTipPanelUpdater("UseDamageMargin"));

        lblDamageMargin = new CampaignOptionsLabel("DamageMargin");
        lblDamageMargin.addMouseListener(createTipPanelUpdater("DamageMargin"));
        spnDamageMargin = new CampaignOptionsSpinner("DamageMargin",
                1, 1, 20, 1);
        spnDamageMargin.addMouseListener(createTipPanelUpdater("DamageMargin"));

        lblDestroyPartTarget = new CampaignOptionsLabel("DestroyPartTarget");
        lblDestroyPartTarget.addMouseListener(createTipPanelUpdater("DestroyPartTarget"));
        spnDestroyPartTarget = new CampaignOptionsSpinner("DestroyPartTarget",
                2, 2, 13, 1);
        spnDestroyPartTarget.addMouseListener(createTipPanelUpdater("DestroyPartTarget"));

        JPanel repairOptionsPanel = createRepairOptionsPanel();
        JPanel componentDamagePanel = createComponentDamagePanel();

        created = true;
        readFromModel(model);

        return CampaignOptionsPagePanel.builder("RepairPage", "RepairPage", imageAddress)
                .header(repairHeader)
                .quote("repairPage")
                .section("lblRepairPage.text",
                        "lblRepairPage.summary",
                        repairOptionsPanel)
                .section("lblRepairPageRight.text",
                        "lblRepairPageRight.summary",
                        componentDamagePanel,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .build();
    }

    private @Nonnull JPanel createRepairOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RepairOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkTechsUseAdministration,
                chkUsefulAsTechs,
                useEraModsCheckBox,
                assignedTechFirstCheckBox,
                resetToFirstTechCheckBox,
                useQuirksBox);

        return panel;
    }

    private @Nonnull JPanel createComponentDamagePanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ComponentDamagePanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                useAeroSystemHitsBox,
                useDamageMargin);
        panel.addRow(lblDamageMargin, spnDamageMargin);
        panel.addRow(lblDestroyPartTarget, spnDestroyPartTarget);

        return panel;
    }

    /**
     * Copies repair values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared repair and maintenance options model to read values from
     */
    void readFromModel(@Nullable RepairAndMaintenanceOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkTechsUseAdministration.setSelected(model.techsUseAdministration);
        chkUsefulAsTechs.setSelected(model.useUsefulAsTechs);
        useEraModsCheckBox.setSelected(model.useEraMods);
        assignedTechFirstCheckBox.setSelected(model.assignedTechFirst);
        resetToFirstTechCheckBox.setSelected(model.resetToFirstTech);
        useQuirksBox.setSelected(model.useQuirks);
        useAeroSystemHitsBox.setSelected(model.useAeroSystemHits);
        useDamageMargin.setSelected(model.destroyByMargin);
        spnDamageMargin.setValue(model.destroyMargin);
        spnDestroyPartTarget.setValue(model.destroyPartTarget);
    }

    /**
     * Copies repair values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared repair and maintenance options model to write values into
     */
    void writeToModel(@Nullable RepairAndMaintenanceOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.techsUseAdministration = chkTechsUseAdministration.isSelected();
        model.useUsefulAsTechs = chkUsefulAsTechs.isSelected();
        model.useEraMods = useEraModsCheckBox.isSelected();
        model.assignedTechFirst = assignedTechFirstCheckBox.isSelected();
        model.resetToFirstTech = resetToFirstTechCheckBox.isSelected();
        model.useQuirks = useQuirksBox.isSelected();
        model.useAeroSystemHits = useAeroSystemHitsBox.isSelected();
        model.destroyByMargin = useDamageMargin.isSelected();
        model.destroyMargin = (int) spnDamageMargin.getValue();
        model.destroyPartTarget = (int) spnDestroyPartTarget.getValue();
    }
}
