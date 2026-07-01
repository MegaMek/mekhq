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
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code ProcreationPage} class builds and manages the Procreation leaf page of the Campaign Options dialog. It owns
 * the widgets for general procreation configuration - manual procreation toggles, pregnancy occurrences, baby surname
 * style, founder-tag assignment, and logging - and for random procreation and random sexuality settings (methods, dice
 * sizes), and synchronises them with a shared {@link RelationshipsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link RelationshipsPages}: the model snapshot and the overall load/apply
 * lifecycle still live on {@code RelationshipsPages}, while this class is responsible only for constructing the
 * Procreation panel and copying procreation values to and from the model. The page is built lazily; until
 * {@link #createPanel(RelationshipsOptionsModel)} is called, {@link #readFromModel(RelationshipsOptionsModel)} and
 * {@link #writeToModel(RelationshipsOptionsModel)} are no-ops.</p>
 */
class ProcreationPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private JCheckBox chkUseManualProcreation;
    private JCheckBox chkUseClanPersonnelProcreation;
    private JCheckBox chkUsePrisonerProcreation;
    private JLabel lblMultiplePregnancyOccurrences;
    private JSpinner spnMultiplePregnancyOccurrences;
    private JLabel lblBabySurnameStyle;
    private MMComboBox<BabySurnameStyle> comboBabySurnameStyle;
    private JCheckBox chkAssignNonPrisonerBabiesFounderTag;
    private JCheckBox chkAssignChildrenOfFoundersFounderTag;
    private JCheckBox chkDetermineFatherAtBirth;
    private JCheckBox chkDisplayTrueDueDate;
    private JLabel lblNoInterestInChildrenDiceSize;
    private JSpinner spnNoInterestInChildrenDiceSize;
    private JCheckBox chkUseMaternityLeave;
    private JCheckBox chkLogProcreation;

    private CampaignOptionsHeaderPanel procreationHeader;
    private JPanel pnlProcreationGeneralOptionsPanel;
    private JPanel pnlRandomProcreationPanel;
    private JLabel lblRandomProcreationMethod;
    private MMComboBox<RandomProcreationMethod> comboRandomProcreationMethod;
    private JCheckBox chkUseRelationshiplessRandomProcreation;
    private JCheckBox chkUseRandomClanPersonnelProcreation;
    private JCheckBox chkUseRandomPrisonerProcreation;
    private JLabel lblRandomProcreationRelationshipDiceSize;
    private JSpinner spnRandomProcreationRelationshipDiceSize;
    private JLabel lblRandomProcreationRelationshiplessDiceSize;
    private JSpinner spnRandomProcreationRelationshiplessDiceSize;

    private JPanel pnlRandomSexualityPanel;
    private JLabel lblNoInterestInRelationshipsDiceSize;
    private JSpinner spnNoInterestInRelationshipsDiceSize;
    private JLabel lblPrefersSameSexDiceSize;
    private JSpinner spnPrefersSameSexDiceSize;
    private JLabel lblPrefersBothSexesDiceSize;
    private JSpinner spnPrefersBothSexesDiceSize;

    private boolean created;

    /**
     * Builds the Procreation page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared relationships options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Procreation Page
     */
    @Nonnull JPanel createPanel(@Nullable RelationshipsOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_hanseatic_league.png";
        procreationHeader = new CampaignOptionsHeaderPanel("ProcreationPage", imageAddress);

        // Contents
        comboBabySurnameStyle = new MMComboBox<>("comboBabySurnameStyle", BabySurnameStyle.values());
        comboRandomProcreationMethod = new MMComboBox<>("comboRandomProcreationMethod",
                RandomProcreationMethod.values());
        pnlProcreationGeneralOptionsPanel = createProcreationGeneralOptionsPanel();
        pnlRandomProcreationPanel = createRandomProcreationPanel();
        pnlRandomSexualityPanel = createRandomSexualityPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("ProcreationPage", "ProcreationPage", imageAddress)
                .header(procreationHeader)
                .quote("procreationPage")
                .section("lblProcreationGeneralOptionsPanel.text",
                        "lblProcreationGeneralOptionsPanel.summary",
                        pnlProcreationGeneralOptionsPanel)
                .section("lblRandomProcreationPanel.text",
                        "lblRandomProcreationPanel.summary",
                        pnlRandomProcreationPanel)
                .section("lblRandomSexualityPanel.text", "lblRandomSexualityPanel.summary", pnlRandomSexualityPanel)
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Creates the panel for general procreation settings. This panel includes
     * controls for determining maternity leave,
     * surname styles, and logging options.
     *
     * @return a {@link JPanel} containing general procreation options.
     */
    private @Nonnull JPanel createProcreationGeneralOptionsPanel() {
        // Contents
        chkUseManualProcreation = new CampaignOptionsCheckBox("UseManualProcreation");
        chkUseManualProcreation.addMouseListener(createTipPanelUpdater("UseManualProcreation"));
        chkUseClanPersonnelProcreation = new CampaignOptionsCheckBox("UseClanPersonnelProcreation");
        chkUseClanPersonnelProcreation.addMouseListener(createTipPanelUpdater("UseClanPersonnelProcreation"));
        chkUsePrisonerProcreation = new CampaignOptionsCheckBox("UsePrisonerProcreation");
        chkUsePrisonerProcreation
                .addMouseListener(createTipPanelUpdater("UsePrisonerProcreation"));

        lblMultiplePregnancyOccurrences = new CampaignOptionsLabel("MultiplePregnancyOccurrences");
        lblMultiplePregnancyOccurrences.addMouseListener(createTipPanelUpdater("MultiplePregnancyOccurrences"));
        spnMultiplePregnancyOccurrences = new CampaignOptionsSpinner("MultiplePregnancyOccurrences",
                50, 1, 1000, 1);
        spnMultiplePregnancyOccurrences.addMouseListener(createTipPanelUpdater("MultiplePregnancyOccurrences"));

        lblBabySurnameStyle = new CampaignOptionsLabel("BabySurnameStyle");
        lblBabySurnameStyle.addMouseListener(createTipPanelUpdater("BabySurnameStyle"));
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
        comboBabySurnameStyle.addMouseListener(createTipPanelUpdater("BabySurnameStyle"));

        chkAssignNonPrisonerBabiesFounderTag = new CampaignOptionsCheckBox("AssignNonPrisonerBabiesFounderTag");
        chkAssignNonPrisonerBabiesFounderTag.addMouseListener(createTipPanelUpdater("AssignNonPrisonerBabiesFounderTag"));
        chkAssignChildrenOfFoundersFounderTag = new CampaignOptionsCheckBox("AssignChildrenOfFoundersFounderTag");
        chkAssignChildrenOfFoundersFounderTag.addMouseListener(createTipPanelUpdater("AssignChildrenOfFoundersFounderTag"));
        chkDetermineFatherAtBirth = new CampaignOptionsCheckBox("DetermineFatherAtBirth");
        chkDetermineFatherAtBirth
                .addMouseListener(createTipPanelUpdater("DetermineFatherAtBirth"));
        chkDisplayTrueDueDate = new CampaignOptionsCheckBox("DisplayTrueDueDate");
        chkDisplayTrueDueDate.addMouseListener(createTipPanelUpdater("DisplayTrueDueDate"));

        lblNoInterestInChildrenDiceSize = new CampaignOptionsLabel("NoInterestInChildrenDiceSize");
        lblNoInterestInChildrenDiceSize.addMouseListener(createTipPanelUpdater("NoInterestInChildrenDiceSize"));
        spnNoInterestInChildrenDiceSize = new CampaignOptionsSpinner("NoInterestInChildrenDiceSize",
                3, 1, 100000, 1);
        spnNoInterestInChildrenDiceSize.addMouseListener(createTipPanelUpdater("NoInterestInChildrenDiceSize"));

        chkUseMaternityLeave = new CampaignOptionsCheckBox("UseMaternityLeave");
        chkUseMaternityLeave.addMouseListener(createTipPanelUpdater("UseMaternityLeave"));
        chkLogProcreation = new CampaignOptionsCheckBox("LogProcreation");
        chkLogProcreation.addMouseListener(createTipPanelUpdater("LogProcreation"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("ProcreationGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkUseManualProcreation,
                chkUseClanPersonnelProcreation,
                chkUsePrisonerProcreation);
        panel.addRow(lblMultiplePregnancyOccurrences, spnMultiplePregnancyOccurrences);
        panel.addRow(lblBabySurnameStyle, comboBabySurnameStyle);
        panel.addCheckBoxGrid(2,
                chkAssignNonPrisonerBabiesFounderTag,
                chkAssignChildrenOfFoundersFounderTag,
                chkDetermineFatherAtBirth,
                chkDisplayTrueDueDate);
        panel.addRow(lblNoInterestInChildrenDiceSize, spnNoInterestInChildrenDiceSize);
        panel.addCheckBoxGrid(2, chkUseMaternityLeave, chkLogProcreation);

        return panel;
    }

    /**
     * Creates the panel for configuring random procreation options. Options include
     * toggles for relationshipless
     * procreation and dice settings.
     *
     * @return a {@link JPanel} containing random procreation settings.
     */
    private @Nonnull JPanel createRandomProcreationPanel() {
        // Contents
        lblRandomProcreationMethod = new CampaignOptionsLabel("RandomProcreationMethod");
        lblRandomProcreationMethod.addMouseListener(createTipPanelUpdater("RandomProcreationMethod"));
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
        comboRandomProcreationMethod.addMouseListener(createTipPanelUpdater("RandomProcreationMethod"));

        chkUseRelationshiplessRandomProcreation = new CampaignOptionsCheckBox(
                "UseRelationshiplessRandomProcreation");
        chkUseRelationshiplessRandomProcreation.addMouseListener(createTipPanelUpdater("UseRelationshiplessRandomProcreation"));
        chkUseRandomClanPersonnelProcreation = new CampaignOptionsCheckBox("UseRandomClanPersonnelProcreation");
        chkUseRandomClanPersonnelProcreation.addMouseListener(createTipPanelUpdater("UseRandomClanPersonnelProcreation"));
        chkUseRandomPrisonerProcreation = new CampaignOptionsCheckBox("UseRandomPrisonerProcreation");
        chkUseRandomPrisonerProcreation.addMouseListener(createTipPanelUpdater("UseRandomPrisonerProcreation"));

        lblRandomProcreationRelationshipDiceSize = new CampaignOptionsLabel(
                "RandomProcreationRelationshipDiceSize");
        lblRandomProcreationRelationshipDiceSize.addMouseListener(createTipPanelUpdater("RandomProcreationRelationshipDiceSize"));
        spnRandomProcreationRelationshipDiceSize = new CampaignOptionsSpinner(
                "RandomProcreationRelationshipDiceSize",
                621, 0, 100000, 1);
        spnRandomProcreationRelationshipDiceSize.addMouseListener(createTipPanelUpdater("RandomProcreationRelationshipDiceSize"));

        lblRandomProcreationRelationshiplessDiceSize = new CampaignOptionsLabel(
                "RandomProcreationRelationshiplessDiceSize");
        lblRandomProcreationRelationshiplessDiceSize.addMouseListener(createTipPanelUpdater("RandomProcreationRelationshiplessDiceSize"));
        spnRandomProcreationRelationshiplessDiceSize = new CampaignOptionsSpinner(
                "RandomProcreationRelationshiplessDiceSize",
                1861,
                0,
                100000,
                1);
        spnRandomProcreationRelationshiplessDiceSize.addMouseListener(createTipPanelUpdater("RandomProcreationRelationshiplessDiceSize"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomProcreationPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblRandomProcreationMethod, comboRandomProcreationMethod);
        panel.addCheckBoxGrid(2,
                chkUseRelationshiplessRandomProcreation,
                chkUseRandomClanPersonnelProcreation,
                chkUseRandomPrisonerProcreation);
        panel.addRow(lblRandomProcreationRelationshipDiceSize, spnRandomProcreationRelationshipDiceSize);
        panel.addRow(lblRandomProcreationRelationshiplessDiceSize, spnRandomProcreationRelationshiplessDiceSize);

        return panel;
    }

    private @Nonnull JPanel createRandomSexualityPanel() {
        // Contents
        lblNoInterestInRelationshipsDiceSize = new CampaignOptionsLabel("NoInterestInRelationshipsDiceSize",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblNoInterestInRelationshipsDiceSize.addMouseListener(createTipPanelUpdater("NoInterestInRelationshipsDiceSize"));
        spnNoInterestInRelationshipsDiceSize = new CampaignOptionsSpinner("NoInterestInRelationshipsDiceSize",
                10, 1, 100000, 1);
        spnNoInterestInRelationshipsDiceSize.addMouseListener(createTipPanelUpdater("NoInterestInRelationshipsDiceSize"));

        lblPrefersSameSexDiceSize = new CampaignOptionsLabel("PrefersSameSexDiceSize",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblPrefersSameSexDiceSize.addMouseListener(createTipPanelUpdater("PrefersSameSexDiceSize"));
        spnPrefersSameSexDiceSize = new CampaignOptionsSpinner("PrefersSameSexDiceSize",
                14, 0, 100000, 1);
        spnPrefersSameSexDiceSize.addMouseListener(createTipPanelUpdater("PrefersSameSexDiceSize"));

        lblPrefersBothSexesDiceSize = new CampaignOptionsLabel("PrefersBothSexesDiceSize",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblPrefersBothSexesDiceSize.addMouseListener(createTipPanelUpdater("PrefersBothSexesDiceSize"));
        spnPrefersBothSexesDiceSize = new CampaignOptionsSpinner("PrefersBothSexesDiceSize",
                20, 0, 100000, 1);
        spnPrefersBothSexesDiceSize.addMouseListener(createTipPanelUpdater("PrefersBothSexesDiceSize"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("RandomSexualityPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblNoInterestInRelationshipsDiceSize, spnNoInterestInRelationshipsDiceSize);
        panel.addRow(lblPrefersSameSexDiceSize, spnPrefersSameSexDiceSize);
        panel.addRow(lblPrefersBothSexesDiceSize, spnPrefersBothSexesDiceSize);

        return panel;
    }

    /**
     * Copies procreation values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared relationships options model to read values from
     */
    void readFromModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseManualProcreation.setSelected(model.useManualProcreation);
        chkUseClanPersonnelProcreation.setSelected(model.useClanPersonnelProcreation);
        chkUsePrisonerProcreation.setSelected(model.usePrisonerProcreation);
        spnMultiplePregnancyOccurrences.setValue(model.multiplePregnancyOccurrences);
        comboBabySurnameStyle.setSelectedItem(model.babySurnameStyle);
        chkAssignNonPrisonerBabiesFounderTag.setSelected(model.assignNonPrisonerBabiesFounderTag);
        chkAssignChildrenOfFoundersFounderTag.setSelected(model.assignChildrenOfFoundersFounderTag);
        chkDetermineFatherAtBirth.setSelected(model.determineFatherAtBirth);
        chkDisplayTrueDueDate.setSelected(model.displayTrueDueDate);
        spnNoInterestInChildrenDiceSize.setValue(model.noInterestInChildrenDiceSize);
        chkUseMaternityLeave.setSelected(model.useMaternityLeave);
        chkLogProcreation.setSelected(model.logProcreation);
        comboRandomProcreationMethod.setSelectedItem(model.randomProcreationMethod);
        chkUseRelationshiplessRandomProcreation.setSelected(model.useRelationshiplessRandomProcreation);
        chkUseRandomClanPersonnelProcreation.setSelected(model.useRandomClanPersonnelProcreation);
        chkUseRandomPrisonerProcreation.setSelected(model.useRandomPrisonerProcreation);
        spnRandomProcreationRelationshipDiceSize.setValue(model.randomProcreationRelationshipDiceSize);
        spnRandomProcreationRelationshiplessDiceSize.setValue(model.randomProcreationRelationshiplessDiceSize);
        spnNoInterestInRelationshipsDiceSize.setValue(model.noInterestInRelationshipsDiceSize);
        spnPrefersSameSexDiceSize.setValue(model.interestedInSameSexDiceSize);
        spnPrefersBothSexesDiceSize.setValue(model.interestedInBothSexesDiceSize);
    }

    /**
     * Copies procreation values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared relationships options model to write values into
     */
    void writeToModel(@Nullable RelationshipsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useManualProcreation = chkUseManualProcreation.isSelected();
        model.useClanPersonnelProcreation = chkUseClanPersonnelProcreation.isSelected();
        model.usePrisonerProcreation = chkUsePrisonerProcreation.isSelected();
        model.multiplePregnancyOccurrences = (int) spnMultiplePregnancyOccurrences.getValue();
        model.babySurnameStyle = comboBabySurnameStyle.getSelectedItem();
        model.assignNonPrisonerBabiesFounderTag = chkAssignNonPrisonerBabiesFounderTag.isSelected();
        model.assignChildrenOfFoundersFounderTag = chkAssignChildrenOfFoundersFounderTag.isSelected();
        model.determineFatherAtBirth = chkDetermineFatherAtBirth.isSelected();
        model.displayTrueDueDate = chkDisplayTrueDueDate.isSelected();
        model.noInterestInChildrenDiceSize = (int) spnNoInterestInChildrenDiceSize.getValue();
        model.useMaternityLeave = chkUseMaternityLeave.isSelected();
        model.logProcreation = chkLogProcreation.isSelected();
        model.randomProcreationMethod = comboRandomProcreationMethod.getSelectedItem();
        model.useRelationshiplessRandomProcreation = chkUseRelationshiplessRandomProcreation.isSelected();
        model.useRandomClanPersonnelProcreation = chkUseRandomClanPersonnelProcreation.isSelected();
        model.useRandomPrisonerProcreation = chkUseRandomPrisonerProcreation.isSelected();
        model.randomProcreationRelationshipDiceSize = (int) spnRandomProcreationRelationshipDiceSize.getValue();
        model.randomProcreationRelationshiplessDiceSize = (int) spnRandomProcreationRelationshiplessDiceSize
                .getValue();
        model.interestedInSameSexDiceSize = (int) spnPrefersSameSexDiceSize.getValue();
        model.noInterestInRelationshipsDiceSize = (int) spnNoInterestInRelationshipsDiceSize.getValue();
        model.interestedInBothSexesDiceSize = (int) spnPrefersBothSexesDiceSize.getValue();
    }
}
