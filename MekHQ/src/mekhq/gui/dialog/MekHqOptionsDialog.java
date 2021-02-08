/*
 * MekHqOptionsDialog.java
 *
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package mekhq.gui.dialog;

import mekhq.MekHQ;
import mekhq.campaign.event.MekHQOptionsChangedEvent;
import mekhq.campaign.universe.enums.CompanyGenerationType;
import mekhq.gui.enums.PersonnelFilterStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class MekHqOptionsDialog extends BaseDialog {
    //region Variable Declarations
    //region Display
    private JTextField optionDisplayDateFormat;
    private JTextField optionLongDisplayDateFormat;
    private JCheckBox optionHistoricalDailyLog;
    private JCheckBox optionShowCompanyGenerator;

    //region Command Center Display
    private JCheckBox optionCommandCenterUseUnitMarket;
    private JCheckBox optionCommandCenterMRMS;
    //endregion Command Center Display

    //region Personnel Tab Display Options
    private JComboBox<PersonnelFilterStyle> optionPersonnelFilterStyle;
    private JCheckBox optionPersonnelFilterOnPrimaryRole;
    //endregion Personnel Tab Display Options
    //endregion Display

    //region Autosave
    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private JCheckBox checkSaveBeforeMissions;
    private JSpinner spinnerSavedGamesCount;
    //endregion Autosave

    //region New Day
    private JCheckBox optionNewDayMRMS;
    //endregion New Day

    //region Campaign XML Save
    private JCheckBox optionPreferGzippedOutput;
    private JCheckBox optionWriteCustomsToXML;
    private JCheckBox optionSaveMothballState;
    private JCheckBox optionSaveCompanyGenerationOptions;
    //endregion Campaign XML Save

    //region Miscellaneous
    private JSpinner optionStartGameDelay;
    private JComboBox<CompanyGenerationType> optionDefaultCompanyGenerationType;
    //endregion Miscellaneous

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog");
    //endregion Variable Declarations

    //region Constructors
    public MekHqOptionsDialog(JFrame parent) {
        super(parent);

        this.initialize(resources);
        this.setInitialState();
    }
    //endregion Constructors

    //region Initialization
    /**
     * This dialog uses the following Mnemonics:
     * C, D, M, M, S, U, W, Y
     */
    @Override
    protected Container createCustomUI() {
        JTabbedPane optionsTabbedPane = new JTabbedPane();
        optionsTabbedPane.setName("optionsTabbedPane");
        optionsTabbedPane.add(resources.getString("displayTab.title"), new JScrollPane(createDisplayTab()));
        optionsTabbedPane.add(resources.getString("autosaveTab.title"), new JScrollPane(createAutosaveTab()));
        optionsTabbedPane.add(resources.getString("newDayTab.title"), new JScrollPane(createNewDayTab()));
        optionsTabbedPane.add(resources.getString("campaignXMLSaveTab.title"), new JScrollPane(createCampaignXMLSaveTab()));
        optionsTabbedPane.add(resources.getString("miscellaneousTab.title"), new JScrollPane(createMiscellaneousTab()));

        return optionsTabbedPane;
    }

    private JPanel createDisplayTab() {
        JLabel labelDisplayDateFormat = new JLabel(resources.getString("labelDisplayDateFormat.text"));
        JLabel labelDisplayDateFormatExample = new JLabel();
        optionDisplayDateFormat = new JTextField();
        optionDisplayDateFormat.addActionListener(evt -> labelDisplayDateFormatExample.setText(
                validateDateFormat(optionDisplayDateFormat.getText())
                        ? LocalDate.now().format(DateTimeFormatter.ofPattern(optionDisplayDateFormat.getText()))
                        : resources.getString("invalidDateFormat.error")));

        JLabel labelLongDisplayDateFormat = new JLabel(resources.getString("labelLongDisplayDateFormat.text"));
        JLabel labelLongDisplayDateFormatExample = new JLabel();
        optionLongDisplayDateFormat = new JTextField();
        optionLongDisplayDateFormat.addActionListener(evt -> labelLongDisplayDateFormatExample.setText(
                validateDateFormat(optionLongDisplayDateFormat.getText())
                        ? LocalDate.now().format(DateTimeFormatter.ofPattern(optionLongDisplayDateFormat.getText()))
                        : resources.getString("invalidDateFormat.error")));

        optionHistoricalDailyLog = new JCheckBox(resources.getString("optionHistoricalDailyLog.text"));
        optionHistoricalDailyLog.setToolTipText(resources.getString("optionHistoricalDailyLog.toolTipText"));

        optionShowCompanyGenerator = new JCheckBox(resources.getString("optionShowCompanyGenerator.text"));
        optionShowCompanyGenerator.setToolTipText(resources.getString("optionShowCompanyGenerator.toolTipText"));
        optionShowCompanyGenerator.setName("optionShowCompanyGenerator");

        //region Command Center Display
        JLabel labelCommandCenterDisplay = new JLabel(resources.getString("labelCommandCenterDisplay.text"));

        optionCommandCenterUseUnitMarket = new JCheckBox(resources.getString("optionCommandCenterUseUnitMarket.text"));
        optionCommandCenterUseUnitMarket.setToolTipText(resources.getString("optionCommandCenterUseUnitMarket.toolTipText"));

        optionCommandCenterMRMS = new JCheckBox(resources.getString("optionCommandCenterMRMS.text"));
        optionCommandCenterMRMS.setToolTipText(resources.getString("optionCommandCenterMRMS.toolTipText"));
        //endregion Command Center Display

        //region Personnel Tab Display Options
        JLabel labelPersonnelDisplay = new JLabel(resources.getString("labelPersonnelDisplay.text"));

        JLabel labelPersonnelFilterStyle = new JLabel(resources.getString("optionPersonnelFilterStyle.text"));
        labelPersonnelFilterStyle.setToolTipText(resources.getString("optionPersonnelFilterStyle.toolTipText"));

        optionPersonnelFilterStyle = new JComboBox<>(PersonnelFilterStyle.values());
        optionPersonnelFilterStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected && (index > -1)) {
                    list.setToolTipText((list.getSelectedValue() instanceof PersonnelFilterStyle)
                            ? ((PersonnelFilterStyle) list.getSelectedValue()).getToolTipText() : "");
                }
                return this;
            }
        });

        optionPersonnelFilterOnPrimaryRole = new JCheckBox(resources.getString("optionPersonnelFilterOnPrimaryRole.text"));
        //endregion Personnel Tab Display Options

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(labelDisplayDateFormat)
                                .addComponent(optionDisplayDateFormat)
                                .addComponent(labelDisplayDateFormatExample, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(labelLongDisplayDateFormat)
                                .addComponent(optionLongDisplayDateFormat)
                                .addComponent(labelLongDisplayDateFormatExample, GroupLayout.Alignment.TRAILING))
                        .addComponent(optionHistoricalDailyLog)
                        .addComponent(optionShowCompanyGenerator)
                        .addComponent(labelCommandCenterDisplay)
                        .addComponent(optionCommandCenterUseUnitMarket)
                        .addComponent(optionCommandCenterMRMS)
                        .addComponent(labelPersonnelDisplay)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(labelPersonnelFilterStyle)
                                .addComponent(optionPersonnelFilterStyle, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addComponent(optionPersonnelFilterOnPrimaryRole)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelDisplayDateFormat)
                                .addComponent(optionDisplayDateFormat)
                                .addComponent(labelDisplayDateFormatExample))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelLongDisplayDateFormat)
                                .addComponent(optionLongDisplayDateFormat)
                                .addComponent(labelLongDisplayDateFormatExample))
                        .addComponent(optionHistoricalDailyLog)
                        .addComponent(optionShowCompanyGenerator)
                        .addComponent(labelCommandCenterDisplay)
                        .addComponent(optionCommandCenterUseUnitMarket)
                        .addComponent(optionCommandCenterMRMS)
                        .addComponent(labelPersonnelDisplay)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelPersonnelFilterStyle)
                                .addComponent(optionPersonnelFilterStyle))
                        .addComponent(optionPersonnelFilterOnPrimaryRole)
        );

        return body;
    }

    private JPanel createAutosaveTab() {
        optionNoSave = new JRadioButton(resources.getString("optionNoSave.text"));
        optionNoSave.setMnemonic(KeyEvent.VK_N);

        optionSaveDaily = new JRadioButton(resources.getString("optionSaveDaily.text"));
        optionSaveDaily.setMnemonic(KeyEvent.VK_D);

        optionSaveWeekly = new JRadioButton(resources.getString("optionSaveWeekly.text"));
        optionSaveWeekly.setMnemonic(KeyEvent.VK_W);

        optionSaveMonthly = new JRadioButton(resources.getString("optionSaveMonthly.text"));
        optionSaveMonthly.setMnemonic(KeyEvent.VK_M);

        optionSaveYearly = new JRadioButton(resources.getString("optionSaveYearly.text"));
        optionSaveYearly.setMnemonic(KeyEvent.VK_Y);

        ButtonGroup saveFrequencyGroup = new ButtonGroup();
        saveFrequencyGroup.add(optionNoSave);
        saveFrequencyGroup.add(optionSaveDaily);
        saveFrequencyGroup.add(optionSaveWeekly);
        saveFrequencyGroup.add(optionSaveMonthly);
        saveFrequencyGroup.add(optionSaveYearly);

        checkSaveBeforeMissions = new JCheckBox(resources.getString("checkSaveBeforeMissions.text"));
        checkSaveBeforeMissions.setMnemonic(KeyEvent.VK_S);

        JLabel labelSavedGamesCount = new JLabel(resources.getString("labelSavedGamesCount.text"));
        spinnerSavedGamesCount = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        labelSavedGamesCount.setLabelFor(spinnerSavedGamesCount);

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(optionNoSave)
                        .addComponent(optionSaveDaily)
                        .addComponent(optionSaveWeekly)
                        .addComponent(optionSaveMonthly)
                        .addComponent(optionSaveYearly)
                        .addComponent(checkSaveBeforeMissions)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(labelSavedGamesCount)
                                .addComponent(spinnerSavedGamesCount, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionNoSave)
                        .addComponent(optionSaveDaily)
                        .addComponent(optionSaveWeekly)
                        .addComponent(optionSaveMonthly)
                        .addComponent(optionSaveYearly)
                        .addComponent(checkSaveBeforeMissions)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelSavedGamesCount)
                                .addComponent(spinnerSavedGamesCount))
        );

        return body;
    }

    private JPanel createNewDayTab() {
        optionNewDayMRMS = new JCheckBox(resources.getString("optionNewDayMRMS.text"));

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(optionNewDayMRMS)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionNewDayMRMS)
        );

        return body;
    }

    private JPanel createCampaignXMLSaveTab() {
        optionPreferGzippedOutput = new JCheckBox(resources.getString("optionPreferGzippedOutput.text"));
        optionPreferGzippedOutput.setToolTipText(resources.getString("optionPreferGzippedOutput.toolTipText"));

        optionWriteCustomsToXML = new JCheckBox(resources.getString("optionWriteCustomsToXML.text"));
        optionWriteCustomsToXML.setMnemonic(KeyEvent.VK_C);

        optionSaveMothballState = new JCheckBox(resources.getString("optionSaveMothballState.text"));
        optionSaveMothballState.setToolTipText(resources.getString("optionSaveMothballState.toolTipText"));
        optionSaveMothballState.setMnemonic(KeyEvent.VK_U);

        optionSaveCompanyGenerationOptions = new JCheckBox(resources.getString("optionSaveCompanyGenerationOptions.text"));
        optionSaveCompanyGenerationOptions.setToolTipText(resources.getString("optionSaveCompanyGenerationOptions.toolTipText"));
        optionSaveCompanyGenerationOptions.setName("optionSaveCompanyGenerationOptions");

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(optionPreferGzippedOutput)
                        .addComponent(optionWriteCustomsToXML)
                        .addComponent(optionSaveMothballState)
                        .addComponent(optionSaveCompanyGenerationOptions)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionPreferGzippedOutput)
                        .addComponent(optionWriteCustomsToXML)
                        .addComponent(optionSaveMothballState)
                        .addComponent(optionSaveCompanyGenerationOptions)
        );

        return body;
    }

    private JPanel createMiscellaneousTab() {
        JLabel labelStartGameDelay = new JLabel(resources.getString("labelStartGameDelay.text"));
        labelStartGameDelay.setToolTipText(resources.getString("optionStartGameDelay.toolTipText"));

        optionStartGameDelay = new JSpinner(new SpinnerNumberModel(0, 0, 2500, 25));
        optionStartGameDelay.setToolTipText(resources.getString("optionStartGameDelay.toolTipText"));

        JLabel labelDefaultCompanyGenerationType = new JLabel(resources.getString("labelDefaultCompanyGenerationType.text"));
        labelDefaultCompanyGenerationType.setToolTipText(resources.getString("labelDefaultCompanyGenerationType.toolTipText"));
        optionDefaultCompanyGenerationType = new JComboBox<>(CompanyGenerationType.values());
        optionDefaultCompanyGenerationType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected && (index > -1)) {
                    list.setToolTipText((list.getSelectedValue() instanceof CompanyGenerationType)
                            ? ((CompanyGenerationType) list.getSelectedValue()).getToolTipText() : "");
                }
                return this;
            }
        });

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(labelStartGameDelay)
                                .addComponent(optionStartGameDelay, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(labelDefaultCompanyGenerationType)
                                .addComponent(optionDefaultCompanyGenerationType))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelStartGameDelay)
                                .addComponent(optionStartGameDelay))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelDefaultCompanyGenerationType)
                                .addComponent(optionDefaultCompanyGenerationType))
        );

        return body;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        if (validateDateFormat(optionDisplayDateFormat.getText())) {
            MekHQ.getMekHQOptions().setDisplayDateFormat(optionDisplayDateFormat.getText());
        }
        if (validateDateFormat(optionLongDisplayDateFormat.getText())) {
            MekHQ.getMekHQOptions().setLongDisplayDateFormat(optionLongDisplayDateFormat.getText());
        }
        MekHQ.getMekHQOptions().setHistoricalDailyLog(optionHistoricalDailyLog.isSelected());
        MekHQ.getMekHQOptions().setShowCompanyGenerator(optionShowCompanyGenerator.isSelected());
        MekHQ.getMekHQOptions().setCommandCenterUseUnitMarket(optionCommandCenterUseUnitMarket.isSelected());
        MekHQ.getMekHQOptions().setCommandCenterMRMS(optionCommandCenterMRMS.isSelected());
        MekHQ.getMekHQOptions().setPersonnelFilterStyle((PersonnelFilterStyle) Objects.requireNonNull(optionPersonnelFilterStyle.getSelectedItem()));
        MekHQ.getMekHQOptions().setPersonnelFilterOnPrimaryRole(optionPersonnelFilterOnPrimaryRole.isSelected());

        MekHQ.getMekHQOptions().setNoAutosaveValue(optionNoSave.isSelected());
        MekHQ.getMekHQOptions().setAutosaveDailyValue(optionSaveDaily.isSelected());
        MekHQ.getMekHQOptions().setAutosaveWeeklyValue(optionSaveWeekly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveMonthlyValue(optionSaveMonthly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveYearlyValue(optionSaveYearly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveBeforeMissionsValue(checkSaveBeforeMissions.isSelected());
        MekHQ.getMekHQOptions().setMaximumNumberOfAutosavesValue((Integer) spinnerSavedGamesCount.getValue());

        MekHQ.getMekHQOptions().setNewDayMRMS(optionNewDayMRMS.isSelected());

        MekHQ.getMekHQOptions().setPreferGzippedOutput(optionPreferGzippedOutput.isSelected());
        MekHQ.getMekHQOptions().setWriteCustomsToXML(optionWriteCustomsToXML.isSelected());
        MekHQ.getMekHQOptions().setSaveMothballState(optionSaveMothballState.isSelected());
        MekHQ.getMekHQOptions().setSaveCompanyGenerationOptions(optionSaveCompanyGenerationOptions.isSelected());

        MekHQ.getMekHQOptions().setStartGameDelay((Integer) optionStartGameDelay.getValue());
        MekHQ.getMekHQOptions().setDefaultCompanyGenerationType((CompanyGenerationType) Objects.requireNonNull(optionDefaultCompanyGenerationType.getSelectedItem()));

        MekHQ.triggerEvent(new MekHQOptionsChangedEvent());
    }

    private void setInitialState() {
        optionDisplayDateFormat.setText(MekHQ.getMekHQOptions().getDisplayDateFormat());
        optionLongDisplayDateFormat.setText(MekHQ.getMekHQOptions().getLongDisplayDateFormat());
        optionHistoricalDailyLog.setSelected(MekHQ.getMekHQOptions().getHistoricalDailyLog());
        optionShowCompanyGenerator.setSelected(MekHQ.getMekHQOptions().getShowCompanyGenerator());
        optionCommandCenterUseUnitMarket.setSelected(MekHQ.getMekHQOptions().getCommandCenterUseUnitMarket());
        optionCommandCenterMRMS.setSelected(MekHQ.getMekHQOptions().getCommandCenterMRMS());
        optionPersonnelFilterStyle.setSelectedItem(MekHQ.getMekHQOptions().getPersonnelFilterStyle());
        optionPersonnelFilterOnPrimaryRole.setSelected(MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole());

        optionNoSave.setSelected(MekHQ.getMekHQOptions().getNoAutosaveValue());
        optionSaveDaily.setSelected(MekHQ.getMekHQOptions().getAutosaveDailyValue());
        optionSaveWeekly.setSelected(MekHQ.getMekHQOptions().getAutosaveWeeklyValue());
        optionSaveMonthly.setSelected(MekHQ.getMekHQOptions().getAutosaveMonthlyValue());
        optionSaveYearly.setSelected(MekHQ.getMekHQOptions().getAutosaveYearlyValue());
        checkSaveBeforeMissions.setSelected(MekHQ.getMekHQOptions().getAutosaveBeforeMissionsValue());
        spinnerSavedGamesCount.setValue(MekHQ.getMekHQOptions().getMaximumNumberOfAutosavesValue());

        optionNewDayMRMS.setSelected(MekHQ.getMekHQOptions().getNewDayMRMS());

        optionPreferGzippedOutput.setSelected(MekHQ.getMekHQOptions().getPreferGzippedOutput());
        optionWriteCustomsToXML.setSelected(MekHQ.getMekHQOptions().getWriteCustomsToXML());
        optionSaveMothballState.setSelected(MekHQ.getMekHQOptions().getSaveMothballState());
        optionSaveCompanyGenerationOptions.setSelected(MekHQ.getMekHQOptions().getSaveCompanyGenerationOptions());

        optionStartGameDelay.setValue(MekHQ.getMekHQOptions().getStartGameDelay());
        optionDefaultCompanyGenerationType.setSelectedItem(MekHQ.getMekHQOptions().getDefaultCompanyGenerationType());
    }

    //region Data Validation
    private boolean validateDateFormat(String format) {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern(format));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
    //endregion Data Validation
}
