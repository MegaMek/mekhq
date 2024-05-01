/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.comboBoxes.FontComboBox;
import megamek.client.ui.displayWrappers.FontDisplay;
import megamek.client.ui.swing.ColourSelectorButton;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.client.ui.swing.HelpDialog;
import megamek.common.preference.PreferenceManager;
import mekhq.MHQConstants;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MekHQ;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.enums.ForceIconOperationalStatusStyle;
import mekhq.gui.enums.PersonnelFilterStyle;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MHQOptionsDialog extends AbstractMHQButtonDialog {
    //region Variable Declaration
    //region Display
    private JTextField optionDisplayDateFormat;
    private JTextField optionLongDisplayDateFormat;
    private JCheckBox optionHistoricalDailyLog;
    private JCheckBox chkCompanyGeneratorStartup;
    private JCheckBox chkShowCompanyGenerator;

    //region Command Center Tab
    private JCheckBox optionCommandCenterUseUnitMarket;
    private JCheckBox optionCommandCenterMRMS;
    //endregion Command Center Tab

    //region Interstellar Map Tab
    private JCheckBox chkInterstellarMapShowJumpRadius;
    private JSpinner spnInterstellarMapShowJumpRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapJumpRadiusColour;
    private JCheckBox chkInterstellarMapShowPlanetaryAcquisitionRadius;
    private JSpinner spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapPlanetaryAcquisitionRadiusColour;
    private JCheckBox chkInterstellarMapShowContractSearchRadius;
    private ColourSelectorButton btnInterstellarMapContractSearchRadiusColour;
    //endregion Interstellar Map Tab

    //region Personnel Tab
    private JComboBox<PersonnelFilterStyle> optionPersonnelFilterStyle;
    private JCheckBox optionPersonnelFilterOnPrimaryRole;
    //endregion Personnel Tab
    //endregion Display

    //region Colours
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
    private ColourSelectorButton optionPregnantForeground;
    private ColourSelectorButton optionPregnantBackground;
    private ColourSelectorButton optionPaidRetirementForeground;
    private ColourSelectorButton optionPaidRetirementBackground;
    private ColourSelectorButton optionStratConHexCoordForeground;
    //endregion Colors

    //region Fonts
    private FontComboBox comboMedicalViewDialogHandwritingFont;
    //endregion Fonts

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
    private JCheckBox chkNewDayAstechPoolFill;
    private JCheckBox chkNewDayMedicPoolFill;
    private JCheckBox chkNewDayMRMS;
    private JCheckBox chkNewDayForceIconOperationalStatus;
    private MMComboBox<ForceIconOperationalStatusStyle> comboNewDayForceIconOperationalStatusStyle;
    //endregion New Day

    //region Campaign XML Save
    private JCheckBox optionPreferGzippedOutput;
    private JCheckBox optionWriteCustomsToXML;
    private JCheckBox optionSaveMothballState;
    //endregion Campaign XML Save

    //region Nag Tab
    private JCheckBox optionUnmaintainedUnitsNag;
    private JCheckBox optionPregnantCombatantNag;
    private JCheckBox optionPrisonersNag;
    private JCheckBox optionUntreatedPersonnelNag;
    private JCheckBox optionInsufficientAstechsNag;
    private JCheckBox optionInsufficientAstechTimeNag;
    private JCheckBox optionInsufficientMedicsNag;
    private JCheckBox optionShortDeploymentNag;
    private JCheckBox optionUnresolvedStratConContactsNag;
    private JCheckBox optionOutstandingScenariosNag;
    private JCheckBox optionCargoCapacityNag;
    //endregion Nag Tab

    //region Miscellaneous
    private JTextField txtUserDir;
    private JSpinner spnStartGameDelay;
    private JSpinner spnStartGameClientDelay;
    private JSpinner spnStartGameClientRetryCount;
    private JSpinner spnStartGameBotClientDelay;
    private JSpinner spnStartGameBotClientRetryCount;
    private MMComboBox<CompanyGenerationMethod> comboDefaultCompanyGenerationMethod;
    //endregion Miscellaneous
    //endregion Variable Declarations

    //region Constructors
    public MHQOptionsDialog(final JFrame frame) {
        super(frame, true, "MHQOptionsDialog", "MHQOptionsDialog.title");
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
        optionsTabbedPane.add(resources.getString("coloursTab.title"), new JScrollPane(createColoursTab()));
        optionsTabbedPane.add(resources.getString("fontsTab.title"), new JScrollPane(createFontsTab()));
        optionsTabbedPane.add(resources.getString("autosaveTab.title"), new JScrollPane(createAutosaveTab()));
        optionsTabbedPane.add(resources.getString("newDayTab.title"), new JScrollPane(createNewDayTab()));
        optionsTabbedPane.add(resources.getString("campaignXMLSaveTab.title"), new JScrollPane(createCampaignXMLSaveTab()));
        optionsTabbedPane.add(resources.getString("nagTab.title"), new JScrollPane(createNagTab()));
        optionsTabbedPane.add(resources.getString("miscellaneousTab.title"), new JScrollPane(createMiscellaneousTab()));
        return optionsTabbedPane;
    }

    private JPanel createDisplayTab() {
        // Initialize Components Used in ActionListeners
        final JLabel lblInterstellarMapShowJumpRadiusMinimumZoom = new JLabel();
        final JLabel lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom = new JLabel();

        // Create Panel Components
        JLabel labelDisplayDateFormat = new JLabel(resources.getString("labelDisplayDateFormat.text"));
        JLabel labelDisplayDateFormatExample = new JLabel();
        optionDisplayDateFormat = new JTextField();
        optionDisplayDateFormat.addActionListener(evt -> labelDisplayDateFormatExample.setText(
                validateDateFormat(optionDisplayDateFormat.getText())
                        ? LocalDate.now().format(DateTimeFormatter.ofPattern(optionDisplayDateFormat.getText())
                                .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                        : resources.getString("invalidDateFormat.error")));

        JLabel labelLongDisplayDateFormat = new JLabel(resources.getString("labelLongDisplayDateFormat.text"));
        JLabel labelLongDisplayDateFormatExample = new JLabel();
        optionLongDisplayDateFormat = new JTextField();
        optionLongDisplayDateFormat.addActionListener(evt -> labelLongDisplayDateFormatExample.setText(
                validateDateFormat(optionLongDisplayDateFormat.getText())
                        ? LocalDate.now().format(DateTimeFormatter.ofPattern(optionLongDisplayDateFormat.getText())
                                .withLocale(MekHQ.getMHQOptions().getDateLocale()))
                        : resources.getString("invalidDateFormat.error")));

        optionHistoricalDailyLog = new JCheckBox(resources.getString("optionHistoricalDailyLog.text"));
        optionHistoricalDailyLog.setToolTipText(resources.getString("optionHistoricalDailyLog.toolTipText"));

        chkCompanyGeneratorStartup = new JCheckBox(resources.getString("chkCompanyGeneratorStartup.text"));
        chkCompanyGeneratorStartup.setToolTipText(resources.getString("chkCompanyGeneratorStartup.toolTipText"));
        chkCompanyGeneratorStartup.setName("chkCompanyGeneratorStartup");

        chkShowCompanyGenerator = new JCheckBox(resources.getString("chkShowCompanyGenerator.text"));
        chkShowCompanyGenerator.setToolTipText(resources.getString("chkShowCompanyGenerator.toolTipText"));
        chkShowCompanyGenerator.setName("chkShowCompanyGenerator");

        //region Command Center Tab
        JLabel labelCommandCenterDisplay = new JLabel(resources.getString("labelCommandCenterDisplay.text"));

        optionCommandCenterUseUnitMarket = new JCheckBox(resources.getString("optionCommandCenterUseUnitMarket.text"));
        optionCommandCenterUseUnitMarket.setToolTipText(resources.getString("optionCommandCenterUseUnitMarket.toolTipText"));

        optionCommandCenterMRMS = new JCheckBox(resources.getString("optionCommandCenterMRMS.text"));
        optionCommandCenterMRMS.setToolTipText(resources.getString("optionCommandCenterMRMS.toolTipText"));
        //endregion Command Center Tab

        //region Interstellar Map Tab
        final JLabel lblInterstellarMapTab = new JLabel(resources.getString("lblInterstellarMapTab.text"));
        lblInterstellarMapTab.setName("lblInterstellarMapTab");

        chkInterstellarMapShowJumpRadius = new JCheckBox(resources.getString("chkInterstellarMapShowJumpRadius.text"));
        chkInterstellarMapShowJumpRadius.setToolTipText(resources.getString("chkInterstellarMapShowJumpRadius.toolTipText"));
        chkInterstellarMapShowJumpRadius.setName("chkInterstellarMapShowJumpRadius");
        chkInterstellarMapShowJumpRadius.addActionListener(evt -> {
            final boolean selected = chkInterstellarMapShowJumpRadius.isSelected();
            lblInterstellarMapShowJumpRadiusMinimumZoom.setEnabled(selected);
            spnInterstellarMapShowJumpRadiusMinimumZoom.setEnabled(selected);
            btnInterstellarMapJumpRadiusColour.setEnabled(selected);
        });

        lblInterstellarMapShowJumpRadiusMinimumZoom.setText(resources.getString("lblInterstellarMapShowJumpRadiusMinimumZoom.text"));
        lblInterstellarMapShowJumpRadiusMinimumZoom.setToolTipText(resources.getString("lblInterstellarMapShowJumpRadiusMinimumZoom.toolTipText"));
        lblInterstellarMapShowJumpRadiusMinimumZoom.setName("lblInterstellarMapShowJumpRadiusMinimumZoom");

        spnInterstellarMapShowJumpRadiusMinimumZoom = new JSpinner(new SpinnerNumberModel(3d, 0d, 10d, 0.5));
        spnInterstellarMapShowJumpRadiusMinimumZoom.setToolTipText(resources.getString("lblInterstellarMapShowJumpRadiusMinimumZoom.toolTipText"));
        spnInterstellarMapShowJumpRadiusMinimumZoom.setName("spnInterstellarMapShowJumpRadiusMinimumZoom");

        btnInterstellarMapJumpRadiusColour = new ColourSelectorButton(resources.getString("btnInterstellarMapJumpRadiusColour.text"));
        btnInterstellarMapJumpRadiusColour.setToolTipText(resources.getString("btnInterstellarMapJumpRadiusColour.toolTipText"));
        btnInterstellarMapJumpRadiusColour.setName("btnInterstellarMapJumpRadiusColour");

        chkInterstellarMapShowPlanetaryAcquisitionRadius = new JCheckBox(resources.getString("chkInterstellarMapShowPlanetaryAcquisitionRadius.text"));
        chkInterstellarMapShowPlanetaryAcquisitionRadius.setToolTipText(resources.getString("chkInterstellarMapShowPlanetaryAcquisitionRadius.toolTipText"));
        chkInterstellarMapShowPlanetaryAcquisitionRadius.setName("chkInterstellarMapShowPlanetaryAcquisitionRadius");
        chkInterstellarMapShowPlanetaryAcquisitionRadius.addActionListener(evt -> {
            final boolean selected = chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected();
            lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setEnabled(selected);
            spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setEnabled(selected);
            btnInterstellarMapPlanetaryAcquisitionRadiusColour.setEnabled(selected);
        });

        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setText(resources.getString("lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.text"));
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setToolTipText(resources.getString("lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.toolTipText"));
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setName("lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom");

        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom = new JSpinner(new SpinnerNumberModel(2d, 0d, 10d, 0.5));
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setToolTipText(resources.getString("lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.toolTipText"));
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setName("spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom");

        btnInterstellarMapPlanetaryAcquisitionRadiusColour = new ColourSelectorButton(resources.getString("btnInterstellarMapPlanetaryAcquisitionRadiusColour.text"));
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setToolTipText(resources.getString("btnInterstellarMapPlanetaryAcquisitionRadiusColour.toolTipText"));
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setName("btnInterstellarMapPlanetaryAcquisitionRadiusColour");

        chkInterstellarMapShowContractSearchRadius = new JCheckBox(resources.getString("chkInterstellarMapShowContractSearchRadius.text"));
        chkInterstellarMapShowContractSearchRadius.setToolTipText(resources.getString("chkInterstellarMapShowContractSearchRadius.toolTipText"));
        chkInterstellarMapShowContractSearchRadius.setName("chkInterstellarMapShowContractSearchRadius");
        chkInterstellarMapShowContractSearchRadius.addActionListener(evt ->
                btnInterstellarMapContractSearchRadiusColour.setEnabled(chkInterstellarMapShowContractSearchRadius.isSelected()));

        btnInterstellarMapContractSearchRadiusColour = new ColourSelectorButton(resources.getString("btnInterstellarMapContractSearchRadiusColour.text"));
        btnInterstellarMapContractSearchRadiusColour.setToolTipText(resources.getString("btnInterstellarMapContractSearchRadiusColour.toolTipText"));
        btnInterstellarMapContractSearchRadiusColour.setName("btnInterstellarMapContractSearchRadiusColour");
        //endregion Interstellar Map Tab

        //region Personnel Tab
        JLabel labelPersonnelDisplay = new JLabel(resources.getString("labelPersonnelDisplay.text"));

        JLabel labelPersonnelFilterStyle = new JLabel(resources.getString("optionPersonnelFilterStyle.text"));
        labelPersonnelFilterStyle.setToolTipText(resources.getString("optionPersonnelFilterStyle.toolTipText"));

        optionPersonnelFilterStyle = new JComboBox<>(PersonnelFilterStyle.values());
        optionPersonnelFilterStyle.setRenderer(new DefaultListCellRenderer() {
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
        //endregion Personnel Tab

        // Programmatically Assign Accessibility Labels
        lblInterstellarMapShowJumpRadiusMinimumZoom.setLabelFor(spnInterstellarMapShowJumpRadiusMinimumZoom);
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setLabelFor(spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom);

        // Disable Panel Portions by Default
        chkInterstellarMapShowJumpRadius.setSelected(true);
        chkInterstellarMapShowJumpRadius.doClick();
        chkInterstellarMapShowPlanetaryAcquisitionRadius.setSelected(true);
        chkInterstellarMapShowPlanetaryAcquisitionRadius.doClick();
        chkInterstellarMapShowContractSearchRadius.setSelected(true);
        chkInterstellarMapShowContractSearchRadius.doClick();

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
                        .addComponent(chkCompanyGeneratorStartup)
                        .addComponent(chkShowCompanyGenerator)
                        .addComponent(labelCommandCenterDisplay)
                        .addComponent(optionCommandCenterUseUnitMarket)
                        .addComponent(optionCommandCenterMRMS)
                        .addComponent(lblInterstellarMapTab)
                        .addComponent(chkInterstellarMapShowJumpRadius)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInterstellarMapShowJumpRadiusMinimumZoom)
                                .addComponent(spnInterstellarMapShowJumpRadiusMinimumZoom, GroupLayout.Alignment.TRAILING))
                        .addComponent(btnInterstellarMapJumpRadiusColour)
                        .addComponent(chkInterstellarMapShowPlanetaryAcquisitionRadius)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom)
                                .addComponent(spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom, GroupLayout.Alignment.TRAILING))
                        .addComponent(btnInterstellarMapPlanetaryAcquisitionRadiusColour)
                        .addComponent(chkInterstellarMapShowContractSearchRadius)
                        .addComponent(btnInterstellarMapContractSearchRadiusColour)
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
                        .addComponent(chkCompanyGeneratorStartup)
                        .addComponent(chkShowCompanyGenerator)
                        .addComponent(labelCommandCenterDisplay)
                        .addComponent(optionCommandCenterUseUnitMarket)
                        .addComponent(optionCommandCenterMRMS)
                        .addComponent(lblInterstellarMapTab)
                        .addComponent(chkInterstellarMapShowJumpRadius)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInterstellarMapShowJumpRadiusMinimumZoom)
                                .addComponent(spnInterstellarMapShowJumpRadiusMinimumZoom))
                        .addComponent(btnInterstellarMapJumpRadiusColour)
                        .addComponent(chkInterstellarMapShowPlanetaryAcquisitionRadius)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom)
                                .addComponent(spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom))
                        .addComponent(btnInterstellarMapPlanetaryAcquisitionRadiusColour)
                        .addComponent(chkInterstellarMapShowContractSearchRadius)
                        .addComponent(btnInterstellarMapContractSearchRadiusColour)
                        .addComponent(labelPersonnelDisplay)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(labelPersonnelFilterStyle)
                                .addComponent(optionPersonnelFilterStyle))
                        .addComponent(optionPersonnelFilterOnPrimaryRole)
        );

        return body;
    }

    private JPanel createColoursTab() {
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

        optionPregnantForeground = new ColourSelectorButton(resources.getString("optionPregnantForeground.text"));

        optionPregnantBackground = new ColourSelectorButton(resources.getString("optionPregnantBackground.text"));

        optionPaidRetirementForeground = new ColourSelectorButton(resources.getString("optionPaidRetirementForeground.text"));

        optionPaidRetirementBackground = new ColourSelectorButton(resources.getString("optionPaidRetirementBackground.text"));

        optionStratConHexCoordForeground = new ColourSelectorButton(resources.getString("optionStratConHexCoordForeground.text"));
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
                                .addComponent(optionPregnantForeground)
                                .addComponent(optionPregnantBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionPaidRetirementForeground)
                                .addComponent(optionPaidRetirementBackground, GroupLayout.Alignment.TRAILING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(optionStratConHexCoordForeground))
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
                                .addComponent(optionPregnantForeground)
                                .addComponent(optionPregnantBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionPaidRetirementForeground)
                                .addComponent(optionPaidRetirementBackground))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(optionStratConHexCoordForeground))
        );
        //endregion Layout

        return body;
    }

    private JPanel createFontsTab() {
        // Create Panel Components
        final JLabel lblMedicalViewDialogHandwritingFont = new JLabel(resources.getString("lblMedicalViewDialogHandwritingFont.text"));
        lblMedicalViewDialogHandwritingFont.setToolTipText(resources.getString("lblMedicalViewDialogHandwritingFont.toolTipText"));
        lblMedicalViewDialogHandwritingFont.setName("lblMedicalViewDialogHandwritingFont");

        comboMedicalViewDialogHandwritingFont = new FontComboBox("comboMedicalViewDialogHandwritingFont");
        comboMedicalViewDialogHandwritingFont.setToolTipText(resources.getString("lblMedicalViewDialogHandwritingFont.toolTipText"));

        // Programmatically Assign Accessibility Labels
        lblMedicalViewDialogHandwritingFont.setLabelFor(comboMedicalViewDialogHandwritingFont);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("fontPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(lblMedicalViewDialogHandwritingFont)
                                .addComponent(comboMedicalViewDialogHandwritingFont, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMedicalViewDialogHandwritingFont)
                                .addComponent(comboMedicalViewDialogHandwritingFont))
        );

        return panel;
    }

    private JPanel createAutosaveTab() {
        // Create Panel Components
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
        // Initialize Components Used in ActionListeners
        final JLabel lblNewDayForceIconOperationalStatusStyle = new JLabel(resources.getString("lblNewDayForceIconOperationalStatusStyle.text"));

        // Create Panel Components
        chkNewDayAstechPoolFill = new JCheckBox(resources.getString("chkNewDayAstechPoolFill.text"));
        chkNewDayAstechPoolFill.setToolTipText(resources.getString("chkNewDayAstechPoolFill.toolTipText"));
        chkNewDayAstechPoolFill.setName("chkNewDayAstechPoolFill");

        chkNewDayMedicPoolFill = new JCheckBox(resources.getString("chkNewDayMedicPoolFill.text"));
        chkNewDayMedicPoolFill.setToolTipText(resources.getString("chkNewDayMedicPoolFill.toolTipText"));
        chkNewDayMedicPoolFill.setName("chkNewDayMedicPoolFill");

        chkNewDayMRMS = new JCheckBox(resources.getString("chkNewDayMRMS.text"));
        chkNewDayMRMS.setToolTipText(resources.getString("chkNewDayMRMS.toolTipText"));
        chkNewDayMRMS.setName("chkNewDayMRMS");

        chkNewDayForceIconOperationalStatus = new JCheckBox(resources.getString("chkNewDayForceIconOperationalStatus.text"));
        chkNewDayForceIconOperationalStatus.setToolTipText(resources.getString("chkNewDayForceIconOperationalStatus.toolTipText"));
        chkNewDayForceIconOperationalStatus.setName("chkNewDayForceIconOperationalStatus");
        chkNewDayForceIconOperationalStatus.addActionListener(evt -> {
            final boolean selected = chkNewDayForceIconOperationalStatus.isSelected();
            lblNewDayForceIconOperationalStatusStyle.setEnabled(selected);
            comboNewDayForceIconOperationalStatusStyle.setEnabled(selected);
        });

        lblNewDayForceIconOperationalStatusStyle.setToolTipText(resources.getString("lblNewDayForceIconOperationalStatusStyle.toolTipText"));
        lblNewDayForceIconOperationalStatusStyle.setName("lblNewDayForceIconOperationalStatusStyle");

        comboNewDayForceIconOperationalStatusStyle = new MMComboBox<>(
                "comboNewDayForceIconOperationalStatusStyle", ForceIconOperationalStatusStyle.values());
        comboNewDayForceIconOperationalStatusStyle.setToolTipText(resources.getString("lblNewDayForceIconOperationalStatusStyle.toolTipText"));
        comboNewDayForceIconOperationalStatusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ForceIconOperationalStatusStyle) {
                    list.setToolTipText(((ForceIconOperationalStatusStyle) value).getToolTipText());
                }
                return this;
            }
        });

        // Programmatically Assign Accessibility Labels
        lblNewDayForceIconOperationalStatusStyle.setLabelFor(comboNewDayForceIconOperationalStatusStyle);

        // Disable Panel Portions by Default
        chkNewDayForceIconOperationalStatus.setSelected(true);
        chkNewDayForceIconOperationalStatus.doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("newDayPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkNewDayAstechPoolFill)
                        .addComponent(chkNewDayMedicPoolFill)
                        .addComponent(chkNewDayMRMS)
                        .addComponent(chkNewDayForceIconOperationalStatus)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblNewDayForceIconOperationalStatusStyle)
                                .addComponent(comboNewDayForceIconOperationalStatusStyle, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkNewDayAstechPoolFill)
                        .addComponent(chkNewDayMedicPoolFill)
                        .addComponent(chkNewDayMRMS)
                        .addComponent(chkNewDayForceIconOperationalStatus)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNewDayForceIconOperationalStatusStyle)
                                .addComponent(comboNewDayForceIconOperationalStatusStyle))
        );

        return panel;
    }

    private JPanel createCampaignXMLSaveTab() {
        // Create Panel Components
        optionPreferGzippedOutput = new JCheckBox(resources.getString("optionPreferGzippedOutput.text"));
        optionPreferGzippedOutput.setToolTipText(resources.getString("optionPreferGzippedOutput.toolTipText"));

        optionWriteCustomsToXML = new JCheckBox(resources.getString("optionWriteCustomsToXML.text"));
        optionWriteCustomsToXML.setMnemonic(KeyEvent.VK_C);

        optionSaveMothballState = new JCheckBox(resources.getString("optionSaveMothballState.text"));
        optionSaveMothballState.setToolTipText(resources.getString("optionSaveMothballState.toolTipText"));
        optionSaveMothballState.setMnemonic(KeyEvent.VK_U);

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

        return body;
    }

    private JPanel createNagTab() {
        // Create Panel Components
        optionUnmaintainedUnitsNag = new JCheckBox(resources.getString("optionUnmaintainedUnitsNag.text"));
        optionUnmaintainedUnitsNag.setToolTipText(resources.getString("optionUnmaintainedUnitsNag.toolTipText"));
        optionUnmaintainedUnitsNag.setName("optionUnmaintainedUnitsNag");

        optionPregnantCombatantNag = new JCheckBox(resources.getString("optionPregnantCombatantNag.text"));
        optionPregnantCombatantNag.setToolTipText(resources.getString("optionPregnantCombatantNag.toolTipText"));
        optionPregnantCombatantNag.setName("optionPregnantCombatantNag");

        optionPrisonersNag = new JCheckBox(resources.getString("optionPrisonersNag.text"));
        optionPrisonersNag.setToolTipText(resources.getString("optionPrisonersNag.toolTipText"));
        optionPrisonersNag.setName("optionPrisonersNag");

        optionUntreatedPersonnelNag = new JCheckBox(resources.getString("optionUntreatedPersonnelNag.text"));
        optionUntreatedPersonnelNag.setToolTipText(resources.getString("optionUntreatedPersonnelNag.toolTipText"));
        optionUntreatedPersonnelNag.setName("optionUntreatedPersonnelNag");

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

        optionCargoCapacityNag = new JCheckBox(resources.getString("optionCargoCapacityNag.text"));
        optionCargoCapacityNag.setToolTipText(resources.getString("optionCargoCapacityNag.toolTipText"));
        optionCargoCapacityNag.setName("optionCargoCapacityNag");

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
                        .addComponent(optionPregnantCombatantNag)
                        .addComponent(optionPrisonersNag)
                        .addComponent(optionUntreatedPersonnelNag)
                        .addComponent(optionInsufficientAstechsNag)
                        .addComponent(optionInsufficientAstechTimeNag)
                        .addComponent(optionInsufficientMedicsNag)
                        .addComponent(optionShortDeploymentNag)
                        .addComponent(optionUnresolvedStratConContactsNag)
                        .addComponent(optionOutstandingScenariosNag)
                        .addComponent(optionCargoCapacityNag)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(optionUnmaintainedUnitsNag)
                        .addComponent(optionPregnantCombatantNag)
                        .addComponent(optionPrisonersNag)
                        .addComponent(optionUntreatedPersonnelNag)
                        .addComponent(optionInsufficientAstechsNag)
                        .addComponent(optionInsufficientAstechTimeNag)
                        .addComponent(optionInsufficientMedicsNag)
                        .addComponent(optionShortDeploymentNag)
                        .addComponent(optionUnresolvedStratConContactsNag)
                        .addComponent(optionOutstandingScenariosNag)
                        .addComponent(optionCargoCapacityNag)
        );

        return panel;
    }

    private JPanel createMiscellaneousTab() {
        // Create Panel Components
        final JLabel lblUserDir = new JLabel(resources.getString("lblUserDir.text"));
        lblUserDir.setToolTipText(resources.getString("lblUserDir.toolTipText"));
        lblUserDir.setName("lblUserDir");

        txtUserDir = new JTextField(20);
        txtUserDir.setToolTipText(resources.getString("lblUserDir.toolTipText"));
        txtUserDir.setName("txtUserDir");

        JButton userDirChooser = new JButton("...");
        userDirChooser.addActionListener(e -> CommonSettingsDialog.fileChooseUserDir(txtUserDir, getFrame()));
        userDirChooser.setToolTipText(resources.getString("userDirChooser.title"));

        JButton userDirHelp = new JButton("Help");
        try {
            String helpTitle = Messages.getString("UserDirHelpDialog.title");
            URL helpFile = new File(MMConstants.USER_DIR_README_FILE).toURI().toURL();
            userDirHelp.addActionListener(e -> new HelpDialog(helpTitle, helpFile, getFrame()).setVisible(true));
        } catch (MalformedURLException e) {
            LogManager.getLogger().error("Could not find the user data directory readme file at "
                    + MMConstants.USER_DIR_README_FILE);
        }

        final JLabel lblStartGameDelay = new JLabel(resources.getString("lblStartGameDelay.text"));
        lblStartGameDelay.setToolTipText(resources.getString("lblStartGameDelay.toolTipText"));
        lblStartGameDelay.setName("lblStartGameDelay");

        spnStartGameDelay = new JSpinner(new SpinnerNumberModel(1000, 250, 2500, 25));
        spnStartGameDelay.setToolTipText(resources.getString("lblStartGameDelay.toolTipText"));
        spnStartGameDelay.setName("spnStartGameDelay");

        final JLabel lblStartGameClientDelay = new JLabel(resources.getString("lblStartGameClientDelay.text"));
        lblStartGameClientDelay.setToolTipText(resources.getString("lblStartGameClientDelay.toolTipText"));
        lblStartGameClientDelay.setName("lblStartGameClientDelay");

        spnStartGameClientDelay = new JSpinner(new SpinnerNumberModel(50, 50, 2500, 25));
        spnStartGameClientDelay.setToolTipText(resources.getString("lblStartGameClientDelay.toolTipText"));
        spnStartGameClientDelay.setName("spnStartGameClientDelay");

        final JLabel lblStartGameClientRetryCount = new JLabel(resources.getString("lblStartGameClientRetryCount.text"));
        lblStartGameClientRetryCount.setToolTipText(resources.getString("lblStartGameClientRetryCount.toolTipText"));
        lblStartGameClientRetryCount.setName("lblStartGameClientRetryCount");

        spnStartGameClientRetryCount = new JSpinner(new SpinnerNumberModel(1000, 100, 2500, 50));
        spnStartGameClientRetryCount.setToolTipText(resources.getString("lblStartGameClientRetryCount.toolTipText"));
        spnStartGameClientRetryCount.setName("spnStartGameClientRetryCount");

        final JLabel lblStartGameBotClientDelay = new JLabel(resources.getString("lblStartGameBotClientDelay.text"));
        lblStartGameBotClientDelay.setToolTipText(resources.getString("lblStartGameBotClientDelay.toolTipText"));
        lblStartGameBotClientDelay.setName("lblStartGameBotClientDelay");

        spnStartGameBotClientDelay = new JSpinner(new SpinnerNumberModel(50, 50, 2500, 25));
        spnStartGameBotClientDelay.setToolTipText(resources.getString("lblStartGameBotClientDelay.toolTipText"));
        spnStartGameBotClientDelay.setName("spnBotClientStartGameDelay");

        final JLabel lblStartGameBotClientRetryCount = new JLabel(resources.getString("lblStartGameBotClientRetryCount.text"));
        lblStartGameBotClientRetryCount.setToolTipText(resources.getString("lblStartGameBotClientRetryCount.toolTipText"));
        lblStartGameBotClientRetryCount.setName("lblStartGameBotClientRetryCount");

        spnStartGameBotClientRetryCount = new JSpinner(new SpinnerNumberModel(250, 100, 2500, 50));
        spnStartGameBotClientRetryCount.setToolTipText(resources.getString("lblStartGameBotClientRetryCount.toolTipText"));
        spnStartGameBotClientRetryCount.setName("spnStartGameBotClientRetryCount");

        final JLabel lblDefaultCompanyGenerationMethod = new JLabel(resources.getString("lblDefaultCompanyGenerationMethod.text"));
        lblDefaultCompanyGenerationMethod.setToolTipText(resources.getString("lblDefaultCompanyGenerationMethod.toolTipText"));
        lblDefaultCompanyGenerationMethod.setName("lblDefaultCompanyGenerationMethod");

        comboDefaultCompanyGenerationMethod = new MMComboBox<>("comboDefaultCompanyGenerationMethod", CompanyGenerationMethod.values());
        comboDefaultCompanyGenerationMethod.setToolTipText(resources.getString("lblDefaultCompanyGenerationMethod.toolTipText"));
        comboDefaultCompanyGenerationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CompanyGenerationMethod) {
                    list.setToolTipText(((CompanyGenerationMethod) value).getToolTipText());
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
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblUserDir)
                                .addComponent(txtUserDir)
                                .addComponent(userDirChooser)
                                .addComponent(userDirHelp, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartGameDelay)
                                .addComponent(spnStartGameDelay, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartGameClientDelay)
                                .addComponent(spnStartGameClientDelay, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartGameClientRetryCount)
                                .addComponent(spnStartGameClientRetryCount, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartGameBotClientDelay)
                                .addComponent(spnStartGameBotClientDelay, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartGameBotClientRetryCount)
                                .addComponent(spnStartGameBotClientRetryCount, GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.DEFAULT_SIZE, 40))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblDefaultCompanyGenerationMethod)
                                .addComponent(comboDefaultCompanyGenerationMethod))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblUserDir)
                                .addComponent(txtUserDir)
                                .addComponent(userDirChooser)
                                .addComponent(userDirHelp))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartGameDelay)
                                .addComponent(spnStartGameDelay))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartGameClientDelay)
                                .addComponent(spnStartGameClientDelay))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartGameClientRetryCount)
                                .addComponent(spnStartGameClientRetryCount))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartGameBotClientDelay)
                                .addComponent(spnStartGameBotClientDelay))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartGameBotClientRetryCount)
                                .addComponent(spnStartGameBotClientRetryCount))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblDefaultCompanyGenerationMethod)
                                .addComponent(comboDefaultCompanyGenerationMethod))
        );

        return body;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        if (validateDateFormat(optionDisplayDateFormat.getText())) {
            MekHQ.getMHQOptions().setDisplayDateFormat(optionDisplayDateFormat.getText());
        }

        if (validateDateFormat(optionLongDisplayDateFormat.getText())) {
            MekHQ.getMHQOptions().setLongDisplayDateFormat(optionLongDisplayDateFormat.getText());
        }
        MekHQ.getMHQOptions().setHistoricalDailyLog(optionHistoricalDailyLog.isSelected());
        MekHQ.getMHQOptions().setCompanyGeneratorStartup(chkCompanyGeneratorStartup.isSelected());
        MekHQ.getMHQOptions().setShowCompanyGenerator(chkShowCompanyGenerator.isSelected());

        // Command Center Tab
        MekHQ.getMHQOptions().setCommandCenterUseUnitMarket(optionCommandCenterUseUnitMarket.isSelected());
        MekHQ.getMHQOptions().setCommandCenterMRMS(optionCommandCenterMRMS.isSelected());

        // Interstellar Map Tab
        MekHQ.getMHQOptions().setInterstellarMapShowJumpRadius(chkInterstellarMapShowJumpRadius.isSelected());
        MekHQ.getMHQOptions().setInterstellarMapShowJumpRadiusMinimumZoom((Double) spnInterstellarMapShowJumpRadiusMinimumZoom.getValue());
        MekHQ.getMHQOptions().setInterstellarMapJumpRadiusColour(btnInterstellarMapJumpRadiusColour.getColour());
        MekHQ.getMHQOptions().setInterstellarMapShowPlanetaryAcquisitionRadius(chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected());
        MekHQ.getMHQOptions().setInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom((Double) spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.getValue());
        MekHQ.getMHQOptions().setInterstellarMapPlanetaryAcquisitionRadiusColour(btnInterstellarMapPlanetaryAcquisitionRadiusColour.getColour());
        MekHQ.getMHQOptions().setInterstellarMapShowContractSearchRadius(chkInterstellarMapShowContractSearchRadius.isSelected());
        MekHQ.getMHQOptions().setInterstellarMapContractSearchRadiusColour(btnInterstellarMapContractSearchRadiusColour.getColour());

        // Personnel Tab
        MekHQ.getMHQOptions().setPersonnelFilterStyle((PersonnelFilterStyle) Objects.requireNonNull(optionPersonnelFilterStyle.getSelectedItem()));
        MekHQ.getMHQOptions().setPersonnelFilterOnPrimaryRole(optionPersonnelFilterOnPrimaryRole.isSelected());

        // Colours
        MekHQ.getMHQOptions().setDeployedForeground(optionDeployedForeground.getColour());
        MekHQ.getMHQOptions().setDeployedBackground(optionDeployedBackground.getColour());
        MekHQ.getMHQOptions().setBelowContractMinimumForeground(optionBelowContractMinimumForeground.getColour());
        MekHQ.getMHQOptions().setBelowContractMinimumBackground(optionBelowContractMinimumBackground.getColour());
        MekHQ.getMHQOptions().setInTransitForeground(optionInTransitForeground.getColour());
        MekHQ.getMHQOptions().setInTransitBackground(optionInTransitBackground.getColour());
        MekHQ.getMHQOptions().setRefittingForeground(optionRefittingForeground.getColour());
        MekHQ.getMHQOptions().setRefittingBackground(optionRefittingBackground.getColour());
        MekHQ.getMHQOptions().setMothballingForeground(optionMothballingForeground.getColour());
        MekHQ.getMHQOptions().setMothballingBackground(optionMothballingBackground.getColour());
        MekHQ.getMHQOptions().setMothballedForeground(optionMothballedForeground.getColour());
        MekHQ.getMHQOptions().setMothballedBackground(optionMothballedBackground.getColour());
        MekHQ.getMHQOptions().setNotRepairableForeground(optionNotRepairableForeground.getColour());
        MekHQ.getMHQOptions().setNotRepairableBackground(optionNotRepairableBackground.getColour());
        MekHQ.getMHQOptions().setNonFunctionalForeground(optionNonFunctionalForeground.getColour());
        MekHQ.getMHQOptions().setNonFunctionalBackground(optionNonFunctionalBackground.getColour());
        MekHQ.getMHQOptions().setNeedsPartsFixedForeground(optionNeedsPartsFixedForeground.getColour());
        MekHQ.getMHQOptions().setNeedsPartsFixedBackground(optionNeedsPartsFixedBackground.getColour());
        MekHQ.getMHQOptions().setUnmaintainedForeground(optionUnmaintainedForeground.getColour());
        MekHQ.getMHQOptions().setUnmaintainedBackground(optionUnmaintainedBackground.getColour());
        MekHQ.getMHQOptions().setUncrewedForeground(optionUncrewedForeground.getColour());
        MekHQ.getMHQOptions().setUncrewedBackground(optionUncrewedBackground.getColour());
        MekHQ.getMHQOptions().setLoanOverdueForeground(optionLoanOverdueForeground.getColour());
        MekHQ.getMHQOptions().setLoanOverdueBackground(optionLoanOverdueBackground.getColour());
        MekHQ.getMHQOptions().setInjuredForeground(optionInjuredForeground.getColour());
        MekHQ.getMHQOptions().setInjuredBackground(optionInjuredBackground.getColour());
        MekHQ.getMHQOptions().setHealedInjuriesForeground(optionHealedInjuriesForeground.getColour());
        MekHQ.getMHQOptions().setHealedInjuriesBackground(optionHealedInjuriesBackground.getColour());
        MekHQ.getMHQOptions().setPregnantForeground(optionPregnantForeground.getColour());
        MekHQ.getMHQOptions().setPregnantBackground(optionPregnantBackground.getColour());
        MekHQ.getMHQOptions().setPaidRetirementForeground(optionPaidRetirementForeground.getColour());
        MekHQ.getMHQOptions().setPaidRetirementBackground(optionPaidRetirementBackground.getColour());
        MekHQ.getMHQOptions().setStratConHexCoordForeground(optionStratConHexCoordForeground.getColour());
        MekHQ.getMHQOptions().setMedicalViewDialogHandwritingFont(comboMedicalViewDialogHandwritingFont.getFont().getFamily());

        MekHQ.getMHQOptions().setNoAutosaveValue(optionNoSave.isSelected());
        MekHQ.getMHQOptions().setAutosaveDailyValue(optionSaveDaily.isSelected());
        MekHQ.getMHQOptions().setAutosaveWeeklyValue(optionSaveWeekly.isSelected());
        MekHQ.getMHQOptions().setAutosaveMonthlyValue(optionSaveMonthly.isSelected());
        MekHQ.getMHQOptions().setAutosaveYearlyValue(optionSaveYearly.isSelected());
        MekHQ.getMHQOptions().setAutosaveBeforeMissionsValue(checkSaveBeforeMissions.isSelected());
        MekHQ.getMHQOptions().setMaximumNumberOfAutosavesValue((Integer) spinnerSavedGamesCount.getValue());

        MekHQ.getMHQOptions().setNewDayAstechPoolFill(chkNewDayAstechPoolFill.isSelected());
        MekHQ.getMHQOptions().setNewDayMedicPoolFill(chkNewDayMedicPoolFill.isSelected());
        MekHQ.getMHQOptions().setNewDayMRMS(chkNewDayMRMS.isSelected());
        MekHQ.getMHQOptions().setNewDayForceIconOperationalStatus(chkNewDayForceIconOperationalStatus.isSelected());
        MekHQ.getMHQOptions().setNewDayForceIconOperationalStatusStyle(Objects.requireNonNull(comboNewDayForceIconOperationalStatusStyle.getSelectedItem()));

        MekHQ.getMHQOptions().setPreferGzippedOutput(optionPreferGzippedOutput.isSelected());
        MekHQ.getMHQOptions().setWriteCustomsToXML(optionWriteCustomsToXML.isSelected());
        MekHQ.getMHQOptions().setSaveMothballState(optionSaveMothballState.isSelected());

        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_UNMAINTAINED_UNITS, optionUnmaintainedUnitsNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_PREGNANT_COMBATANT, optionPregnantCombatantNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_PRISONERS, optionPrisonersNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_UNTREATED_PERSONNEL, optionUntreatedPersonnelNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_ASTECHS, optionInsufficientAstechsNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME, optionInsufficientAstechTimeNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_MEDICS, optionInsufficientMedicsNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_SHORT_DEPLOYMENT, optionShortDeploymentNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS, optionUnresolvedStratConContactsNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_OUTSTANDING_SCENARIOS, optionOutstandingScenariosNag.isSelected());
        MekHQ.getMHQOptions().setNagDialogIgnore(MHQConstants.NAG_CARGO_CAPACITY, optionCargoCapacityNag.isSelected());

        PreferenceManager.getClientPreferences().setUserDir(txtUserDir.getText());
        PreferenceManager.getInstance().save();
        MekHQ.getMHQOptions().setStartGameDelay((Integer) spnStartGameDelay.getValue());
        MekHQ.getMHQOptions().setStartGameClientDelay((Integer) spnStartGameClientDelay.getValue());
        MekHQ.getMHQOptions().setStartGameClientRetryCount((Integer) spnStartGameClientRetryCount.getValue());
        MekHQ.getMHQOptions().setStartGameBotClientDelay((Integer) spnStartGameBotClientDelay.getValue());
        MekHQ.getMHQOptions().setStartGameBotClientRetryCount((Integer) spnStartGameBotClientRetryCount.getValue());
        MekHQ.getMHQOptions().setDefaultCompanyGenerationMethod(Objects.requireNonNull(comboDefaultCompanyGenerationMethod.getSelectedItem()));

        MekHQ.triggerEvent(new MHQOptionsChangedEvent());
    }

    private void setInitialState() {
        optionDisplayDateFormat.setText(MekHQ.getMHQOptions().getDisplayDateFormat());
        optionLongDisplayDateFormat.setText(MekHQ.getMHQOptions().getLongDisplayDateFormat());
        optionHistoricalDailyLog.setSelected(MekHQ.getMHQOptions().getHistoricalDailyLog());
        chkCompanyGeneratorStartup.setSelected(MekHQ.getMHQOptions().getCompanyGeneratorStartup());
        chkShowCompanyGenerator.setSelected(MekHQ.getMHQOptions().getShowCompanyGenerator());

        // Command Center Tab
        optionCommandCenterUseUnitMarket.setSelected(MekHQ.getMHQOptions().getCommandCenterUseUnitMarket());
        optionCommandCenterMRMS.setSelected(MekHQ.getMHQOptions().getCommandCenterMRMS());

        // Interstellar Map Tab
        if (chkInterstellarMapShowJumpRadius.isSelected() != MekHQ.getMHQOptions().getInterstellarMapShowJumpRadius()) {
            chkInterstellarMapShowJumpRadius.doClick();
        }
        spnInterstellarMapShowJumpRadiusMinimumZoom.setValue(MekHQ.getMHQOptions().getInterstellarMapShowJumpRadiusMinimumZoom());
        btnInterstellarMapJumpRadiusColour.setColour(MekHQ.getMHQOptions().getInterstellarMapJumpRadiusColour());
        if (chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected() != MekHQ.getMHQOptions().getInterstellarMapShowPlanetaryAcquisitionRadius()) {
            chkInterstellarMapShowPlanetaryAcquisitionRadius.doClick();
        }
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setValue(MekHQ.getMHQOptions().getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom());
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setColour(MekHQ.getMHQOptions().getInterstellarMapPlanetaryAcquisitionRadiusColour());
        if (chkInterstellarMapShowContractSearchRadius.isSelected() != MekHQ.getMHQOptions().getInterstellarMapShowContractSearchRadius()) {
            chkInterstellarMapShowContractSearchRadius.doClick();
        }
        btnInterstellarMapContractSearchRadiusColour.setColour(MekHQ.getMHQOptions().getInterstellarMapContractSearchRadiusColour());

        // Personnel Tab
        optionPersonnelFilterStyle.setSelectedItem(MekHQ.getMHQOptions().getPersonnelFilterStyle());
        optionPersonnelFilterOnPrimaryRole.setSelected(MekHQ.getMHQOptions().getPersonnelFilterOnPrimaryRole());

        // Colours
        optionDeployedForeground.setColour(MekHQ.getMHQOptions().getDeployedForeground());
        optionDeployedBackground.setColour(MekHQ.getMHQOptions().getDeployedBackground());
        optionBelowContractMinimumForeground.setColour(MekHQ.getMHQOptions().getBelowContractMinimumForeground());
        optionBelowContractMinimumBackground.setColour(MekHQ.getMHQOptions().getBelowContractMinimumBackground());
        optionInTransitForeground.setColour(MekHQ.getMHQOptions().getInTransitForeground());
        optionInTransitBackground.setColour(MekHQ.getMHQOptions().getInTransitBackground());
        optionRefittingForeground.setColour(MekHQ.getMHQOptions().getRefittingForeground());
        optionRefittingBackground.setColour(MekHQ.getMHQOptions().getRefittingBackground());
        optionMothballingForeground.setColour(MekHQ.getMHQOptions().getMothballingForeground());
        optionMothballingBackground.setColour(MekHQ.getMHQOptions().getMothballingBackground());
        optionMothballedForeground.setColour(MekHQ.getMHQOptions().getMothballedForeground());
        optionMothballedBackground.setColour(MekHQ.getMHQOptions().getMothballedBackground());
        optionNotRepairableForeground.setColour(MekHQ.getMHQOptions().getNotRepairableForeground());
        optionNotRepairableBackground.setColour(MekHQ.getMHQOptions().getNotRepairableBackground());
        optionNonFunctionalForeground.setColour(MekHQ.getMHQOptions().getNonFunctionalForeground());
        optionNonFunctionalBackground.setColour(MekHQ.getMHQOptions().getNonFunctionalBackground());
        optionNeedsPartsFixedForeground.setColour(MekHQ.getMHQOptions().getNeedsPartsFixedForeground());
        optionNeedsPartsFixedBackground.setColour(MekHQ.getMHQOptions().getNeedsPartsFixedBackground());
        optionUnmaintainedForeground.setColour(MekHQ.getMHQOptions().getUnmaintainedForeground());
        optionUnmaintainedBackground.setColour(MekHQ.getMHQOptions().getUnmaintainedBackground());
        optionUncrewedForeground.setColour(MekHQ.getMHQOptions().getUncrewedForeground());
        optionUncrewedBackground.setColour(MekHQ.getMHQOptions().getUncrewedBackground());
        optionLoanOverdueForeground.setColour(MekHQ.getMHQOptions().getLoanOverdueForeground());
        optionLoanOverdueBackground.setColour(MekHQ.getMHQOptions().getLoanOverdueBackground());
        optionInjuredForeground.setColour(MekHQ.getMHQOptions().getInjuredForeground());
        optionInjuredBackground.setColour(MekHQ.getMHQOptions().getInjuredBackground());
        optionHealedInjuriesForeground.setColour(MekHQ.getMHQOptions().getHealedInjuriesForeground());
        optionHealedInjuriesBackground.setColour(MekHQ.getMHQOptions().getHealedInjuriesBackground());
        optionPregnantForeground.setColour(MekHQ.getMHQOptions().getPregnantForeground());
        optionPregnantBackground.setColour(MekHQ.getMHQOptions().getPregnantBackground());
        optionPaidRetirementForeground.setColour(MekHQ.getMHQOptions().getPaidRetirementForeground());
        optionPaidRetirementBackground.setColour(MekHQ.getMHQOptions().getPaidRetirementBackground());
        optionStratConHexCoordForeground.setColour(MekHQ.getMHQOptions().getStratConHexCoordForeground());

        comboMedicalViewDialogHandwritingFont.setSelectedItem(new FontDisplay(MekHQ.getMHQOptions().getMedicalViewDialogHandwritingFont()));

        optionNoSave.setSelected(MekHQ.getMHQOptions().getNoAutosaveValue());
        optionSaveDaily.setSelected(MekHQ.getMHQOptions().getAutosaveDailyValue());
        optionSaveWeekly.setSelected(MekHQ.getMHQOptions().getAutosaveWeeklyValue());
        optionSaveMonthly.setSelected(MekHQ.getMHQOptions().getAutosaveMonthlyValue());
        optionSaveYearly.setSelected(MekHQ.getMHQOptions().getAutosaveYearlyValue());
        checkSaveBeforeMissions.setSelected(MekHQ.getMHQOptions().getAutosaveBeforeMissionsValue());
        spinnerSavedGamesCount.setValue(MekHQ.getMHQOptions().getMaximumNumberOfAutosavesValue());

        chkNewDayAstechPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayAstechPoolFill());
        chkNewDayMedicPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayMedicPoolFill());
        chkNewDayMRMS.setSelected(MekHQ.getMHQOptions().getNewDayMRMS());
        if (chkNewDayForceIconOperationalStatus.isSelected() != MekHQ.getMHQOptions().getNewDayForceIconOperationalStatus()) {
            chkNewDayForceIconOperationalStatus.doClick();
        }
        comboNewDayForceIconOperationalStatusStyle.setSelectedItem(MekHQ.getMHQOptions().getNewDayForceIconOperationalStatusStyle());

        optionPreferGzippedOutput.setSelected(MekHQ.getMHQOptions().getPreferGzippedOutput());
        optionWriteCustomsToXML.setSelected(MekHQ.getMHQOptions().getWriteCustomsToXML());
        optionSaveMothballState.setSelected(MekHQ.getMHQOptions().getSaveMothballState());

        optionUnmaintainedUnitsNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_UNMAINTAINED_UNITS));
        optionPregnantCombatantNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_PREGNANT_COMBATANT));
        optionPrisonersNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_PRISONERS));
        optionUntreatedPersonnelNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_UNTREATED_PERSONNEL));
        optionInsufficientAstechsNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_ASTECHS));
        optionInsufficientAstechTimeNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME));
        optionInsufficientMedicsNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_MEDICS));
        optionShortDeploymentNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_SHORT_DEPLOYMENT));
        optionUnresolvedStratConContactsNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_UNRESOLVED_STRATCON_CONTACTS));
        optionOutstandingScenariosNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_OUTSTANDING_SCENARIOS));
        optionCargoCapacityNag.setSelected(MekHQ.getMHQOptions().getNagDialogIgnore(MHQConstants.NAG_CARGO_CAPACITY));

        txtUserDir.setText(PreferenceManager.getClientPreferences().getUserDir());
        spnStartGameDelay.setValue(MekHQ.getMHQOptions().getStartGameDelay());
        spnStartGameClientDelay.setValue(MekHQ.getMHQOptions().getStartGameClientDelay());
        spnStartGameClientRetryCount.setValue(MekHQ.getMHQOptions().getStartGameClientRetryCount());
        spnStartGameBotClientDelay.setValue(MekHQ.getMHQOptions().getStartGameBotClientDelay());
        spnStartGameBotClientRetryCount.setValue(MekHQ.getMHQOptions().getStartGameBotClientRetryCount());
        comboDefaultCompanyGenerationMethod.setSelectedItem(MekHQ.getMHQOptions().getDefaultCompanyGenerationMethod());
    }

    //region Data Validation
    private boolean validateDateFormat(final String format) {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern(format).withLocale(MekHQ.getMHQOptions().getDateLocale()));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
    //endregion Data Validation
}
