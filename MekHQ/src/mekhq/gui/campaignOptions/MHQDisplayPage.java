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

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.enums.PersonnelFilterStyle;

/**
 * The Display page of the MekHQ Client Options dialog: a General section (date formats, GUI scale, and assorted display
 * toggles), an Interstellar Map section (jump/acquisition/contract overlay radii with their colours), and a Personnel
 * List section (filter style and daily-report toggles). Owns its controls and copies them back into the shared
 * {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQDisplayPage extends MHQOptionsPage {
    // Display - General
    private JTextField fieldDisplayDateFormat;
    private JTextField fieldLongDisplayDateFormat;
    private JSlider guiScaleSlider;
    private CampaignOptionsCheckBox chkHideUnitFluff;
    private CampaignOptionsCheckBox chkHistoricalDailyLog;
    private CampaignOptionsCheckBox chkCompanyGeneratorStartup;
    private CampaignOptionsCheckBox chkShowCompanyGenerator;
    private CampaignOptionsCheckBox chkShowUnitPicturesOnTOE;

    // Display - Interstellar Map
    private CampaignOptionsCheckBox chkInterstellarMapShowJumpRadius;
    private CampaignOptionsSpinner spinnerInterstellarMapShowJumpRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapJumpRadiusColour;
    private CampaignOptionsCheckBox chkInterstellarMapShowPlanetaryAcquisitionRadius;
    private CampaignOptionsSpinner spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapPlanetaryAcquisitionRadiusColour;
    private CampaignOptionsCheckBox chkInterstellarMapShowContractSearchRadius;
    private ColourSelectorButton btnInterstellarMapContractSearchRadiusColour;

    // Display - Personnel List
    private JComboBox<PersonnelFilterStyle> comboPersonnelFilterStyle;
    private CampaignOptionsCheckBox chkPersonnelFilterOnPrimaryRole;
    private CampaignOptionsCheckBox chkUnifiedDailyReport;
    private CampaignOptionsCheckBox chkEnableDailyReportAggregateTab;

    MHQDisplayPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        JComponent generalContent = createDisplayGeneralSection();
        JComponent interstellarContent = createDisplayInterstellarSection();
        JComponent personnelContent = createDisplayPersonnelSection();
        // Register each section body's tips before the page is built; |= always evaluates its right side, so no
        // section's registration is skipped.
        boolean hasTooltips = registerDetailsTips(generalContent);
        hasTooltips |= registerDetailsTips(interstellarContent);
        hasTooltips |= registerDetailsTips(personnelContent);
        Component page = pageBuilder("MHQDisplayPage", hasTooltips)
                     .section("lblMHQDisplayGeneralSection.text", "lblMHQDisplayGeneralSection.summary",
                           generalContent)
                     .section("lblMHQDisplayInterstellarSection.text", "lblMHQDisplayInterstellarSection.summary",
                           interstellarContent)
                     .section("lblMHQDisplayPersonnelSection.text", "lblMHQDisplayPersonnelSection.summary",
                           personnelContent)
                     .build();
        created = true;
        return page;
    }

    private JPanel createDisplayGeneralSection() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQDisplayGeneralContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);

        fieldDisplayDateFormat = new JTextField(model.displayDateFormat, 14);
        fieldDisplayDateFormat.setName("txtlabelDisplayDateFormat");
        panel.addRow(new CampaignOptionsLabel(RESOURCE_BUNDLE, "labelDisplayDateFormat"),
              dateFormatControl(fieldDisplayDateFormat));
        fieldLongDisplayDateFormat = new JTextField(model.longDisplayDateFormat, 14);
        fieldLongDisplayDateFormat.setName("txtlabelLongDisplayDateFormat");
        panel.addRow(new CampaignOptionsLabel(RESOURCE_BUNDLE, "labelLongDisplayDateFormat"),
              dateFormatControl(fieldLongDisplayDateFormat));

        String guiScaleText = Messages.getString("CommonSettingsDialog.guiScale");
        JLabel guiScaleLabel = new JLabel(guiScaleText);
        guiScaleSlider = new JSlider(7, 24);
        guiScaleSlider.setName("guiScale");
        guiScaleSlider.setMinorTickSpacing(1);
        // Six labels divide the 70-240% range into symmetric 30%/40% gaps without crowding either endpoint.
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(7, new JLabel("70%"));
        labelTable.put(10, new JLabel("100%"));
        labelTable.put(14, new JLabel("140%"));
        labelTable.put(17, new JLabel("170%"));
        labelTable.put(21, new JLabel("210%"));
        labelTable.put(24, new JLabel("240%"));
        guiScaleSlider.setLabelTable(labelTable);
        guiScaleSlider.setPaintTicks(true);
        guiScaleSlider.setPaintLabels(true);
        guiScaleSlider.setValue(model.guiScaleValue);
        guiScaleSlider.setToolTipText(Messages.getString("CommonSettingsDialog.guiScaleTT"));
        guiScaleSlider.setPreferredSize(new Dimension(UIUtil.scaleForGUI(320),
              guiScaleSlider.getPreferredSize().height));
        JPanel guiScaleControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        guiScaleControl.setOpaque(false);
        guiScaleControl.add(guiScaleSlider);
        panel.addRow(guiScaleLabel, guiScaleControl);

        chkHideUnitFluff = checkBox("optionHideUnitFluff", model.hideUnitFluff);
        chkHistoricalDailyLog = checkBox("optionHistoricalDailyLog", model.historicalDailyLog);
        chkCompanyGeneratorStartup = checkBox("chkCompanyGeneratorStartup", model.companyGeneratorStartup,
              getMetadata(null, CampaignOptionFlag.UNIMPLEMENTED));
        chkShowCompanyGenerator = checkBox("chkShowCompanyGenerator", model.showCompanyGenerator);
        chkShowUnitPicturesOnTOE = checkBox("chkShowUnitPicturesOnTOE", model.showUnitPicturesOnTOE);
        panel.addCheckBoxGrid(2, chkHideUnitFluff, chkHistoricalDailyLog, chkCompanyGeneratorStartup,
              chkShowCompanyGenerator, chkShowUnitPicturesOnTOE);

        return panel;
    }

    /**
     * Builds the control for a date-format row: the supplied editable pattern field plus a live example label showing
     * today's date in the entered pattern (or an error when it is invalid). The field's value is read back into the
     * model - only when it is a valid pattern - by {@link #writeToModel()}, matching the original dialog.
     */
    private JComponent dateFormatControl(JTextField field) {
        JLabel example = new JLabel();
        String invalidDateFormatText = getTextAt(RESOURCE_BUNDLE, "invalidDateFormat.error");
        Runnable updateExample = () -> example.setText(validateDateFormat(field.getText())
              ? LocalDate.now().format(DateTimeFormatter.ofPattern(field.getText())
                    .withLocale(MekHQ.getMHQOptions().getDateLocale()))
              : invalidDateFormatText);
        // Refresh on every document edit (typing, paste, delete), not only on Enter, so the example and validation
        // stay in step with the field instead of lagging behind until an ActionEvent fires.
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateExample.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateExample.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateExample.run();
            }
        });
        updateExample.run();

        JPanel control = new JPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(6), 0));
        control.setOpaque(false);
        control.add(field);
        control.add(example);
        return control;
    }

    private boolean validateDateFormat(String format) {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern(format)
                  .withLocale(MekHQ.getMHQOptions().getDateLocale()));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private JPanel createDisplayInterstellarSection() {
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQDisplayInterstellarContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);

        chkInterstellarMapShowJumpRadius =
              checkBox("chkInterstellarMapShowJumpRadius", model.interstellarMapShowJumpRadius);
        CampaignOptionsLabel jumpZoomLabel =
              new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblInterstellarMapShowJumpRadiusMinimumZoom");
        spinnerInterstellarMapShowJumpRadiusMinimumZoom = new CampaignOptionsSpinner(RESOURCE_BUNDLE,
              "lblInterstellarMapShowJumpRadiusMinimumZoom", 3d, 0d, 10d, 0.5);
        spinnerInterstellarMapShowJumpRadiusMinimumZoom.setValue(model.interstellarMapShowJumpRadiusMinimumZoom);
        btnInterstellarMapJumpRadiusColour =
              colourButton("btnInterstellarMapJumpRadiusColour", model.interstellarMapJumpRadiusColour);
        bindRadiusGating(chkInterstellarMapShowJumpRadius, jumpZoomLabel,
              spinnerInterstellarMapShowJumpRadiusMinimumZoom, btnInterstellarMapJumpRadiusColour);

        chkInterstellarMapShowPlanetaryAcquisitionRadius = checkBox(
              "chkInterstellarMapShowPlanetaryAcquisitionRadius", model.interstellarMapShowPlanetaryAcquisitionRadius);
        CampaignOptionsLabel acquisitionZoomLabel = new CampaignOptionsLabel(RESOURCE_BUNDLE,
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom");
        spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom = new CampaignOptionsSpinner(RESOURCE_BUNDLE,
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom", 2d, 0d, 10d, 0.5);
        spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setValue(
              model.interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom);
        btnInterstellarMapPlanetaryAcquisitionRadiusColour = colourButton(
              "btnInterstellarMapPlanetaryAcquisitionRadiusColour",
              model.interstellarMapPlanetaryAcquisitionRadiusColour);
        bindRadiusGating(chkInterstellarMapShowPlanetaryAcquisitionRadius, acquisitionZoomLabel,
              spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom,
              btnInterstellarMapPlanetaryAcquisitionRadiusColour);

        chkInterstellarMapShowContractSearchRadius = checkBox("chkInterstellarMapShowContractSearchRadius",
              model.interstellarMapShowContractSearchRadius);
        btnInterstellarMapContractSearchRadiusColour = colourButton("btnInterstellarMapContractSearchRadiusColour",
              model.interstellarMapContractSearchRadiusColour);
        bindRadiusGating(chkInterstellarMapShowContractSearchRadius, btnInterstellarMapContractSearchRadiusColour);

        // The "Show ..." toggles share the left column with the "Minimum Zoom" labels, so keep their box and text packed
        // to the start instead of centred when the grid stretches that column to the shared label width.
        for (JCheckBox toggle : List.of(chkInterstellarMapShowJumpRadius,
              chkInterstellarMapShowPlanetaryAcquisitionRadius, chkInterstellarMapShowContractSearchRadius)) {
            toggle.setHorizontalAlignment(SwingConstants.LEADING);
        }
        // One shared width for the three swatch buttons so they line up in the right-hand column.
        setUniformWidth(List.of(btnInterstellarMapJumpRadiusColour, btnInterstellarMapPlanetaryAcquisitionRadiusColour,
              btnInterstellarMapContractSearchRadiusColour));

        // Each overlay is a "[Show X] [colour]" row, then a "[Minimum Zoom] [spinner]" row where one applies, reusing
        // the standard two-column grid so the swatches align under one another and with the spinners.
        panel.addComponentGrid(2, chkInterstellarMapShowJumpRadius, btnInterstellarMapJumpRadiusColour);
        panel.addRow(jumpZoomLabel, spinnerInterstellarMapShowJumpRadiusMinimumZoom);
        panel.addComponentGrid(2, chkInterstellarMapShowPlanetaryAcquisitionRadius,
              btnInterstellarMapPlanetaryAcquisitionRadiusColour);
        panel.addRow(acquisitionZoomLabel, spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom);
        panel.addComponentGrid(2, chkInterstellarMapShowContractSearchRadius,
              btnInterstellarMapContractSearchRadiusColour);

        return panel;
    }

    /**
     * Enables every component in {@code gated} only while {@code checkBox} is selected, and keeps them in sync when it
     * is toggled - matching the original dialog's gating of each interstellar-map radius group. The gated set varies by
     * group; the contract-search overlay has no minimum-zoom control.
     */
    private void bindRadiusGating(JCheckBox checkBox, JComponent... gated) {
        Runnable sync = () -> {
            boolean enabled = checkBox.isSelected();
            for (JComponent component : gated) {
                component.setEnabled(enabled);
            }
        };
        checkBox.addActionListener(evt -> sync.run());
        sync.run();
    }

    private JPanel createDisplayPersonnelSection() {
        CampaignOptionsLabel filterStyleLabel =
              new CampaignOptionsLabel(RESOURCE_BUNDLE, "optionPersonnelFilterStyle");
        comboPersonnelFilterStyle = new JComboBox<>(PersonnelFilterStyle.values());
        comboPersonnelFilterStyle.setName("optionPersonnelFilterStyle");
        comboPersonnelFilterStyle.setSelectedItem(model.personnelFilterStyle);
        comboPersonnelFilterStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PersonnelFilterStyle style) {
                    list.setToolTipText(style.getToolTipText());
                }
                return this;
            }
        });

        chkPersonnelFilterOnPrimaryRole =
              checkBox("optionPersonnelFilterOnPrimaryRole", model.personnelFilterOnPrimaryRole);
        chkUnifiedDailyReport = checkBox("chkUnifiedDailyReport", model.unifiedDailyReport);
        chkEnableDailyReportAggregateTab = checkBox("chkEnableDailyReportAggregateTab", model.aggregateDailyReport,
              getMetadata(null, CampaignOptionFlag.IMPORTANT));

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQDisplayPersonnelContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addRow(filterStyleLabel, comboPersonnelFilterStyle);
        panel.addCheckBoxGrid(2, chkPersonnelFilterOnPrimaryRole, chkUnifiedDailyReport,
              chkEnableDailyReportAggregateTab);

        return panel;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        // Only accept a valid pattern, matching the original dialog; an invalid one keeps the previous value.
        if (validateDateFormat(fieldDisplayDateFormat.getText())) {
            model.displayDateFormat = fieldDisplayDateFormat.getText();
        }
        if (validateDateFormat(fieldLongDisplayDateFormat.getText())) {
            model.longDisplayDateFormat = fieldLongDisplayDateFormat.getText();
        }
        model.guiScaleValue = guiScaleSlider.getValue();
        model.hideUnitFluff = chkHideUnitFluff.isSelected();
        model.historicalDailyLog = chkHistoricalDailyLog.isSelected();
        model.companyGeneratorStartup = chkCompanyGeneratorStartup.isSelected();
        model.showCompanyGenerator = chkShowCompanyGenerator.isSelected();
        model.showUnitPicturesOnTOE = chkShowUnitPicturesOnTOE.isSelected();

        model.interstellarMapShowJumpRadius = chkInterstellarMapShowJumpRadius.isSelected();
        model.interstellarMapShowJumpRadiusMinimumZoom =
              (Double) spinnerInterstellarMapShowJumpRadiusMinimumZoom.getValue();
        model.interstellarMapJumpRadiusColour = btnInterstellarMapJumpRadiusColour.getColour();
        model.interstellarMapShowPlanetaryAcquisitionRadius =
              chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected();
        model.interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom =
              (Double) spinnerInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.getValue();
        model.interstellarMapPlanetaryAcquisitionRadiusColour =
              btnInterstellarMapPlanetaryAcquisitionRadiusColour.getColour();
        model.interstellarMapShowContractSearchRadius = chkInterstellarMapShowContractSearchRadius.isSelected();
        model.interstellarMapContractSearchRadiusColour = btnInterstellarMapContractSearchRadiusColour.getColour();

        model.personnelFilterStyle =
              (PersonnelFilterStyle) Objects.requireNonNull(comboPersonnelFilterStyle.getSelectedItem());
        model.personnelFilterOnPrimaryRole = chkPersonnelFilterOnPrimaryRole.isSelected();
        model.unifiedDailyReport = chkUnifiedDailyReport.isSelected();
        model.aggregateDailyReport = chkEnableDailyReportAggregateTab.isSelected();
    }
}
