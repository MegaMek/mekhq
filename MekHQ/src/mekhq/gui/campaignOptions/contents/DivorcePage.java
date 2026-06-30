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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;

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
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code DivorcePage} class builds and manages the Divorce leaf page of the Campaign Options dialog. It owns the
 * widgets for general divorce configuration - manual divorce toggles, clan and prisoner divorces - and for random
 * divorce settings (method, same-sex and opposite-sex toggles, dice size), and synchronises them with a shared
 * {@link RelationshipsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RelationshipsPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code RelationshipsPages}, while this class is responsible only for constructing the Divorce
 * panel and copying divorce values to and from the model. The page is built lazily; until
 * {@link #createPanel(RelationshipsOptionsModel)} is called, {@link #readFromModel(RelationshipsOptionsModel)} and
 * {@link #writeToModel(RelationshipsOptionsModel)} are no-ops.</p>
 */
class DivorcePage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel divorceHeader;
    private JCheckBox chkUseManualDivorce;
    private JCheckBox chkUseClanPersonnelDivorce;
    private JCheckBox chkUsePrisonerDivorce;

    private JPanel pnlRandomDivorce;
    private JLabel lblRandomDivorceMethod;
    private MMComboBox<RandomDivorceMethod> comboRandomDivorceMethod;
    private JCheckBox chkUseRandomOppositeSexDivorce;
    private JCheckBox chkUseRandomSameSexDivorce;
    private JCheckBox chkUseRandomClanPersonnelDivorce;
    private JCheckBox chkUseRandomPrisonerDivorce;
    private JLabel lblRandomDivorceDiceSize;
    private JSpinner spnRandomDivorceDiceSize;

    private boolean created;

    /**
     * Builds the Divorce page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared relationships options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Divorce Page
     */
    @Nonnull JPanel createPanel(@Nullable RelationshipsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_escorpion_imperio.png";
        divorceHeader = new CampaignOptionsHeaderPanel("DivorcePage", imageAddress);

        // Contents
        comboRandomDivorceMethod = new MMComboBox<>("comboRandomDivorceMethod", RandomDivorceMethod.values());
        JPanel divorceGeneralOptionsPanel = createDivorceGeneralOptionsPanel();
        pnlRandomDivorce = createRandomDivorcePanel();
        JPanel panel = CampaignOptionsPagePanel.builder("DivorcePage", "DivorcePage", imageAddress)
                .header(divorceHeader)
                .quote("divorcePage")
                .section("lblDivorceGeneralOptionsPanel.text",
                        "lblDivorceGeneralOptionsPanel.summary",
                        divorceGeneralOptionsPanel)
                .section("lblRandomDivorcePanel.text", "lblRandomDivorcePanel.summary", pnlRandomDivorce)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createDivorceGeneralOptionsPanel() {
        // Contents
        chkUseManualDivorce = new CampaignOptionsCheckBox("UseManualDivorce");
        chkUseManualDivorce.addMouseListener(createTipPanelUpdater("UseManualDivorce"));
        chkUseClanPersonnelDivorce = new CampaignOptionsCheckBox("UseClanPersonnelDivorce");
        chkUseClanPersonnelDivorce
                .addMouseListener(createTipPanelUpdater("UseClanPersonnelDivorce"));
        chkUsePrisonerDivorce = new CampaignOptionsCheckBox("UsePrisonerDivorce");
        chkUsePrisonerDivorce.addMouseListener(createTipPanelUpdater("UsePrisonerDivorce"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DivorceGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseManualDivorce,
                chkUseClanPersonnelDivorce,
                chkUsePrisonerDivorce);

        return panel;
    }

    /**
     * Creates the panel for configuring random divorce settings. Options include
     * toggles for random same-sex and
     * opposite-sex divorces.
     *
     * @return a {@link JPanel} containing random divorce settings.
     */
    private @Nonnull JPanel createRandomDivorcePanel() {
        // Contents
        lblRandomDivorceMethod = new CampaignOptionsLabel("RandomDivorceMethod");
        lblRandomDivorceMethod.addMouseListener(createTipPanelUpdater("RandomDivorceMethod"));
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
        comboRandomDivorceMethod.addMouseListener(createTipPanelUpdater("RandomDivorceMethod"));

        chkUseRandomOppositeSexDivorce = new CampaignOptionsCheckBox("UseRandomOppositeSexDivorce");
        chkUseRandomOppositeSexDivorce.addMouseListener(createTipPanelUpdater("UseRandomOppositeSexDivorce"));
        chkUseRandomSameSexDivorce = new CampaignOptionsCheckBox("UseRandomSameSexDivorce");
        chkUseRandomSameSexDivorce
                .addMouseListener(createTipPanelUpdater("UseRandomSameSexDivorce"));
        chkUseRandomClanPersonnelDivorce = new CampaignOptionsCheckBox("UseRandomClanPersonnelDivorce");
        chkUseRandomClanPersonnelDivorce.addMouseListener(createTipPanelUpdater("UseRandomClanPersonnelDivorce"));
        chkUseRandomPrisonerDivorce = new CampaignOptionsCheckBox("UseRandomPrisonerDivorce");
        chkUseRandomPrisonerDivorce
                .addMouseListener(createTipPanelUpdater("UseRandomPrisonerDivorce"));

        lblRandomDivorceDiceSize = new CampaignOptionsLabel("RandomDivorceDiceSize");
        lblRandomDivorceDiceSize.addMouseListener(createTipPanelUpdater("RandomDivorceDiceSize"));
        spnRandomDivorceDiceSize = new CampaignOptionsSpinner("RandomDivorceDiceSize",
                900, 0, 100000, 1);
        spnRandomDivorceDiceSize.addMouseListener(createTipPanelUpdater("RandomDivorceDiceSize"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomDivorcePanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRandomDivorceMethod, comboRandomDivorceMethod);
        panel.addCheckBoxGrid(2,
                chkUseRandomOppositeSexDivorce,
                chkUseRandomSameSexDivorce,
                chkUseRandomClanPersonnelDivorce,
                chkUseRandomPrisonerDivorce);
        panel.addRow(lblRandomDivorceDiceSize, spnRandomDivorceDiceSize);

        return panel;
    }

    /**
     * Copies divorce values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared relationships options model to read values from
     */
    void readFromModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseManualDivorce.setSelected(model.useManualDivorce);
        chkUseClanPersonnelDivorce.setSelected(model.useClanPersonnelDivorce);
        chkUsePrisonerDivorce.setSelected(model.usePrisonerDivorce);
        comboRandomDivorceMethod.setSelectedItem(model.randomDivorceMethod);
        chkUseRandomOppositeSexDivorce.setSelected(model.useRandomOppositeSexDivorce);
        chkUseRandomSameSexDivorce.setSelected(model.useRandomSameSexDivorce);
        chkUseRandomClanPersonnelDivorce.setSelected(model.useRandomClanPersonnelDivorce);
        chkUseRandomPrisonerDivorce.setSelected(model.useRandomPrisonerDivorce);
        spnRandomDivorceDiceSize.setValue(model.randomDivorceDiceSize);
    }

    /**
     * Copies divorce values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared relationships options model to write values into
     */
    void writeToModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useManualDivorce = chkUseManualDivorce.isSelected();
        model.useClanPersonnelDivorce = chkUseClanPersonnelDivorce.isSelected();
        model.usePrisonerDivorce = chkUsePrisonerDivorce.isSelected();
        model.randomDivorceMethod = comboRandomDivorceMethod.getSelectedItem();
        model.useRandomOppositeSexDivorce = chkUseRandomOppositeSexDivorce.isSelected();
        model.useRandomSameSexDivorce = chkUseRandomSameSexDivorce.isSelected();
        model.useRandomClanPersonnelDivorce = chkUseRandomClanPersonnelDivorce.isSelected();
        model.useRandomPrisonerDivorce = chkUseRandomPrisonerDivorce.isSelected();
        model.randomDivorceDiceSize = (int) spnRandomDivorceDiceSize.getValue();
    }
}
