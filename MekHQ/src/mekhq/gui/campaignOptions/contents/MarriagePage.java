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
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code MarriagePage} class builds and manages the Marriage leaf page of the Campaign Options dialog. It owns the
 * widgets for general marriage configuration - manual marriage toggles, clan and prisoner marriages, mutual-ancestor
 * depth, and name-change logging - and for random marriage settings (method, age range, dice sizes), and synchronises
 * them with a shared {@link RelationshipsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RelationshipsPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code RelationshipsPages}, while this class is responsible only for constructing the Marriage
 * panel and copying marriage values to and from the model. The page is built lazily; until
 * {@link #createPanel(RelationshipsOptionsModel)} is called, {@link #readFromModel(RelationshipsOptionsModel)} and
 * {@link #writeToModel(RelationshipsOptionsModel)} are no-ops.</p>
 */
class MarriagePage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel marriageHeader;
    private JPanel pnlMarriageGeneralOptions;
    private JCheckBox chkUseManualMarriages;
    private JCheckBox chkUseClanPersonnelMarriages;
    private JCheckBox chkUsePrisonerMarriages;
    private JLabel lblCheckMutualAncestorsDepth;
    private JSpinner spnCheckMutualAncestorsDepth;
    private JCheckBox chkLogMarriageNameChanges;

    private JPanel pnlRandomMarriage;
    private JLabel lblRandomMarriageMethod;
    private MMComboBox<RandomMarriageMethod> comboRandomMarriageMethod;
    private JCheckBox chkUseRandomClanPersonnelMarriages;
    private JCheckBox chkUseRandomPrisonerMarriages;
    private JLabel lblRandomMarriageAgeRange;
    private JSpinner spnRandomMarriageAgeRange;
    private JLabel lblRandomMarriageDiceSize;
    private JSpinner spnRandomMarriageDiceSize;
    private JLabel lblRandomNewDependentMarriage;
    private JSpinner spnRandomNewDependentMarriage;

    private boolean created;

    /**
     * Builds the Marriage page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared relationships options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Marriage Page
     */
    @Nonnull JPanel createPanel(@Nullable RelationshipsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_morgrains_valkyrate.png";
        marriageHeader = new CampaignOptionsHeaderPanel("MarriagePage", imageAddress);

        // Contents
        comboRandomMarriageMethod = new MMComboBox<>("comboRandomMarriageMethod",
                RandomMarriageMethod.values());
        pnlMarriageGeneralOptions = createMarriageGeneralOptionsPanel();
        pnlRandomMarriage = createRandomMarriagePanel();
        JPanel panel = CampaignOptionsPagePanel.builder("MarriagePage", "MarriagePage", imageAddress)
                .header(marriageHeader)
                .quote("marriagePage")
                .section("lblMarriageGeneralOptionsPanel.text",
                        "lblMarriageGeneralOptionsPanel.summary",
                        pnlMarriageGeneralOptions)
                .section("lblRandomMarriages.text", "lblRandomMarriages.summary", pnlRandomMarriage)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for general marriage settings. This panel includes controls
     * like manual marriage toggles and
     * ancestor checks.
     *
     * @return a {@link JPanel} containing general marriage options.
     */
    private @Nonnull JPanel createMarriageGeneralOptionsPanel() {
        // Contents
        chkUseManualMarriages = new CampaignOptionsCheckBox("UseManualMarriages");
        chkUseManualMarriages.addMouseListener(createTipPanelUpdater("UseManualMarriages"));
        chkUseClanPersonnelMarriages = new CampaignOptionsCheckBox("UseClanPersonnelMarriages");
        chkUseClanPersonnelMarriages.addMouseListener(createTipPanelUpdater("UseClanPersonnelMarriages"));
        chkUsePrisonerMarriages = new CampaignOptionsCheckBox("UsePrisonerMarriages");
        chkUsePrisonerMarriages.addMouseListener(createTipPanelUpdater("UsePrisonerMarriages"));

        lblCheckMutualAncestorsDepth = new CampaignOptionsLabel("CheckMutualAncestorsDepth");
        lblCheckMutualAncestorsDepth.addMouseListener(createTipPanelUpdater("CheckMutualAncestorsDepth"));
        spnCheckMutualAncestorsDepth = new CampaignOptionsSpinner("CheckMutualAncestorsDepth",
                4, 0, 20, 1);
        spnCheckMutualAncestorsDepth.addMouseListener(createTipPanelUpdater("CheckMutualAncestorsDepth"));

        chkLogMarriageNameChanges = new CampaignOptionsCheckBox("LogMarriageNameChanges");
        chkLogMarriageNameChanges.addMouseListener(createTipPanelUpdater("LogMarriageNameChanges"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MarriageGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseManualMarriages,
                chkUseClanPersonnelMarriages,
                chkUsePrisonerMarriages);
        panel.addRow(lblCheckMutualAncestorsDepth, spnCheckMutualAncestorsDepth);
        panel.addCheckBox(chkLogMarriageNameChanges);

        return panel;
    }

    /**
     * Creates the panel for configuring random marriage settings. Options include
     * random clan marriages, prisoner
     * marriages, and other random marriage rules.
     *
     * @return a {@link JPanel} containing random marriage settings.
     */
    private @Nonnull JPanel createRandomMarriagePanel() {
        // Contents
        lblRandomMarriageMethod = new CampaignOptionsLabel("RandomMarriageMethod");
        lblRandomMarriageMethod.addMouseListener(createTipPanelUpdater("RandomMarriageMethod"));
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
        comboRandomMarriageMethod.addMouseListener(createTipPanelUpdater("RandomMarriageMethod"));

        chkUseRandomClanPersonnelMarriages = new CampaignOptionsCheckBox("UseRandomClanPersonnelMarriages");
        chkUseRandomClanPersonnelMarriages.addMouseListener(createTipPanelUpdater("UseRandomClanPersonnelMarriages"));
        chkUseRandomPrisonerMarriages = new CampaignOptionsCheckBox("UseRandomPrisonerMarriages");
        chkUseRandomPrisonerMarriages.addMouseListener(createTipPanelUpdater("UseRandomPrisonerMarriages"));

        lblRandomMarriageAgeRange = new CampaignOptionsLabel("RandomMarriageAgeRange");
        lblRandomMarriageAgeRange.addMouseListener(createTipPanelUpdater("RandomMarriageAgeRange"));
        spnRandomMarriageAgeRange = new CampaignOptionsSpinner("RandomMarriageAgeRange",
                10, 0, 100, 1);
        spnRandomMarriageAgeRange.addMouseListener(createTipPanelUpdater("RandomMarriageAgeRange"));

        lblRandomMarriageDiceSize = new CampaignOptionsLabel("RandomMarriageDiceSize");
        lblRandomMarriageDiceSize.addMouseListener(createTipPanelUpdater("RandomMarriageDiceSize"));
        spnRandomMarriageDiceSize = new CampaignOptionsSpinner("RandomMarriageDiceSize",
                5000, 0, 100000, 1);
        spnRandomMarriageDiceSize.addMouseListener(createTipPanelUpdater("RandomMarriageDiceSize"));

        lblRandomNewDependentMarriage = new CampaignOptionsLabel("RandomNewDependentMarriage");
        lblRandomNewDependentMarriage.addMouseListener(createTipPanelUpdater("RandomNewDependentMarriage"));
        spnRandomNewDependentMarriage = new CampaignOptionsSpinner("RandomNewDependentMarriage",
                20, 0, 100000, 1);
        spnRandomNewDependentMarriage.addMouseListener(createTipPanelUpdater("RandomNewDependentMarriage"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomMarriages",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRandomMarriageMethod, comboRandomMarriageMethod);
        panel.addCheckBoxGrid(2, chkUseRandomClanPersonnelMarriages, chkUseRandomPrisonerMarriages);
        panel.addRow(lblRandomMarriageAgeRange, spnRandomMarriageAgeRange);
        panel.addRow(lblRandomMarriageDiceSize, spnRandomMarriageDiceSize);
        panel.addRow(lblRandomNewDependentMarriage, spnRandomNewDependentMarriage);

        return panel;
    }

    /**
     * Copies marriage values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared relationships options model to read values from
     */
    void readFromModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseManualMarriages.setSelected(model.useManualMarriages);
        chkUseClanPersonnelMarriages.setSelected(model.useClanPersonnelMarriages);
        chkUsePrisonerMarriages.setSelected(model.usePrisonerMarriages);
        spnCheckMutualAncestorsDepth.setValue(model.checkMutualAncestorsDepth);
        chkLogMarriageNameChanges.setSelected(model.logMarriageNameChanges);
        comboRandomMarriageMethod.setSelectedItem(model.randomMarriageMethod);
        chkUseRandomClanPersonnelMarriages.setSelected(model.useRandomClanPersonnelMarriages);
        chkUseRandomPrisonerMarriages.setSelected(model.useRandomPrisonerMarriages);
        spnRandomMarriageAgeRange.setValue(model.randomMarriageAgeRange);
        spnRandomMarriageDiceSize.setValue(model.randomMarriageDiceSize);
        spnRandomNewDependentMarriage.setValue(model.randomNewDependentMarriage);
    }

    /**
     * Copies marriage values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared relationships options model to write values into
     */
    void writeToModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useManualMarriages = chkUseManualMarriages.isSelected();
        model.useClanPersonnelMarriages = chkUseClanPersonnelMarriages.isSelected();
        model.usePrisonerMarriages = chkUsePrisonerMarriages.isSelected();
        model.checkMutualAncestorsDepth = (int) spnCheckMutualAncestorsDepth.getValue();
        model.logMarriageNameChanges = chkLogMarriageNameChanges.isSelected();
        model.randomMarriageMethod = comboRandomMarriageMethod.getSelectedItem();
        model.useRandomClanPersonnelMarriages = chkUseRandomClanPersonnelMarriages.isSelected();
        model.useRandomPrisonerMarriages = chkUseRandomPrisonerMarriages.isSelected();
        model.randomMarriageAgeRange = (int) spnRandomMarriageAgeRange.getValue();
        model.randomMarriageDiceSize = (int) spnRandomMarriageDiceSize.getValue();
        model.randomNewDependentMarriage = (int) spnRandomNewDependentMarriage.getValue();
    }
}
