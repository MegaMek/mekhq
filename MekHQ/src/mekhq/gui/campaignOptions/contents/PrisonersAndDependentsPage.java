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

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code PrisonersAndDependentsPage} class builds and manages the Prisoners and Dependents leaf page of the
 * Campaign Options dialog. It owns the widgets for prisoner handling (capture style, escape artist, capacity reset) and
 * dependent management (random addition/removal, profession die sizes), and synchronises them with a shared
 * {@link PersonnelOptionsModel}.
 *
 * <p>This view is a sub-component of {@link PersonnelPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code PersonnelPages}, while this class is responsible only for constructing the Prisoners and
 * Dependents panel and copying its values to and from the model. The page is built lazily; until
 * {@link #createPanel(PersonnelOptionsModel)} is called, {@link #readFromModel(PersonnelOptionsModel)} and
 * {@link #writeToModel(PersonnelOptionsModel)} are no-ops.</p>
 */
class PrisonersAndDependentsPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel prisonersAndDependentsHeader;
    private JLabel lblPrisonerCaptureStyle;
    private MMComboBox<PrisonerCaptureStyle> comboPrisonerCaptureStyle;
    private JCheckBox chkResetTemporaryPrisonerCapacity;
    private JCheckBox chkUseFunctionalEscapeArtist;
    private JCheckBox chkUseRandomDependentAddition;
    private JCheckBox chkUseRandomDependentRemoval;
    private JLabel lblDependentProfessionDieSize;
    private JSpinner spnDependentProfessionDieSize;
    private JLabel lblCivilianProfessionDieSize;
    private JSpinner spnCivilianProfessionDieSize;

    private boolean created;

    /**
     * Builds the Prisoners and Dependents page, populates its controls from the supplied model, and returns the
     * assembled panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Prisoners and Dependents Page
     */
    @Nonnull JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_illyrian_palatinate.png";
        prisonersAndDependentsHeader = new CampaignOptionsHeaderPanel("PrisonersAndDependentsPage", imageAddress);

        // Contents
        comboPrisonerCaptureStyle = new MMComboBox<>("comboPrisonerCaptureStyle", PrisonerCaptureStyle.values());
        JPanel prisonerPanel = createPrisonersPanel();
        JPanel dependentsPanel = createDependentsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("PrisonersAndDependentsPage",
                        "PrisonersAndDependentsPage",
                        imageAddress)
                .header(prisonersAndDependentsHeader)
                .quote("prisonersAndDependentsPage")
                .section("lblPrisonersPanel.text",
                        "lblPrisonersPanel.summary",
                        prisonerPanel,
                        getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED))
                .section("lblDependentsPanel.text",
                        "lblDependentsPanel.summary",
                        dependentsPanel,
                        getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM, CampaignOptionFlag.DOCUMENTED))
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for configuring prisoner settings in the Prisoners and Dependents Page.
     *
     * @return a {@link JPanel} containing prisoner-related options such as capture style and status
     */
    private @Nonnull JPanel createPrisonersPanel() {
        // Contents
        lblPrisonerCaptureStyle = new CampaignOptionsLabel("PrisonerCaptureStyle");
        lblPrisonerCaptureStyle.addMouseListener(createTipPanelUpdater("PrisonerCaptureStyle"));
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(wordWrap(((PrisonerCaptureStyle) value).getTooltip()));
                }
                return this;
            }
        });
        comboPrisonerCaptureStyle.addMouseListener(createTipPanelUpdater("PrisonerCaptureStyle"));

        chkResetTemporaryPrisonerCapacity = new CampaignOptionsCheckBox("ResetTemporaryPrisonerCapacity");
        chkResetTemporaryPrisonerCapacity.addMouseListener(createTipPanelUpdater("ResetTemporaryPrisonerCapacity"));

        chkUseFunctionalEscapeArtist = new CampaignOptionsCheckBox("UseFunctionalEscapeArtist",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseFunctionalEscapeArtist.addMouseListener(createTipPanelUpdater("UseFunctionalEscapeArtist"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PrisonersPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPrisonerCaptureStyle, comboPrisonerCaptureStyle);
        panel.addCheckBoxGrid(2,
                chkUseFunctionalEscapeArtist,
                chkResetTemporaryPrisonerCapacity);

        return panel;
    }

    /**
     * Creates the panel for configuring dependent settings in the Prisoners and Dependents Page.
     *
     * @return a {@link JPanel} containing dependent management options
     */
    private @Nonnull JPanel createDependentsPanel() {
        // Contents
        chkUseRandomDependentAddition = new CampaignOptionsCheckBox("UseRandomDependentAddition");
        chkUseRandomDependentAddition.addMouseListener(createTipPanelUpdater("UseRandomDependentAddition"));

        chkUseRandomDependentRemoval = new CampaignOptionsCheckBox("UseRandomDependentRemoval");
        chkUseRandomDependentRemoval.addMouseListener(createTipPanelUpdater("UseRandomDependentRemoval"));

        lblDependentProfessionDieSize = new CampaignOptionsLabel("DependentProfessionDieSize",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblDependentProfessionDieSize.addMouseListener(createTipPanelUpdater("DependentProfessionDieSize"));
        spnDependentProfessionDieSize = new CampaignOptionsSpinner("DependentProfessionDieSize",
                4, 0, 100, 1);
        spnDependentProfessionDieSize.addMouseListener(createTipPanelUpdater("DependentProfessionDieSize"));

        lblCivilianProfessionDieSize = new CampaignOptionsLabel("CivilianProfessionDieSize",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblCivilianProfessionDieSize.addMouseListener(createTipPanelUpdater("CivilianProfessionDieSize"));
        spnCivilianProfessionDieSize = new CampaignOptionsSpinner("CivilianProfessionDieSize",
                2, 0, 100, 1);
        spnCivilianProfessionDieSize.addMouseListener(createTipPanelUpdater("CivilianProfessionDieSize"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DependentsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseRandomDependentAddition,
                chkUseRandomDependentRemoval);
        panel.addRow(lblDependentProfessionDieSize, spnDependentProfessionDieSize);
        panel.addRow(lblCivilianProfessionDieSize, spnCivilianProfessionDieSize);

        return panel;
    }

    /**
     * Copies prisoner and dependent values from the shared model into this page's controls. This is a no-op until the
     * page has been built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboPrisonerCaptureStyle.setSelectedItem(model.prisonerCaptureStyle);
        chkUseFunctionalEscapeArtist.setSelected(model.useFunctionalEscapeArtist);
        chkResetTemporaryPrisonerCapacity.setSelected(model.resetTemporaryPrisonerCapacity);
        chkUseRandomDependentAddition.setSelected(model.useRandomDependentAddition);
        chkUseRandomDependentRemoval.setSelected(model.useRandomDependentRemoval);
        spnDependentProfessionDieSize.setValue(model.dependentProfessionDieSize);
        spnCivilianProfessionDieSize.setValue(model.civilianProfessionDieSize);
    }

    /**
     * Copies prisoner and dependent values from this page's controls into the shared model. This is a no-op until the
     * page has been built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.prisonerCaptureStyle = comboPrisonerCaptureStyle.getSelectedItem();
        model.useFunctionalEscapeArtist = chkUseFunctionalEscapeArtist.isSelected();
        model.resetTemporaryPrisonerCapacity = chkResetTemporaryPrisonerCapacity.isSelected();
        model.useRandomDependentAddition = chkUseRandomDependentAddition.isSelected();
        model.useRandomDependentRemoval = chkUseRandomDependentRemoval.isSelected();
        model.dependentProfessionDieSize = (int) spnDependentProfessionDieSize.getValue();
        model.civilianProfessionDieSize = (int) spnCivilianProfessionDieSize.getValue();
    }
}
