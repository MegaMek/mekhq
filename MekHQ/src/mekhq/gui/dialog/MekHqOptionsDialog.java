/*
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

import megamek.client.ui.swing.ColourSelectorButton;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.event.MekHQOptionsChangedEvent;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.enums.PersonnelFilterStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class MekHqOptionsDialog extends AbstractMHQButtonDialog {
    //region Variable Declaration
    //region Display
    private JTextField optionDisplayDateFormat;
    private JTextField optionLongDisplayDateFormat;
    private JCheckBox optionHistoricalDailyLog;

    //region Command Center Display
    private JCheckBox optionCommandCenterUseUnitMarket;
    private JCheckBox optionCommandCenterMRMS;
    //endregion Command Center Display

    //region Personnel Tab Display Options
    private JComboBox<PersonnelFilterStyle> optionPersonnelFilterStyle;
    private JCheckBox optionPersonnelFilterOnPrimaryRole;
    //endregion Personnel Tab Display Options

    //region Colors
    private ColourSelectorButton optionDeployedForeground;
    private ColourSelectorButton optionDeployedBackground;
    private ColourSelectorButton optionBelowContractMinimumForeground;
    private ColourSelectorButton optionBelowContractMinimumBackground;
    private ColourSelectorButton optionInTransitForeground;
    private ColourSelectorButton optionInTransitBackground;
    private ColourSelectorButton optionRefittingForeground;
    private ColourSelectorButton optionRefittingBackground;
    private ColourSelectorButton optionMothballingForeground;
    private ColourSelectorButton optionMothballingBackground;
    private ColourSelectorButton optionMothballedForeground;
    private ColourSelectorButton optionMothballedBackground;
    private ColourSelectorButton optionNotRepairableForeground;
    private ColourSelectorButton optionNotRepairableBackground;
    private ColourSelectorButton optionNonFunctionalForeground;
    private ColourSelectorButton optionNonFunctionalBackground;
    private ColourSelectorButton optionNeedsPartsFixedForeground;
    private ColourSelectorButton optionNeedsPartsFixedBackground;
    private ColourSelectorButton optionUnmaintainedForeground;
    private ColourSelectorButton optionUnmaintainedBackground;
    private ColourSelectorButton optionUncrewedForeground;
    private ColourSelectorButton optionUncrewedBackground;
    private ColourSelectorButton optionLoanOverdueForeground;
    private ColourSelectorButton optionLoanOverdueBackground;
    private ColourSelectorButton optionInjuredForeground;
    private ColourSelectorButton optionInjuredBackground;
    private ColourSelectorButton optionHealedInjuriesForeground;
    private ColourSelectorButton optionHealedInjuriesBackground;
    private ColourSelectorButton optionPaidRetirementForeground;
    private ColourSelectorButton optionPaidRetirementBackground;
    //endregion Colors
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
    private JCheckBox optionNewDayAstechPoolFill;
    private JCheckBox optionNewDayMedicPoolFill;
    private JCheckBox optionNewDayMRMS;
    //endregion New Day

    //region Campaign XML Save
    private JCheckBox optionPreferGzippedOutput;
    private JCheckBox optionWriteCustomsToXML;
    private JCheckBox optionSaveMothballState;
    //endregion Campaign XML Save

    //region Nag Tab
    private JCheckBox optionUnmaintainedUnitsNag;
    private JCheckBox optionInsufficientAstechsNag;
    private JCheckBox optionInsufficientAstechTimeNag;
    private JCheckBox optionInsufficientMedicsNag;
    private JCheckBox optionShortDeploymentNag;
    private JCheckBox optionUnresolvedStratConContactsNag;
    private JCheckBox optionOutstandingScenariosNag;
    //endregion Nag Tab

    //region Miscellaneous
    private JSpinner optionStartGameDelay;
    //endregion Miscellaneous
    //endregion Variable Declaration

    //region Constructors
    public MekHqOptionsDialog(final JFrame frame) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.MekHqOptionsDialog",
                new EncodeControl()), "MekHQOptionsDialog", "MekHQOptionsDialog.title");
        initialize();
        setInitialState();
    }
    //endregion Constructors

    //region Initialization
    /**
     * This dialog uses the following Mnemonics:
     * C, D, M, M, S, U, W, Y
     */
    @Override
    protected Container createCenterPane() {
        JTabbedPane optionsTabbedPane = new JTabbedPane();
        optionsTabbedPane.setName("optionsTabbedPane");
        optionsTabbedPane.add(resources.getString("displayTab.title"), new JScrollPane(createDisplayTab()));
        optionsTabbedPane.add(resources.getString("displayColourTab.title"), new JScrollPane(createDisplayColourTab()));
        optionsTabbedPane.add(resources.getString("autosaveTab.title"), new JScrollPane(createAutosaveTab()));
        optionsTabbedPane.add(resources.getString("newDayTab.title"), new JScrollPane(createNewDayTab()));
        optionsTabbedPane.add(resources.getString("campaignXMLSaveTab.title"), new JScrollPane(createCampaignXMLSaveTab()));
        optionsTabbedPane.add(resources.getString("nagTab.title"), new JScrollPane(createNagTab()));
        optionsTabbedPane.add(resources.getString("miscellaneousTab.title"), new JScrollPane(createMiscellaneousTab()));

        return optionsTabbedPane;
    }

    private JPanel createDisplayTab() {
        //region Create Graphical Segments
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
            private static final long serialVersionUID = -543354619818226314L;

            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PersonnelFilterStyle) {
                    list.setToolTipText(((PersonnelFilterStyle) value).getToolTipText());
                }
                return this;
            }
        });

        optionPersonnelFilterOnPrimaryRole = new JCheckBox(resources.getString("optionPersonnelFilterOnPrimaryRole.text"));
        //endregion Personnel Tab Display Options
        //endregion Create Graphical Components

        //region Layout
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
                        .addComponent(labelCommandCenterDisplay)
                        .addComponent(optionCommandCenterUseUnitMarket)
                        .addComponent(optionCommandCenterMRMS)
                        .addComponent(labelPersonnelDisplay)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelPersonnelFilterStyle)
                                .addComponent(optionPersonnelFilterStyle))
                        .addComponent(optionPersonnelFilterOnPrimaryRole)
        );
        //endregion Layout

        return body;
    }

    private JPanel createDisplayColourTab() {
        //region Create Graphical Components
        optionDeployedForeground = new ColourSelectorButton(resources.getString("optionDeployedForeground.text"));

        optionDeployedBackground = new ColourSelectorButton(resources.getString("optionDeployedBackground.text"));

        optionBelowContractMinimumForeground = new ColourSelectorButton(resources.getString("optionBelowContractMinimumForeground.text"));

        optionBelowContractMinimumBackground = new ColourSelectorButton(resources.getString("optionBelowContractMinimumBackground.text"));

        optionInTransitForeground = new ColourSelectorButton(resources.getString("optionInTransitForeground.text"));

        optionInTransitBackground = new ColourSelectorButton(resources.getString("optionInTransitBackground.text"));

        optionRefittingForeground = new ColourSelectorButton(resources.getString("optionRefittingForeground.text"));

        optionRefittingBackground = new ColourSelectorButton(resources.getString("optionRefittingBackground.text"));

        optionMothballingForeground = new ColourSelectorButton(resources.getString("optionMothballingForeground.text"));

        optionMothballingBackground = new ColourSelectorButton(resources.getString("optionMothballingBackground.text"));

        optionMothballedForeground = new ColourSelectorButton(resources.getString("optionMothballedForeground.text"));

        optionMothballedBackground = new ColourSelectorButton(resources.getString("optionMothballedBackground.text"));

        optionNotRepairableForeground = new ColourSelectorButton(resources.getString("optionNotRepairableForeground.text"));

        optionNotRepairableBackground = new ColourSelectorButton(resources.getString("optionNotRepairableBackground.text"));

        optionNonFunctionalForeground = new ColourSelectorButton(resources.getString("optionNonFunctionalForeground.text"));

        optionNonFunctionalBackground = new ColourSelectorButton(resources.getString("optionNonFunctionalBackground.text"));

        optionNeedsPartsFixedForeground = new ColourSelectorButton(resources.getString("optionNeedsPartsFixedForeground.text"));

        optionNeedsPartsFixedBackground = new ColourSelectorButton(resources.getString("optionNeedsPartsFixedBackground.text"));

        optionUnmaintainedForeground = new ColourSelectorButton(resources.getString("optionUnmaintainedForeground.text"));

        optionUnmaintainedBackground = new ColourSelectorButton(resources.getString("optionUnmaintainedBackground.text"));

        optionUncrewedForeground = new ColourSelectorButton(resources.getString("optionUncrewedForeground.text"));

        optionUncrewedBackground = new ColourSelectorButton(resources.getString("optionUncrewedBackground.text"));

        optionLoanOverdueForeground = new ColourSelectorButton(resources.getString("optionLoanOverdueForeground.text"));

        optionLoanOverdueBackground = new ColourSelectorButton(resources.getString("optionLoanOverdueBackground.text"));

        optionInjuredForeground = new ColourSelectorButton(resources.getString("optionInjuredForeground.text"));

        optionInjuredBackground = new ColourSelectorButton(resources.getString("optionInjuredBackground.text"));

        optionHealedInjuriesForeground = new ColourSelectorButton(resources.getString("optionHealedInjuriesForeground.text"));

        optionHealedInjuriesBackground = new ColourSelectorButton(resources.getString("optionHealedInjuriesBackground.text"));

        optionPaidRetirementForeground = new ColourSelectorButton(resources.getString("optionPaidRetirementForeground.text"));

        optionPaidRetirementBackground = new ColourSelectorButton(resources.getString("optionPaidRetirementBackground.text"));
        //endregion Create Graphical Components

        //region Layout
        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionDeployedForeground)
                                .addComponent(optionDeployedBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionBelowContractMinimumForeground)
                                .addComponent(optionBelowContractMinimumBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionInTransitForeground)
                                .addComponent(optionInTransitBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionRefittingForeground)
                                .addComponent(optionRefittingBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionMothballingForeground)
                                .addComponent(optionMothballingBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionMothballedForeground)
                                .addComponent(optionMothballedBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionNotRepairableForeground)
                                .addComponent(optionNotRepairableBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionNonFunctionalForeground)
                                .addComponent(optionNonFunctionalBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionNeedsPartsFixedForeground)
                                .addComponent(optionNeedsPartsFixedBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionUnmaintainedForeground)
                                .addComponent(optionUnmaintainedBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionUncrewedForeground)
                                .addComponent(optionUncrewedBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionLoanOverdueForeground)
                                .addComponent(optionLoanOverdueBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionInjuredForeground)
                                .addComponent(optionInjuredBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionHealedInjuriesForeground)
                                .addComponent(optionHealedInjuriesBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionPaidRetirementForeground)
                                .addComponent(optionPaidRetirementBackground, GroupLayout.Alignment.TRAILING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionDeployedForeground)
                                .addComponent(optionDeployedBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionBelowContractMinimumForeground)
                                .addComponent(optionBelowContractMinimumBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionInTransitForeground)
                                .addComponent(optionInTransitBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionRefittingForeground)
                                .addComponent(optionRefittingBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionMothballingForeground)
                                .addComponent(optionMothballingBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionMothballedForeground)
                                .addComponent(optionMothballedBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionNotRepairableForeground)
                                .addComponent(optionNotRepairableBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionNonFunctionalForeground)
                                .addComponent(optionNonFunctionalBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionNeedsPartsFixedForeground)
                                .addComponent(optionNeedsPartsFixedBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionUnmaintainedForeground)
                                .addComponent(optionUnmaintainedBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionUncrewedForeground)
                                .addComponent(optionUncrewedBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionLoanOverdueForeground)
                                .addComponent(optionLoanOverdueBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionInjuredForeground)
                                .addComponent(optionInjuredBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionHealedInjuriesForeground)
                                .addComponent(optionHealedInjuriesBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionPaidRetirementForeground)
                                .addComponent(optionPaidRetirementBackground))
        );
        //endregion Layout

        return body;
    }

    private JPanel createAutosaveTab() {
        //region Create Graphical Components
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
        //endregion Create Graphical Components

        //region Layout
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
        //endregion Layout

        return body;
    }

    private JPanel createNewDayTab() {
        // Create Panel Components
        optionNewDayAstechPoolFill = new JCheckBox(resources.getString("optionNewDayAstechPoolFill.text"));
        optionNewDayAstechPoolFill.setToolTipText(resources.getString("optionNewDayAstechPoolFill.toolTipText"));
        optionNewDayAstechPoolFill.setName("optionNewDayAstechPoolFill");

        optionNewDayMedicPoolFill = new JCheckBox(resources.getString("optionNewDayMedicPoolFill.text"));
        optionNewDayMedicPoolFill.setToolTipText(resources.getString("optionNewDayMedicPoolFill.toolTipText"));
        optionNewDayMedicPoolFill.setName("optionNewDayMedicPoolFill");

        optionNewDayMRMS = new JCheckBox(resources.getString("optionNewDayMRMS.text"));
        optionNewDayMRMS.setToolTipText(resources.getString("optionNewDayMRMS.toolTipText"));
        optionNewDayMRMS.setName("optionNewDayMRMS");

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("newDayPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(optionNewDayAstechPoolFill)
                        .addComponent(optionNewDayMedicPoolFill)
                        .addComponent(optionNewDayMRMS)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionNewDayAstechPoolFill)
                        .addComponent(optionNewDayMedicPoolFill)
                        .addComponent(optionNewDayMRMS)
        );
        //endregion Layout

        return panel;
    }

    private JPanel createCampaignXMLSaveTab() {
        //region Create Graphical Components
        optionPreferGzippedOutput = new JCheckBox(resources.getString("optionPreferGzippedOutput.text"));
        optionPreferGzippedOutput.setToolTipText(resources.getString("optionPreferGzippedOutput.toolTipText"));

        optionWriteCustomsToXML = new JCheckBox(resources.getString("optionWriteCustomsToXML.text"));
        optionWriteCustomsToXML.setMnemonic(KeyEvent.VK_C);

        optionSaveMothballState = new JCheckBox(resources.getString("optionSaveMothballState.text"));
        optionSaveMothballState.setToolTipText(resources.getString("optionSaveMothballState.toolTipText"));
        optionSaveMothballState.setMnemonic(KeyEvent.VK_U);
        //endregion Create Graphical Components

        //region Layout
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
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionPreferGzippedOutput)
                        .addComponent(optionWriteCustomsToXML)
                        .addComponent(optionSaveMothballState)
        );
        //endregion Layout

        return body;
    }

    private JPanel createNagTab() {
        // Create Panel Components
        optionUnmaintainedUnitsNag = new JCheckBox(resources.getString("optionUnmaintainedUnitsNag.text"));
        optionUnmaintainedUnitsNag.setToolTipText(resources.getString("optionUnmaintainedUnitsNag.toolTipText"));
        optionUnmaintainedUnitsNag.setName("optionUnmaintainedUnitsNag");

        optionInsufficientAstechsNag = new JCheckBox(resources.getString("optionInsufficientAstechsNag.text"));
        optionInsufficientAstechsNag.setToolTipText(resources.getString("optionInsufficientAstechsNag.toolTipText"));
        optionInsufficientAstechsNag.setName("optionInsufficientAstechsNag");

        optionInsufficientAstechTimeNag = new JCheckBox(resources.getString("optionInsufficientAstechTimeNag.text"));
        optionInsufficientAstechTimeNag.setToolTipText(resources.getString("optionInsufficientAstechTimeNag.toolTipText"));
        optionInsufficientAstechTimeNag.setName("optionInsufficientAstechTimeNag");

        optionInsufficientMedicsNag = new JCheckBox(resources.getString("optionInsufficientMedicsNag.text"));
        optionInsufficientMedicsNag.setToolTipText(resources.getString("optionInsufficientMedicsNag.toolTipText"));
        optionInsufficientMedicsNag.setName("optionInsufficientMedicsNag");

        optionShortDeploymentNag = new JCheckBox(resources.getString("optionShortDeploymentNag.text"));
        optionShortDeploymentNag.setToolTipText(resources.getString("optionShortDeploymentNag.toolTipText"));
        optionShortDeploymentNag.setName("optionShortDeploymentNag");

        optionUnresolvedStratConContactsNag = new JCheckBox(resources.getString("optionUnresolvedStratConContactsNag.text"));
        optionUnresolvedStratConContactsNag.setToolTipText(resources.getString("optionUnresolvedStratConContactsNag.toolTipText"));
        optionUnresolvedStratConContactsNag.setName("optionUnresolvedStratConContactsNag");

        optionOutstandingScenariosNag = new JCheckBox(resources.getString("optionOutstandingScenariosNag.text"));
        optionOutstandingScenariosNag.setToolTipText(resources.getString("optionOutstandingScenariosNag.toolTipText"));
        optionOutstandingScenariosNag.setName("optionOutstandingScenariosNag");

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("nagPanel");
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(optionUnmaintainedUnitsNag)
                        .addComponent(optionInsufficientAstechsNag)
                        .addComponent(optionInsufficientAstechTimeNag)
                        .addComponent(optionInsufficientMedicsNag)
                        .addComponent(optionShortDeploymentNag)
                        .addComponent(optionUnresolvedStratConContactsNag)
                        .addComponent(optionOutstandingScenariosNag)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionUnmaintainedUnitsNag)
                        .addComponent(optionInsufficientAstechsNag)
                        .addComponent(optionInsufficientAstechTimeNag)
                        .addComponent(optionInsufficientMedicsNag)
                        .addComponent(optionShortDeploymentNag)
                        .addComponent(optionUnresolvedStratConContactsNag)
                        .addComponent(optionOutstandingScenariosNag)
        );

        return panel;
    }

    private JPanel createMiscellaneousTab() {
        //region Create Graphical Components
        JLabel labelStartGameDelay = new JLabel(resources.getString("labelStartGameDelay.text"));
        labelStartGameDelay.setToolTipText(resources.getString("optionStartGameDelay.toolTipText"));

        optionStartGameDelay = new JSpinner(new SpinnerNumberModel(0, 0, 2500, 25));
        optionStartGameDelay.setToolTipText(resources.getString("optionStartGameDelay.toolTipText"));
        //endregion Create Graphical Components

        //region Layout
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
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelStartGameDelay)
                                .addComponent(optionStartGameDelay))
        );
        //endregion Layout

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
        MekHQ.getMekHQOptions().setCommandCenterUseUnitMarket(optionCommandCenterUseUnitMarket.isSelected());
        MekHQ.getMekHQOptions().setCommandCenterMRMS(optionCommandCenterMRMS.isSelected());
        MekHQ.getMekHQOptions().setPersonnelFilterStyle((PersonnelFilterStyle) Objects.requireNonNull(optionPersonnelFilterStyle.getSelectedItem()));
        MekHQ.getMekHQOptions().setPersonnelFilterOnPrimaryRole(optionPersonnelFilterOnPrimaryRole.isSelected());

        MekHQ.getMekHQOptions().setDeployedForeground(optionDeployedForeground.getColour());
        MekHQ.getMekHQOptions().setDeployedBackground(optionDeployedBackground.getColour());
        MekHQ.getMekHQOptions().setBelowContractMinimumForeground(optionBelowContractMinimumForeground.getColour());
        MekHQ.getMekHQOptions().setBelowContractMinimumBackground(optionBelowContractMinimumBackground.getColour());
        MekHQ.getMekHQOptions().setInTransitForeground(optionInTransitForeground.getColour());
        MekHQ.getMekHQOptions().setInTransitBackground(optionInTransitBackground.getColour());
        MekHQ.getMekHQOptions().setRefittingForeground(optionRefittingForeground.getColour());
        MekHQ.getMekHQOptions().setRefittingBackground(optionRefittingBackground.getColour());
        MekHQ.getMekHQOptions().setMothballingForeground(optionMothballingForeground.getColour());
        MekHQ.getMekHQOptions().setMothballingBackground(optionMothballingBackground.getColour());
        MekHQ.getMekHQOptions().setMothballedForeground(optionMothballedForeground.getColour());
        MekHQ.getMekHQOptions().setMothballedBackground(optionMothballedBackground.getColour());
        MekHQ.getMekHQOptions().setNotRepairableForeground(optionNotRepairableForeground.getColour());
        MekHQ.getMekHQOptions().setNotRepairableBackground(optionNotRepairableBackground.getColour());
        MekHQ.getMekHQOptions().setNonFunctionalForeground(optionNonFunctionalForeground.getColour());
        MekHQ.getMekHQOptions().setNonFunctionalBackground(optionNonFunctionalBackground.getColour());
        MekHQ.getMekHQOptions().setNeedsPartsFixedForeground(optionNeedsPartsFixedForeground.getColour());
        MekHQ.getMekHQOptions().setNeedsPartsFixedBackground(optionNeedsPartsFixedBackground.getColour());
        MekHQ.getMekHQOptions().setUnmaintainedForeground(optionUnmaintainedForeground.getColour());
        MekHQ.getMekHQOptions().setUnmaintainedBackground(optionUnmaintainedBackground.getColour());
        MekHQ.getMekHQOptions().setUncrewedForeground(optionUncrewedForeground.getColour());
        MekHQ.getMekHQOptions().setUncrewedBackground(optionUncrewedBackground.getColour());
        MekHQ.getMekHQOptions().setLoanOverdueForeground(optionLoanOverdueForeground.getColour());
        MekHQ.getMekHQOptions().setLoanOverdueBackground(optionLoanOverdueBackground.getColour());
        MekHQ.getMekHQOptions().setInjuredForeground(optionInjuredForeground.getColour());
        MekHQ.getMekHQOptions().setInjuredBackground(optionInjuredBackground.getColour());
        MekHQ.getMekHQOptions().setHealedInjuriesForeground(optionHealedInjuriesForeground.getColour());
        MekHQ.getMekHQOptions().setHealedInjuriesBackground(optionHealedInjuriesBackground.getColour());
        MekHQ.getMekHQOptions().setPaidRetirementForeground(optionPaidRetirementForeground.getColour());
        MekHQ.getMekHQOptions().setPaidRetirementBackground(optionPaidRetirementBackground.getColour());

        MekHQ.getMekHQOptions().setNoAutosaveValue(optionNoSave.isSelected());
        MekHQ.getMekHQOptions().setAutosaveDailyValue(optionSaveDaily.isSelected());
        MekHQ.getMekHQOptions().setAutosaveWeeklyValue(optionSaveWeekly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveMonthlyValue(optionSaveMonthly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveYearlyValue(optionSaveYearly.isSelected());
        MekHQ.getMekHQOptions().setAutosaveBeforeMissionsValue(checkSaveBeforeMissions.isSelected());
        MekHQ.getMekHQOptions().setMaximumNumberOfAutosavesValue((Integer) spinnerSavedGamesCount.getValue());

        MekHQ.getMekHQOptions().setNewDayAstechPoolFill(optionNewDayAstechPoolFill.isSelected());
        MekHQ.getMekHQOptions().setNewDayMedicPoolFill(optionNewDayMedicPoolFill.isSelected());
        MekHQ.getMekHQOptions().setNewDayMRMS(optionNewDayMRMS.isSelected());

        MekHQ.getMekHQOptions().setPreferGzippedOutput(optionPreferGzippedOutput.isSelected());
        MekHQ.getMekHQOptions().setWriteCustomsToXML(optionWriteCustomsToXML.isSelected());
        MekHQ.getMekHQOptions().setSaveMothballState(optionSaveMothballState.isSelected());

        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_UNMAINTAINED_UNITS, optionUnmaintainedUnitsNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_ASTECHS, optionInsufficientAstechsNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_ASTECH_TIME, optionInsufficientAstechTimeNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_MEDICS, optionInsufficientMedicsNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_SHORT_DEPLOYMENT, optionShortDeploymentNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_UNRESOLVED_STRATCON_CONTACTS, optionUnresolvedStratConContactsNag.isSelected());
        MekHQ.getMekHQOptions().setNagDialogIgnore(MekHqConstants.NAG_OUTSTANDING_SCENARIOS, optionOutstandingScenariosNag.isSelected());

        MekHQ.getMekHQOptions().setStartGameDelay((Integer) optionStartGameDelay.getValue());

        MekHQ.triggerEvent(new MekHQOptionsChangedEvent());
    }

    private void setInitialState() {
        optionDisplayDateFormat.setText(MekHQ.getMekHQOptions().getDisplayDateFormat());
        optionLongDisplayDateFormat.setText(MekHQ.getMekHQOptions().getLongDisplayDateFormat());
        optionHistoricalDailyLog.setSelected(MekHQ.getMekHQOptions().getHistoricalDailyLog());
        optionCommandCenterUseUnitMarket.setSelected(MekHQ.getMekHQOptions().getCommandCenterUseUnitMarket());
        optionCommandCenterMRMS.setSelected(MekHQ.getMekHQOptions().getCommandCenterMRMS());
        optionPersonnelFilterStyle.setSelectedItem(MekHQ.getMekHQOptions().getPersonnelFilterStyle());
        optionPersonnelFilterOnPrimaryRole.setSelected(MekHQ.getMekHQOptions().getPersonnelFilterOnPrimaryRole());

        optionDeployedForeground.setColour(MekHQ.getMekHQOptions().getDeployedForeground());
        optionDeployedBackground.setColour(MekHQ.getMekHQOptions().getDeployedBackground());
        optionBelowContractMinimumForeground.setColour(MekHQ.getMekHQOptions().getBelowContractMinimumForeground());
        optionBelowContractMinimumBackground.setColour(MekHQ.getMekHQOptions().getBelowContractMinimumBackground());
        optionInTransitForeground.setColour(MekHQ.getMekHQOptions().getInTransitForeground());
        optionInTransitBackground.setColour(MekHQ.getMekHQOptions().getInTransitBackground());
        optionRefittingForeground.setColour(MekHQ.getMekHQOptions().getRefittingForeground());
        optionRefittingBackground.setColour(MekHQ.getMekHQOptions().getRefittingBackground());
        optionMothballingForeground.setColour(MekHQ.getMekHQOptions().getMothballingForeground());
        optionMothballingBackground.setColour(MekHQ.getMekHQOptions().getMothballingBackground());
        optionMothballedForeground.setColour(MekHQ.getMekHQOptions().getMothballedForeground());
        optionMothballedBackground.setColour(MekHQ.getMekHQOptions().getMothballedBackground());
        optionNotRepairableForeground.setColour(MekHQ.getMekHQOptions().getNotRepairableForeground());
        optionNotRepairableBackground.setColour(MekHQ.getMekHQOptions().getNotRepairableBackground());
        optionNonFunctionalForeground.setColour(MekHQ.getMekHQOptions().getNonFunctionalForeground());
        optionNonFunctionalBackground.setColour(MekHQ.getMekHQOptions().getNonFunctionalBackground());
        optionNeedsPartsFixedForeground.setColour(MekHQ.getMekHQOptions().getNeedsPartsFixedForeground());
        optionNeedsPartsFixedBackground.setColour(MekHQ.getMekHQOptions().getNeedsPartsFixedBackground());
        optionUnmaintainedForeground.setColour(MekHQ.getMekHQOptions().getUnmaintainedForeground());
        optionUnmaintainedBackground.setColour(MekHQ.getMekHQOptions().getUnmaintainedBackground());
        optionUncrewedForeground.setColour(MekHQ.getMekHQOptions().getUncrewedForeground());
        optionUncrewedBackground.setColour(MekHQ.getMekHQOptions().getUncrewedBackground());
        optionLoanOverdueForeground.setColour(MekHQ.getMekHQOptions().getLoanOverdueForeground());
        optionLoanOverdueBackground.setColour(MekHQ.getMekHQOptions().getLoanOverdueBackground());
        optionInjuredForeground.setColour(MekHQ.getMekHQOptions().getInjuredForeground());
        optionInjuredBackground.setColour(MekHQ.getMekHQOptions().getInjuredBackground());
        optionHealedInjuriesForeground.setColour(MekHQ.getMekHQOptions().getHealedInjuriesForeground());
        optionHealedInjuriesBackground.setColour(MekHQ.getMekHQOptions().getHealedInjuriesBackground());
        optionPaidRetirementForeground.setColour(MekHQ.getMekHQOptions().getPaidRetirementForeground());
        optionPaidRetirementBackground.setColour(MekHQ.getMekHQOptions().getPaidRetirementBackground());

        optionNoSave.setSelected(MekHQ.getMekHQOptions().getNoAutosaveValue());
        optionSaveDaily.setSelected(MekHQ.getMekHQOptions().getAutosaveDailyValue());
        optionSaveWeekly.setSelected(MekHQ.getMekHQOptions().getAutosaveWeeklyValue());
        optionSaveMonthly.setSelected(MekHQ.getMekHQOptions().getAutosaveMonthlyValue());
        optionSaveYearly.setSelected(MekHQ.getMekHQOptions().getAutosaveYearlyValue());
        checkSaveBeforeMissions.setSelected(MekHQ.getMekHQOptions().getAutosaveBeforeMissionsValue());
        spinnerSavedGamesCount.setValue(MekHQ.getMekHQOptions().getMaximumNumberOfAutosavesValue());

        optionNewDayAstechPoolFill.setSelected(MekHQ.getMekHQOptions().getNewDayAstechPoolFill());
        optionNewDayMedicPoolFill.setSelected(MekHQ.getMekHQOptions().getNewDayMedicPoolFill());
        optionNewDayMRMS.setSelected(MekHQ.getMekHQOptions().getNewDayMRMS());

        optionPreferGzippedOutput.setSelected(MekHQ.getMekHQOptions().getPreferGzippedOutput());
        optionWriteCustomsToXML.setSelected(MekHQ.getMekHQOptions().getWriteCustomsToXML());
        optionSaveMothballState.setSelected(MekHQ.getMekHQOptions().getSaveMothballState());

        optionUnmaintainedUnitsNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_UNMAINTAINED_UNITS));
        optionInsufficientAstechsNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_ASTECHS));
        optionInsufficientAstechTimeNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_ASTECH_TIME));
        optionInsufficientMedicsNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_INSUFFICIENT_MEDICS));
        optionShortDeploymentNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_SHORT_DEPLOYMENT));
        optionUnresolvedStratConContactsNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_UNRESOLVED_STRATCON_CONTACTS));
        optionOutstandingScenariosNag.setSelected(MekHQ.getMekHQOptions().getNagDialogIgnore(MekHqConstants.NAG_OUTSTANDING_SCENARIOS));

        optionStartGameDelay.setValue(MekHQ.getMekHQOptions().getStartGameDelay());
    }

    //region Data Validation
    private boolean validateDateFormat(final String format) {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern(format));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
    //endregion Data Validation
}
