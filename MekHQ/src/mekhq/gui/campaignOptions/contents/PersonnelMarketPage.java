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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPairedFieldGridPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * The {@code PersonnelMarketPage} class builds and manages the Personnel Market leaf page of the Campaign Options
 * dialog. It owns the widgets for personnel market configuration - the market style and (legacy) type, Dylan's weight,
 * report refresh toggles, and the per-skill-level random removal targets - and synchronises them with a shared
 * {@link MarketsOptionsModel}.
 *
 * <p>This view is a sub-component of {@link MarketsPages}: the model snapshot and the overall load/apply lifecycle still
 * live on {@code MarketsPages}, while this class is responsible only for constructing the Personnel Market panel and
 * copying personnel market values to and from the model. The page is built lazily; until
 * {@link #createPanel(MarketsOptionsModel)} is called, {@link #readFromModel(MarketsOptionsModel)} and
 * {@link #writeToModel(MarketsOptionsModel)} are no-ops.</p>
 */
class PersonnelMarketPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;
    private static final int LABEL_CONTROL_GAP = 12;
    private static final int GRID_CONTROL_COLUMN_WIDTH = 100;
    private static final int CHECKBOX_GRID_COLUMNS = 2;
    private static final int REMOVAL_TARGET_GRID_COLUMNS = 2;

    private CampaignOptionsHeaderPanel personnelMarketHeader;
    private JPanel pnlPersonnelMarketGeneralOptions;
    private JLabel lblPersonnelMarketStyle;
    private MMComboBox<PersonnelMarketStyle> comboPersonnelMarketStyle;
    private JCheckBox chkPersonnelMarketReportRefresh;
    private JCheckBox chkUsePersonnelHireHiringHallOnly;
    @Deprecated(since = "0.50.06")
    private JLabel lblPersonnelMarketType;
    @Deprecated(since = "0.50.06")
    private MMComboBox<String> comboPersonnelMarketType;

    @Deprecated(since = "0.50.06")
    private JPanel pnlRemovalTargets;
    @Deprecated(since = "0.50.06")
    private JLabel lblPersonnelMarketDylansWeight;
    @Deprecated(since = "0.50.06")
    private JSpinner spnPersonnelMarketDylansWeight;
    @Deprecated(since = "0.50.06")
    private Map<SkillLevel, JLabel> lblPersonnelMarketRandomRemovalTargets;
    @Deprecated(since = "0.50.06")
    private Map<SkillLevel, JSpinner> spnPersonnelMarketRandomRemovalTargets;

    private boolean created;

    /**
     * Creates and returns the JPanel representing the Personnel Market configuration page.
     * <p>
     * This page includes general personnel market settings, as well as removal target configuration options for various
     * skill levels.
     *
     * @param model the shared markets options model to populate the freshly built controls from
     *
     * @return A {@link JPanel} for the Personnel Market configuration page.
     */
    @Nonnull JPanel createPanel(@Nullable MarketsOptionsModel model) {
        comboPersonnelMarketStyle = new MMComboBox<>("comboPersonnelMarketStyle", PersonnelMarketStyle.values());
        lblPersonnelMarketRandomRemovalTargets = new HashMap<>();
        spnPersonnelMarketRandomRemovalTargets = new HashMap<>();

        // Header
        String imageAddress = getImageDirectory() + "logo_st_ives_compact.png";
        personnelMarketHeader = new CampaignOptionsHeaderPanel("PersonnelMarketPage", imageAddress);

        // Contents
        pnlPersonnelMarketGeneralOptions = createPersonnelMarketGeneralOptionsPanel();
        pnlRemovalTargets = createPersonnelMarketRemovalOptionsPanel();

        final JPanel panel = CampaignOptionsPagePanel.builder("PersonnelMarketPage", "PersonnelMarketPage",
                imageAddress)
                .header(personnelMarketHeader)
                .quote("personnelMarketPage")
                .section("lblPersonnelMarketGeneralOptionsPanel.text",
                        "lblPersonnelMarketGeneralOptionsPanel.summary",
                        pnlPersonnelMarketGeneralOptions)
                .section("lblPersonnelMarketRemovalOptionsPanel.text",
                        "lblPersonnelMarketRemovalOptionsPanel.summary",
                        pnlRemovalTargets,
                        getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM))
                .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Retrieves the available personnel market type options for display in a combo box.
     * <p>
     * These types are fetched from the {@link PersonnelMarketServiceManager} and represent the available personnel
     * market methods configured for the campaign.
     *
     * @return A {@link DefaultComboBoxModel} containing the personnel market type options.
     */
    @Deprecated(since = "0.50.06")
    private static DefaultComboBoxModel<String> getPersonnelMarketTypeOptions() {
        final DefaultComboBoxModel<String> personnelMarketTypeModel = new DefaultComboBoxModel<>();
        for (final PersonnelMarketMethod method : PersonnelMarketServiceManager.getInstance()
                .getAllServices(true)) {
            personnelMarketTypeModel.addElement(method.getModuleName());
        }
        return personnelMarketTypeModel;
    }

    /**
     * Builds the general options panel for the Personnel Market page, which includes settings such as the personnel
     * market type, Dylan's weight, and options like report refresh toggles.
     * <p>
     * These components are laid out into a panel and returned for use in the UI.
     *
     * @return A {@link JPanel} representing the general options within the Personnel Market page.
     */
    private @Nonnull JPanel createPersonnelMarketGeneralOptionsPanel() {
        // Contents
        lblPersonnelMarketStyle = new CampaignOptionsLabel("PersonnelMarketStyle",
                getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT,
                        CampaignOptionFlag.RECOMMENDED));
        lblPersonnelMarketStyle
                .addMouseListener(createTipPanelUpdater("PersonnelMarketStyle"));
        comboPersonnelMarketStyle.addMouseListener(createTipPanelUpdater("PersonnelMarketStyle"));

        lblPersonnelMarketType = new CampaignOptionsLabel("PersonnelMarketType");
        lblPersonnelMarketType
                .addMouseListener(createTipPanelUpdater("PersonnelMarketType"));
        comboPersonnelMarketType = new MMComboBox<>("comboPersonnelMarketType", getPersonnelMarketTypeOptions());
        comboPersonnelMarketType
                .addMouseListener(createTipPanelUpdater("PersonnelMarketType"));

        lblPersonnelMarketDylansWeight = new CampaignOptionsLabel("PersonnelMarketDylansWeight");
        lblPersonnelMarketDylansWeight.addMouseListener(createTipPanelUpdater("PersonnelMarketDylansWeight"));
        spnPersonnelMarketDylansWeight = new CampaignOptionsSpinner("PersonnelMarketDylansWeight", 0.3, 0, 1, 0.1);
        spnPersonnelMarketDylansWeight.addMouseListener(createTipPanelUpdater("PersonnelMarketDylansWeight"));

        chkPersonnelMarketReportRefresh = new CampaignOptionsCheckBox("PersonnelMarketReportRefresh");
        chkPersonnelMarketReportRefresh.addMouseListener(createTipPanelUpdater("PersonnelMarketReportRefresh"));

        chkUsePersonnelHireHiringHallOnly = new CampaignOptionsCheckBox("UsePersonnelHireHiringHallOnly",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUsePersonnelHireHiringHallOnly.addMouseListener(createTipPanelUpdater("UsePersonnelHireHiringHallOnly"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("PersonnelMarketGeneralOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addRow(lblPersonnelMarketStyle, comboPersonnelMarketStyle);
        panel.addRow(lblPersonnelMarketType, comboPersonnelMarketType);
        panel.addRow(lblPersonnelMarketDylansWeight, spnPersonnelMarketDylansWeight);
        panel.addCheckBoxGrid(CHECKBOX_GRID_COLUMNS,
                chkPersonnelMarketReportRefresh,
                chkUsePersonnelHireHiringHallOnly);

        return panel;
    }

    /**
     * Creates and configures the removal options panel for the Personnel Market page.
     * <p>
     * This panel includes settings for removal targets, which are based on various {@link SkillLevel} entries. Each
     * skill level configuration includes both a label and an associated spinner for setting values.
     *
     * @return A {@link JPanel} containing removal options for the Personnel Market.
     */
    private @Nonnull JPanel createPersonnelMarketRemovalOptionsPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel jLabel = new JLabel(skillLevel.toString());
            lblPersonnelMarketRandomRemovalTargets.put(skillLevel, jLabel);

            final JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
            CampaignOptionsSpinner.installSelectAllOnFocus(jSpinner);

            spnPersonnelMarketRandomRemovalTargets.put(skillLevel, jSpinner);
        }

        // Layout the Panels
        //
        // Use the same column geometry as the two-column sections (label width + gap, then control width) so the
        // grid columns line up with the General section above it. A compact grid control width keeps the spinners
        // tight while the wider pair widths keep the section broad enough that the section title stays on one line.
        final List<JComponent> labels = new ArrayList<>();
        final List<JComponent> controls = new ArrayList<>();
        for (SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            labels.add(lblPersonnelMarketRandomRemovalTargets.get(skillLevel));
            controls.add(spnPersonnelMarketRandomRemovalTargets.get(skillLevel));
        }

        return createPairedFieldGridPanel("PersonnelMarketRemovalOptionsPanel",
                labels.toArray(new JComponent[0]),
                controls.toArray(new JComponent[0]),
                REMOVAL_TARGET_GRID_COLUMNS,
                GRID_CONTROL_COLUMN_WIDTH);
    }

    /**
     * Creates a dense paired-field grid whose columns line up with the two-column form sections. The first column pair
     * reserves the label-column width plus the label/control gap, and following pairs reserve the control-column width,
     * matching the geometry used by {@link CampaignOptionsFormPanel#addRow}.
     *
     * @param name         the internal panel name
     * @param labels       the labels, one per control
     * @param controls     the controls, one per label
     * @param columnCount  the number of label/control pairs per row
     * @param controlWidth the minimum width of each control within its pair
     *
     * @return the assembled grid panel
     */
    private @Nonnull JPanel createPairedFieldGridPanel(String name, JComponent[] labels, JComponent[] controls,
            int columnCount, int controlWidth) {
        final CampaignOptionsPairedFieldGridPanel panel = new CampaignOptionsPairedFieldGridPanel(name,
                LABEL_COLUMN_WIDTH + LABEL_CONTROL_GAP,
                CONTROL_COLUMN_WIDTH,
                controlWidth,
                columnCount);
        panel.addPairs(labels, controls);
        return panel;
    }

    /**
     * Copies personnel market values from the shared model into this page's controls. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to read values from
     */
    void readFromModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        comboPersonnelMarketStyle.setSelectedItem(model.personnelMarketStyle);
        comboPersonnelMarketType.setSelectedItem(model.personnelMarketName);
        chkPersonnelMarketReportRefresh.setSelected(model.personnelMarketReportRefresh);
        chkUsePersonnelHireHiringHallOnly.setSelected(model.usePersonnelHireHiringHallOnly);
        spnPersonnelMarketDylansWeight.setValue(model.personnelMarketDylansWeight);
        for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
            entry.getValue().setValue(model.personnelMarketRandomRemovalTargets.get(entry.getKey()));
        }
    }

    /**
     * Copies personnel market values from this page's controls into the shared model. This is a no-op until the page has
     * been built.
     *
     * @param model the shared markets options model to write values into
     */
    void writeToModel(@Nullable MarketsOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.personnelMarketStyle = comboPersonnelMarketStyle.getSelectedItem();
        model.personnelMarketName = comboPersonnelMarketType.getSelectedItem();
        model.personnelMarketReportRefresh = chkPersonnelMarketReportRefresh.isSelected();
        model.usePersonnelHireHiringHallOnly = chkUsePersonnelHireHiringHallOnly.isSelected();
        model.personnelMarketDylansWeight = (double) spnPersonnelMarketDylansWeight.getValue();
        for (final Entry<SkillLevel, JSpinner> entry : spnPersonnelMarketRandomRemovalTargets.entrySet()) {
            model.personnelMarketRandomRemovalTargets.put(entry.getKey(), (int) entry.getValue().getValue());
        }
    }
}
