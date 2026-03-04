/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.Objects;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.comboBoxes.FontComboBox;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.displayWrappers.FontDisplay;
import megamek.common.preference.PreferenceManager;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MHQOptions;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MekHQ;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.enums.FormationIconOperationalStatusStyle;
import mekhq.gui.enums.PersonnelFilterStyle;

/**
 * MHQOptionsDialog is a dialog that allows the user to configure various options in MegaMekHQ. It extends the
 * {@link AbstractMHQButtonDialog} class and inherits its common dialog features. The dialog allows configuration of
 * options related to display, colors, fonts, autosave, startup behavior, notifications, and various other miscellaneous
 * options.
 * <p>
 * To create an instance of MHQOptionsDialog, invoke one of its constructors with a frame as a parameter.
 * <p>
 * Example Usage: JFrame frame = new JFrame("Main Frame"); MHQOptionsDialog dialog = new MHQOptionsDialog(frame);
 * dialog.setVisible(true);
 * <p>
 * This dialog uses the following Mnemonics: C, D, M, M, S, U, W, Y
 */
public class MHQOptionsDialog extends AbstractMHQButtonDialog {
    private static final MMLogger LOGGER = MMLogger.create(MHQOptionsDialog.class);

    // region Variable Declaration
    // region Display
    private JTextField optionDisplayDateFormat;
    private JTextField optionLongDisplayDateFormat;
    private final JSlider guiScale = new JSlider();
    private JCheckBox optionHideUnitFluff;
    private JCheckBox optionHistoricalDailyLog;
    private JCheckBox chkCompanyGeneratorStartup;
    private JCheckBox chkShowCompanyGenerator;
    private JCheckBox chkShowUnitPicturesOnTOE;

    // region Command Center Tab
    private JCheckBox optionCommandCenterMRMS;
    // endregion Command Center Tab

    // region Interstellar Map Tab
    private JCheckBox chkInterstellarMapShowJumpRadius;
    private JSpinner spnInterstellarMapShowJumpRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapJumpRadiusColour;
    private JCheckBox chkInterstellarMapShowPlanetaryAcquisitionRadius;
    private JSpinner spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom;
    private ColourSelectorButton btnInterstellarMapPlanetaryAcquisitionRadiusColour;
    private JCheckBox chkInterstellarMapShowContractSearchRadius;
    private ColourSelectorButton btnInterstellarMapContractSearchRadiusColour;
    // endregion Interstellar Map Tab

    // region Personnel Tab
    private JComboBox<PersonnelFilterStyle> optionPersonnelFilterStyle;
    private JCheckBox optionPersonnelFilterOnPrimaryRole;
    private JCheckBox chkUnifiedDailyReport;
    // endregion Personnel Tab
    // endregion Display

    // region Colours
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
    private ColourSelectorButton optionGoneForeground;
    private ColourSelectorButton optionGoneBackground;
    private ColourSelectorButton optionAbsentForeground;
    private ColourSelectorButton optionAbsentBackground;
    private ColourSelectorButton optionFatiguedForeground;
    private ColourSelectorButton optionFatiguedBackground;
    private ColourSelectorButton optionStratConHexCoordForeground;
    private ColourSelectorButton optionFontColorNegative;
    private ColourSelectorButton optionFontColorWarning;
    private ColourSelectorButton optionFontColorPositive;
    private ColourSelectorButton optionFontColorAmazing;
    private ColourSelectorButton optionFontColorSkillUltraGreen;
    private ColourSelectorButton optionFontColorSkillGreen;
    private ColourSelectorButton optionFontColorSkillRegular;
    private ColourSelectorButton optionFontColorSkillVeteran;
    private ColourSelectorButton optionFontColorSkillElite;
    // endregion Colors

    // region Fonts
    private FontComboBox comboMedicalViewDialogHandwritingFont;
    // endregion Fonts

    // region Autosave
    private JRadioButton optionNoSave;
    private JRadioButton optionSaveDaily;
    private JRadioButton optionSaveWeekly;
    private JRadioButton optionSaveMonthly;
    private JRadioButton optionSaveYearly;
    private JCheckBox checkSaveBeforeScenarios;
    private JCheckBox checkSaveBeforeContractEnd;
    private JSpinner spinnerSavedGamesCount;
    // endregion Autosave

    // region New Day
    private JCheckBox chkNewDayAsTechPoolFill;
    private JCheckBox chkNewDayMedicPoolFill;
    private JCheckBox chkNewDaySoldierPoolFill;
    private JCheckBox chkNewDayBattleArmorPoolFill;
    private JCheckBox chkNewDayVehicleCrewGroundPoolFill;
    private JCheckBox chkNewDayVehicleCrewVTOLPoolFill;
    private JCheckBox chkNewDayVehicleCrewNavalPoolFill;
    private JCheckBox chkNewDayVesselPilotPoolFill;
    private JCheckBox chkNewDayVesselGunnerPoolFill;
    private JCheckBox chkNewDayVesselCrewPoolFill;
    private JCheckBox chkNewDayMRMS;
    private JCheckBox chkNewDayOptimizeMedicalAssignments;
    private JCheckBox chkNewDayAutomaticallyAssignUnmaintainedUnits;
    private JCheckBox chkNewMonthQuickTrain;
    private JCheckBox chkSelfCorrectMaintenance;
    private JCheckBox chkNewDayFormationIconOperationalStatus;
    private MMComboBox<FormationIconOperationalStatusStyle> comboNewDayFormationIconOperationalStatusStyle;
    // endregion New Day

    // region Campaign XML Save
    private JCheckBox optionPreferGzippedOutput;
    private JCheckBox optionWriteCustomsToXML;
    private JCheckBox optionWriteAllUnitsToXML;
    private JCheckBox optionSaveMothballState;
    // endregion Campaign XML Save

    // region Nag Tab
    private JCheckBox optionUnmaintainedUnitsNag;
    private JCheckBox optionPregnantCombatantNag;
    private JCheckBox optionPrisonersNag;
    private JCheckBox optionHRStrainNag;
    private JCheckBox optionUntreatedPersonnelNag;
    private JCheckBox optionNoCommanderNag;
    private JCheckBox optionContractEndedNag;
    private JCheckBox optionSingleDropNag;
    private JCheckBox optionInsufficientAsTechsNag;
    private JCheckBox optionInsufficientAsTechTimeNag;
    private JCheckBox optionInsufficientMedicsNag;
    private JCheckBox optionShortDeploymentNag;
    private JCheckBox optionCombatChallengeNag;
    private JCheckBox optionUnresolvedStratConContactsNag;
    private JCheckBox optionOutstandingScenariosNag;
    private JCheckBox optionInvalidFactionNag;
    private JCheckBox optionUnableToAffordExpensesNag;
    private JCheckBox optionUnableToAffordRentNag;
    private JCheckBox optionUnableToAffordLoanPaymentNag;
    private JCheckBox optionUnableToAffordJumpNag;
    private JCheckBox optionUnableToAffordShoppingListNag;

    private JCheckBox optionContractRentalConfirmation;
    private JCheckBox optionFactionStandingsUltimatumConfirmation;
    private JCheckBox optionBeginTransitConfirmation;
    private JCheckBox optionStratConBatchallBreachConfirmation;
    private JCheckBox optionStratConDeployConfirmation;
    private JCheckBox optionAbandonUnitsConfirmation;

    // endregion Nag Tab

    // region Miscellaneous
    private JTextField txtUserDir;
    private JSpinner spnStartGameDelay;
    private JSpinner spnStartGameClientDelay;
    private JSpinner spnStartGameClientRetryCount;
    private JSpinner spnStartGameBotClientDelay;
    private JSpinner spnStartGameBotClientRetryCount;
    private MMComboBox<CompanyGenerationMethod> comboDefaultCompanyGenerationMethod;
    // endregion Miscellaneous
    // endregion Variable Declarations

    // region Constructors
    public MHQOptionsDialog(final JFrame frame) {
        super(frame, true, "MHQOptionsDialog", "MHQOptionsDialog.title");
        initialize();
        setInitialState();
    }
    // endregion Constructors

    // region Initialization

    /**
     * This dialog uses the following Mnemonics: C, D, M, M, S, U, W, Y
     */
    @Override
    protected Container createCenterPane() {
        JTabbedPane optionsTabbedPane = new JTabbedPane();
        optionsTabbedPane.setName("optionsTabbedPane");
        optionsTabbedPane.add(resources.getString("displayTab.title"), new FastJScrollPane(createDisplayTab()));
        optionsTabbedPane.add(resources.getString("coloursTab.title"), new FastJScrollPane(createColoursTab()));
        optionsTabbedPane.add(resources.getString("fontsTab.title"), new FastJScrollPane(createFontsTab()));
        optionsTabbedPane.add(resources.getString("autosaveTab.title"), new FastJScrollPane(createAutosaveTab()));
        optionsTabbedPane.add(resources.getString("newDayTab.title"), new FastJScrollPane(createNewDayTab()));
        optionsTabbedPane.add(resources.getString("campaignXMLSaveTab.title"),
              new FastJScrollPane(createCampaignXMLSaveTab()));
        optionsTabbedPane.add(resources.getString("nagTab.title"), new FastJScrollPane(createNagTab()));
        optionsTabbedPane.add(resources.getString("miscellaneousTab.title"),
              new FastJScrollPane(createMiscellaneousTab()));
        return optionsTabbedPane;
    }

    private JPanel createDisplayTab() {
        guiScale.setMajorTickSpacing(3);
        guiScale.setMinimum(7);
        guiScale.setMaximum(24);
        Hashtable<Integer, JComponent> table = new Hashtable<>();
        table.put(7, new JLabel("70%"));
        table.put(10, new JLabel("100%"));
        table.put(16, new JLabel("160%"));
        table.put(22, new JLabel("220%"));
        guiScale.setLabelTable(table);
        guiScale.setPaintTicks(true);
        guiScale.setPaintLabels(true);
        guiScale.setValue((int) (GUIPreferences.getInstance().getGUIScale() * 10));
        guiScale.setToolTipText(Messages.getString("CommonSettingsDialog.guiScaleTT"));
        JLabel guiScaleLabel = new JLabel(Messages.getString("CommonSettingsDialog.guiScale"));
        JPanel scaleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        scaleLine.add(guiScaleLabel);
        scaleLine.add(Box.createHorizontalStrut(5));
        scaleLine.add(guiScale);

        // Initialize Components Used in ActionListeners
        final JLabel lblInterstellarMapShowJumpRadiusMinimumZoom = new JLabel();
        final JLabel lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom = new JLabel();

        // Create Panel Components
        JLabel labelDisplayDateFormat = new JLabel(resources.getString("labelDisplayDateFormat.text"));
        JLabel labelDisplayDateFormatExample = new JLabel();
        optionDisplayDateFormat = new JTextField();
        optionDisplayDateFormat.addActionListener(evt -> labelDisplayDateFormatExample.setText(validateDateFormat(
              optionDisplayDateFormat.getText()) ?
                                                                                                     LocalDate.now()
                                                                                                           .format(
                                                                                                                 DateTimeFormatter.ofPattern(
                                                                                                                             optionDisplayDateFormat.getText())
                                                                                                                       .withLocale(
                                                                                                                             MekHQ.getMHQOptions()
                                                                                                                                   .getDateLocale())) :
                                                                                                     resources.getString(
                                                                                                           "invalidDateFormat.error")));

        JLabel labelLongDisplayDateFormat = new JLabel(resources.getString("labelLongDisplayDateFormat.text"));
        JLabel labelLongDisplayDateFormatExample = new JLabel();
        optionLongDisplayDateFormat = new JTextField();
        optionLongDisplayDateFormat.addActionListener(evt -> labelLongDisplayDateFormatExample.setText(
              validateDateFormat(optionLongDisplayDateFormat.getText()) ?
                    LocalDate.now()
                          .format(DateTimeFormatter.ofPattern(optionLongDisplayDateFormat.getText())
                                        .withLocale(MekHQ.getMHQOptions().getDateLocale())) :
                    resources.getString("invalidDateFormat.error")));

        optionHideUnitFluff = new JCheckBox(resources.getString("optionHideUnitFluff.text"));
        optionHideUnitFluff.setToolTipText(resources.getString("optionHideUnitFluff.toolTipText"));

        optionHistoricalDailyLog = new JCheckBox(resources.getString("optionHistoricalDailyLog.text"));
        optionHistoricalDailyLog.setToolTipText(resources.getString("optionHistoricalDailyLog.toolTipText"));

        chkCompanyGeneratorStartup = new JCheckBox(resources.getString("chkCompanyGeneratorStartup.text"));
        chkCompanyGeneratorStartup.setToolTipText(resources.getString("chkCompanyGeneratorStartup.toolTipText"));
        chkCompanyGeneratorStartup.setName("chkCompanyGeneratorStartup");

        chkShowCompanyGenerator = new JCheckBox(resources.getString("chkShowCompanyGenerator.text"));
        chkShowCompanyGenerator.setToolTipText(resources.getString("chkShowCompanyGenerator.toolTipText"));
        chkShowCompanyGenerator.setName("chkShowCompanyGenerator");

        chkShowUnitPicturesOnTOE = new JCheckBox(resources.getString("chkShowUnitPicturesOnTOE.text"));
        chkShowUnitPicturesOnTOE.setToolTipText(resources.getString("chkShowUnitPicturesOnTOE.toolTipText"));
        chkShowUnitPicturesOnTOE.setName("chkShowUnitPicturesOnTOE");

        // region Command Center Tab
        JLabel labelCommandCenterDisplay = new JLabel(resources.getString("labelCommandCenterDisplay.text"));

        optionCommandCenterMRMS = new JCheckBox(resources.getString("optionCommandCenterMRMS.text"));
        optionCommandCenterMRMS.setToolTipText(resources.getString("optionCommandCenterMRMS.toolTipText"));
        // endregion Command Center Tab

        // region Interstellar Map Tab
        final JLabel lblInterstellarMapTab = new JLabel(resources.getString("lblInterstellarMapTab.text"));
        lblInterstellarMapTab.setName("lblInterstellarMapTab");

        chkInterstellarMapShowJumpRadius = new JCheckBox(resources.getString("chkInterstellarMapShowJumpRadius.text"));
        chkInterstellarMapShowJumpRadius.setToolTipText(resources.getString(
              "chkInterstellarMapShowJumpRadius.toolTipText"));
        chkInterstellarMapShowJumpRadius.setName("chkInterstellarMapShowJumpRadius");
        chkInterstellarMapShowJumpRadius.addActionListener(evt -> {
            final boolean selected = chkInterstellarMapShowJumpRadius.isSelected();
            lblInterstellarMapShowJumpRadiusMinimumZoom.setEnabled(selected);
            spnInterstellarMapShowJumpRadiusMinimumZoom.setEnabled(selected);
            btnInterstellarMapJumpRadiusColour.setEnabled(selected);
        });

        lblInterstellarMapShowJumpRadiusMinimumZoom.setText(resources.getString(
              "lblInterstellarMapShowJumpRadiusMinimumZoom.text"));
        lblInterstellarMapShowJumpRadiusMinimumZoom.setToolTipText(resources.getString(
              "lblInterstellarMapShowJumpRadiusMinimumZoom.toolTipText"));
        lblInterstellarMapShowJumpRadiusMinimumZoom.setName("lblInterstellarMapShowJumpRadiusMinimumZoom");

        spnInterstellarMapShowJumpRadiusMinimumZoom = new JSpinner(new SpinnerNumberModel(3d, 0d, 10d, 0.5));
        spnInterstellarMapShowJumpRadiusMinimumZoom.setToolTipText(resources.getString(
              "lblInterstellarMapShowJumpRadiusMinimumZoom.toolTipText"));
        spnInterstellarMapShowJumpRadiusMinimumZoom.setName("spnInterstellarMapShowJumpRadiusMinimumZoom");

        btnInterstellarMapJumpRadiusColour = new ColourSelectorButton(resources.getString(
              "btnInterstellarMapJumpRadiusColour.text"));
        btnInterstellarMapJumpRadiusColour.setToolTipText(resources.getString(
              "btnInterstellarMapJumpRadiusColour.toolTipText"));
        btnInterstellarMapJumpRadiusColour.setName("btnInterstellarMapJumpRadiusColour");

        chkInterstellarMapShowPlanetaryAcquisitionRadius = new JCheckBox(resources.getString(
              "chkInterstellarMapShowPlanetaryAcquisitionRadius.text"));
        chkInterstellarMapShowPlanetaryAcquisitionRadius.setToolTipText(resources.getString(
              "chkInterstellarMapShowPlanetaryAcquisitionRadius.toolTipText"));
        chkInterstellarMapShowPlanetaryAcquisitionRadius.setName("chkInterstellarMapShowPlanetaryAcquisitionRadius");
        chkInterstellarMapShowPlanetaryAcquisitionRadius.addActionListener(evt -> {
            final boolean selected = chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected();
            lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setEnabled(selected);
            spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setEnabled(selected);
            btnInterstellarMapPlanetaryAcquisitionRadiusColour.setEnabled(selected);
        });

        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setText(resources.getString(
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.text"));
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setToolTipText(resources.getString(
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.toolTipText"));
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setName(
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom");

        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom = new JSpinner(new SpinnerNumberModel(2d,
              0d,
              10d,
              0.5));
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setToolTipText(resources.getString(
              "lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.toolTipText"));
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setName(
              "spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom");

        btnInterstellarMapPlanetaryAcquisitionRadiusColour = new ColourSelectorButton(resources.getString(
              "btnInterstellarMapPlanetaryAcquisitionRadiusColour.text"));
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setToolTipText(resources.getString(
              "btnInterstellarMapPlanetaryAcquisitionRadiusColour.toolTipText"));
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setName("btnInterstellarMapPlanetaryAcquisitionRadiusColour");

        chkInterstellarMapShowContractSearchRadius = new JCheckBox(resources.getString(
              "chkInterstellarMapShowContractSearchRadius.text"));
        chkInterstellarMapShowContractSearchRadius.setToolTipText(resources.getString(
              "chkInterstellarMapShowContractSearchRadius.toolTipText"));
        chkInterstellarMapShowContractSearchRadius.setName("chkInterstellarMapShowContractSearchRadius");
        chkInterstellarMapShowContractSearchRadius.addActionListener(evt -> btnInterstellarMapContractSearchRadiusColour.setEnabled(
              chkInterstellarMapShowContractSearchRadius.isSelected()));

        btnInterstellarMapContractSearchRadiusColour = new ColourSelectorButton(resources.getString(
              "btnInterstellarMapContractSearchRadiusColour.text"));
        btnInterstellarMapContractSearchRadiusColour.setToolTipText(resources.getString(
              "btnInterstellarMapContractSearchRadiusColour.toolTipText"));
        btnInterstellarMapContractSearchRadiusColour.setName("btnInterstellarMapContractSearchRadiusColour");
        // endregion Interstellar Map Tab

        // region Personnel Tab
        JLabel labelPersonnelDisplay = new JLabel(resources.getString("labelPersonnelDisplay.text"));

        JLabel labelPersonnelFilterStyle = new JLabel(resources.getString("optionPersonnelFilterStyle.text"));
        labelPersonnelFilterStyle.setToolTipText(resources.getString("optionPersonnelFilterStyle.toolTipText"));

        optionPersonnelFilterStyle = new JComboBox<>(PersonnelFilterStyle.values());
        optionPersonnelFilterStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PersonnelFilterStyle) {
                    list.setToolTipText(((PersonnelFilterStyle) value).getToolTipText());
                }
                return this;
            }
        });

        optionPersonnelFilterOnPrimaryRole = new JCheckBox(resources.getString("optionPersonnelFilterOnPrimaryRole.text"));

        chkUnifiedDailyReport = new JCheckBox(resources.getString("chkUnifiedDailyReport.text"));
        chkUnifiedDailyReport.setToolTipText(resources.getString("chkUnifiedDailyReport.toolTipText"));
        chkUnifiedDailyReport.setName("chkUnifiedDailyReport");
        // endregion Personnel Tab

        // Programmatically Assign Accessibility Labels
        lblInterstellarMapShowJumpRadiusMinimumZoom.setLabelFor(spnInterstellarMapShowJumpRadiusMinimumZoom);
        lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setLabelFor(
              spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom);

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

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(labelDisplayDateFormat)
                                                      .addComponent(optionDisplayDateFormat)
                                                      .addComponent(labelDisplayDateFormatExample, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(labelLongDisplayDateFormat)
                                                      .addComponent(optionLongDisplayDateFormat)
                                                      .addComponent(labelLongDisplayDateFormatExample,
                                                            Alignment.TRAILING))
                                      .addComponent(scaleLine)
                                      .addComponent(optionHideUnitFluff)
                                      .addComponent(optionHistoricalDailyLog)
                                      .addComponent(chkCompanyGeneratorStartup)
                                      .addComponent(chkShowCompanyGenerator)
                                      .addComponent(chkShowUnitPicturesOnTOE)
                                      .addComponent(labelCommandCenterDisplay)
                                      .addComponent(optionCommandCenterMRMS)
                                      .addComponent(lblInterstellarMapTab)
                                      .addComponent(chkInterstellarMapShowJumpRadius)
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblInterstellarMapShowJumpRadiusMinimumZoom)
                                                      .addComponent(spnInterstellarMapShowJumpRadiusMinimumZoom,
                                                            Alignment.TRAILING))
                                      .addComponent(btnInterstellarMapJumpRadiusColour)
                                      .addComponent(chkInterstellarMapShowPlanetaryAcquisitionRadius)
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(
                                                            lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom)
                                                      .addComponent(
                                                            spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom,
                                                            Alignment.TRAILING))
                                      .addComponent(btnInterstellarMapPlanetaryAcquisitionRadiusColour)
                                      .addComponent(chkInterstellarMapShowContractSearchRadius)
                                      .addComponent(btnInterstellarMapContractSearchRadiusColour)
                                      .addComponent(labelPersonnelDisplay)
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(labelPersonnelFilterStyle)
                                                      .addComponent(optionPersonnelFilterStyle,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addComponent(optionPersonnelFilterOnPrimaryRole)
                                      .addComponent(chkUnifiedDailyReport));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(labelDisplayDateFormat)
                                                        .addComponent(optionDisplayDateFormat)
                                                        .addComponent(labelDisplayDateFormatExample))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(labelLongDisplayDateFormat)
                                                        .addComponent(optionLongDisplayDateFormat)
                                                        .addComponent(labelLongDisplayDateFormatExample))
                                        .addComponent(scaleLine)
                                        .addComponent(optionHideUnitFluff)
                                        .addComponent(optionHistoricalDailyLog)
                                        .addComponent(chkCompanyGeneratorStartup)
                                        .addComponent(chkShowCompanyGenerator)
                                        .addComponent(chkShowUnitPicturesOnTOE)
                                        .addComponent(labelCommandCenterDisplay)
                                        .addComponent(optionCommandCenterMRMS)
                                        .addComponent(lblInterstellarMapTab)
                                        .addComponent(chkInterstellarMapShowJumpRadius)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(lblInterstellarMapShowJumpRadiusMinimumZoom)
                                                        .addComponent(spnInterstellarMapShowJumpRadiusMinimumZoom))
                                        .addComponent(btnInterstellarMapJumpRadiusColour)
                                        .addComponent(chkInterstellarMapShowPlanetaryAcquisitionRadius)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(
                                                              lblInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom)
                                                        .addComponent(
                                                              spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom))
                                        .addComponent(btnInterstellarMapPlanetaryAcquisitionRadiusColour)
                                        .addComponent(chkInterstellarMapShowContractSearchRadius)
                                        .addComponent(btnInterstellarMapContractSearchRadiusColour)
                                        .addComponent(labelPersonnelDisplay)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(labelPersonnelFilterStyle)
                                                        .addComponent(optionPersonnelFilterStyle))
                                        .addComponent(optionPersonnelFilterOnPrimaryRole)
                                        .addComponent(chkUnifiedDailyReport));

        return body;
    }

    private JPanel createColoursTab() {
        // region Create Graphical Components
        optionDeployedForeground = new ColourSelectorButton(resources.getString("optionDeployedForeground.text"));

        optionDeployedBackground = new ColourSelectorButton(resources.getString("optionDeployedBackground.text"));

        optionBelowContractMinimumForeground = new ColourSelectorButton(resources.getString(
              "optionBelowContractMinimumForeground.text"));

        optionBelowContractMinimumBackground = new ColourSelectorButton(resources.getString(
              "optionBelowContractMinimumBackground.text"));

        optionInTransitForeground = new ColourSelectorButton(resources.getString("optionInTransitForeground.text"));

        optionInTransitBackground = new ColourSelectorButton(resources.getString("optionInTransitBackground.text"));

        optionRefittingForeground = new ColourSelectorButton(resources.getString("optionRefittingForeground.text"));

        optionRefittingBackground = new ColourSelectorButton(resources.getString("optionRefittingBackground.text"));

        optionMothballingForeground = new ColourSelectorButton(resources.getString("optionMothballingForeground.text"));

        optionMothballingBackground = new ColourSelectorButton(resources.getString("optionMothballingBackground.text"));

        optionMothballedForeground = new ColourSelectorButton(resources.getString("optionMothballedForeground.text"));

        optionMothballedBackground = new ColourSelectorButton(resources.getString("optionMothballedBackground.text"));

        optionNotRepairableForeground = new ColourSelectorButton(resources.getString(
              "optionNotRepairableForeground.text"));

        optionNotRepairableBackground = new ColourSelectorButton(resources.getString(
              "optionNotRepairableBackground.text"));

        optionNonFunctionalForeground = new ColourSelectorButton(resources.getString(
              "optionNonFunctionalForeground.text"));

        optionNonFunctionalBackground = new ColourSelectorButton(resources.getString(
              "optionNonFunctionalBackground.text"));

        optionNeedsPartsFixedForeground = new ColourSelectorButton(resources.getString(
              "optionNeedsPartsFixedForeground.text"));

        optionNeedsPartsFixedBackground = new ColourSelectorButton(resources.getString(
              "optionNeedsPartsFixedBackground.text"));

        optionUnmaintainedForeground = new ColourSelectorButton(resources.getString("optionUnmaintainedForeground.text"));

        optionUnmaintainedBackground = new ColourSelectorButton(resources.getString("optionUnmaintainedBackground.text"));

        optionUncrewedForeground = new ColourSelectorButton(resources.getString("optionUncrewedForeground.text"));

        optionUncrewedBackground = new ColourSelectorButton(resources.getString("optionUncrewedBackground.text"));

        optionLoanOverdueForeground = new ColourSelectorButton(resources.getString("optionLoanOverdueForeground.text"));

        optionLoanOverdueBackground = new ColourSelectorButton(resources.getString("optionLoanOverdueBackground.text"));

        optionInjuredForeground = new ColourSelectorButton(resources.getString("optionInjuredForeground.text"));

        optionInjuredBackground = new ColourSelectorButton(resources.getString("optionInjuredBackground.text"));

        optionHealedInjuriesForeground = new ColourSelectorButton(resources.getString(
              "optionHealedInjuriesForeground.text"));

        optionHealedInjuriesBackground = new ColourSelectorButton(resources.getString(
              "optionHealedInjuriesBackground.text"));

        optionPregnantForeground = new ColourSelectorButton(resources.getString("optionPregnantForeground.text"));

        optionPregnantBackground = new ColourSelectorButton(resources.getString("optionPregnantBackground.text"));

        optionGoneForeground = new ColourSelectorButton(resources.getString("optionGoneForeground.text"));

        optionGoneBackground = new ColourSelectorButton(resources.getString("optionGoneBackground.text"));

        optionAbsentForeground = new ColourSelectorButton(resources.getString("optionAbsentForeground.text"));

        optionAbsentBackground = new ColourSelectorButton(resources.getString("optionAbsentBackground.text"));

        optionFatiguedForeground = new ColourSelectorButton(resources.getString("optionFatiguedForeground.text"));

        optionFatiguedBackground = new ColourSelectorButton(resources.getString("optionFatiguedBackground.text"));

        optionStratConHexCoordForeground = new ColourSelectorButton(resources.getString(
              "optionStratConHexCoordForeground.text"));

        optionFontColorNegative = new ColourSelectorButton(resources.getString("optionFontColorNegative.text"));

        optionFontColorWarning = new ColourSelectorButton(resources.getString("optionFontColorWarning.text"));

        optionFontColorPositive = new ColourSelectorButton(resources.getString("optionFontColorPositive.text"));
        optionFontColorAmazing = new ColourSelectorButton(resources.getString("optionFontColorAmazing.text"));

        optionFontColorSkillUltraGreen = new ColourSelectorButton(resources.getString(
              "optionFontColorSkillUltraGreen.text"));

        optionFontColorSkillGreen = new ColourSelectorButton(resources.getString("optionFontColorSkillGreen.text"));

        optionFontColorSkillRegular = new ColourSelectorButton(resources.getString("optionFontColorSkillRegular.text"));

        optionFontColorSkillVeteran = new ColourSelectorButton(resources.getString("optionFontColorSkillVeteran.text"));

        optionFontColorSkillElite = new ColourSelectorButton(resources.getString("optionFontColorSkillElite.text"));

        // endregion Create Graphical Components

        // region Layout
        //  the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionDeployedForeground)
                                                      .addComponent(optionDeployedBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionBelowContractMinimumForeground)
                                                      .addComponent(optionBelowContractMinimumBackground,
                                                            Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionInTransitForeground)
                                                      .addComponent(optionInTransitBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionRefittingForeground)
                                                      .addComponent(optionRefittingBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionMothballingForeground)
                                                      .addComponent(optionMothballingBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionMothballedForeground)
                                                      .addComponent(optionMothballedBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionNotRepairableForeground)
                                                      .addComponent(optionNotRepairableBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionNonFunctionalForeground)
                                                      .addComponent(optionNonFunctionalBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionNeedsPartsFixedForeground)
                                                      .addComponent(optionNeedsPartsFixedBackground,
                                                            Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionUnmaintainedForeground)
                                                      .addComponent(optionUnmaintainedBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionUncrewedForeground)
                                                      .addComponent(optionUncrewedBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionLoanOverdueForeground)
                                                      .addComponent(optionLoanOverdueBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionInjuredForeground)
                                                      .addComponent(optionInjuredBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionHealedInjuriesForeground)
                                                      .addComponent(optionHealedInjuriesBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionPregnantForeground)
                                                      .addComponent(optionPregnantBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionGoneForeground)
                                                      .addComponent(optionGoneBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionAbsentForeground)
                                                      .addComponent(optionAbsentBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFatiguedForeground)
                                                      .addComponent(optionFatiguedBackground, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionStratConHexCoordForeground))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFontColorAmazing)
                                                      .addComponent(optionFontColorPositive, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFontColorNegative)
                                                      .addComponent(optionFontColorWarning, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFontColorSkillUltraGreen)
                                                      .addComponent(optionFontColorSkillGreen, Alignment.TRAILING))
                                      .addComponent(optionFontColorSkillRegular)
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFontColorSkillRegular)
                                                      .addComponent(optionFontColorSkillVeteran, Alignment.TRAILING))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(optionFontColorSkillElite, Alignment.TRAILING)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
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
                                                        .addComponent(optionGoneForeground)
                                                        .addComponent(optionGoneBackground))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionAbsentForeground)
                                                        .addComponent(optionAbsentBackground))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionFatiguedForeground)
                                                        .addComponent(optionFatiguedBackground))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionStratConHexCoordForeground))
                                        .addGroup(layout.createSequentialGroup().addComponent(optionFontColorAmazing)
                                                        .addComponent(optionFontColorPositive))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionFontColorNegative)
                                                        .addComponent(optionFontColorWarning))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionFontColorSkillUltraGreen)
                                                        .addComponent(optionFontColorSkillGreen))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionFontColorSkillRegular)
                                                        .addComponent(optionFontColorSkillVeteran))
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(optionFontColorSkillElite)));
        // endregion Layout

        return body;
    }

    private JPanel createFontsTab() {
        // Create Panel Components
        final JLabel lblMedicalViewDialogHandwritingFont = new JLabel(resources.getString(
              "lblMedicalViewDialogHandwritingFont.text"));
        lblMedicalViewDialogHandwritingFont.setToolTipText(resources.getString(
              "lblMedicalViewDialogHandwritingFont.toolTipText"));
        lblMedicalViewDialogHandwritingFont.setName("lblMedicalViewDialogHandwritingFont");

        comboMedicalViewDialogHandwritingFont = new FontComboBox("comboMedicalViewDialogHandwritingFont");
        comboMedicalViewDialogHandwritingFont.setToolTipText(resources.getString(
              "lblMedicalViewDialogHandwritingFont.toolTipText"));

        // Programmatically Assign Accessibility Labels
        lblMedicalViewDialogHandwritingFont.setLabelFor(comboMedicalViewDialogHandwritingFont);

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("fontPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                      .addComponent(lblMedicalViewDialogHandwritingFont)
                                                      .addComponent(comboMedicalViewDialogHandwritingFont,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(lblMedicalViewDialogHandwritingFont)
                                                        .addComponent(comboMedicalViewDialogHandwritingFont)));

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

        checkSaveBeforeScenarios = new JCheckBox(resources.getString("checkSaveBeforeScenarios.text"));
        checkSaveBeforeScenarios.setMnemonic(KeyEvent.VK_S);

        checkSaveBeforeContractEnd = new JCheckBox(resources.getString("checkSaveBeforeMissionEnd.text"));

        JLabel labelSavedGamesCount = new JLabel(resources.getString("labelSavedGamesCount.text"));
        spinnerSavedGamesCount = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        labelSavedGamesCount.setLabelFor(spinnerSavedGamesCount);

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(optionNoSave)
                                      .addComponent(optionSaveDaily)
                                      .addComponent(optionSaveWeekly)
                                      .addComponent(optionSaveMonthly)
                                      .addComponent(optionSaveYearly)
                                      .addComponent(checkSaveBeforeScenarios)
                                      .addComponent(checkSaveBeforeContractEnd)
                                      .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                      .addComponent(labelSavedGamesCount)
                                                      .addComponent(spinnerSavedGamesCount,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(optionNoSave)
                                        .addComponent(optionSaveDaily)
                                        .addComponent(optionSaveWeekly)
                                        .addComponent(optionSaveMonthly)
                                        .addComponent(optionSaveYearly)
                                        .addComponent(checkSaveBeforeScenarios)
                                        .addComponent(checkSaveBeforeContractEnd)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(labelSavedGamesCount)
                                                        .addComponent(spinnerSavedGamesCount)));

        return body;
    }

    private JPanel createNewDayTab() {
        // Initialize Components Used in ActionListeners
        final JLabel lblNewDayFormationIconOperationalStatusStyle = new JLabel(resources.getString(
              "lblNewDayFormationIconOperationalStatusStyle.text"));

        // Create Panel Components
        chkNewDayAsTechPoolFill = new JCheckBox(resources.getString("chkNewDayAstechPoolFill.text"));
        chkNewDayAsTechPoolFill.setToolTipText(resources.getString("chkNewDayAstechPoolFill.toolTipText"));
        chkNewDayAsTechPoolFill.setName("chkNewDayAsTechPoolFill");

        chkNewDayMedicPoolFill = new JCheckBox(resources.getString("chkNewDayMedicPoolFill.text"));
        chkNewDayMedicPoolFill.setToolTipText(resources.getString("chkNewDayMedicPoolFill.toolTipText"));
        chkNewDayMedicPoolFill.setName("chkNewDayMedicPoolFill");

        chkNewDaySoldierPoolFill = new JCheckBox(resources.getString("chkNewDaySoldierPoolFill.text"));
        chkNewDaySoldierPoolFill.setToolTipText(resources.getString("chkNewDaySoldierPoolFill.toolTipText"));
        chkNewDaySoldierPoolFill.setName("chkNewDaySoldierPoolFill");

        chkNewDayBattleArmorPoolFill = new JCheckBox(resources.getString("chkNewDayBattleArmorPoolFill.text"));
        chkNewDayBattleArmorPoolFill.setToolTipText(resources.getString("chkNewDayBattleArmorPoolFill.toolTipText"));
        chkNewDayBattleArmorPoolFill.setName("chkNewDayBattleArmorPoolFill");

        chkNewDayVehicleCrewGroundPoolFill = new JCheckBox(resources.getString("chkNewDayVehicleCrewGroundPoolFill.text"));
        chkNewDayVehicleCrewGroundPoolFill.setToolTipText(resources.getString("chkNewDayVehicleCrewGroundPoolFill.toolTipText"));
        chkNewDayVehicleCrewGroundPoolFill.setName("chkNewDayVehicleCrewGroundPoolFill");

        chkNewDayVehicleCrewVTOLPoolFill = new JCheckBox(resources.getString("chkNewDayVehicleCrewVTOLPoolFill.text"));
        chkNewDayVehicleCrewVTOLPoolFill.setToolTipText(resources.getString("chkNewDayVehicleCrewVTOLPoolFill.toolTipText"));
        chkNewDayVehicleCrewVTOLPoolFill.setName("chkNewDayVehicleCrewVTOLPoolFill");

        chkNewDayVehicleCrewNavalPoolFill = new JCheckBox(resources.getString("chkNewDayVehicleCrewNavalPoolFill.text"));
        chkNewDayVehicleCrewNavalPoolFill.setToolTipText(resources.getString("chkNewDayVehicleCrewNavalPoolFill.toolTipText"));
        chkNewDayVehicleCrewNavalPoolFill.setName("chkNewDayVehicleCrewNavalPoolFill");

        chkNewDayVesselPilotPoolFill = new JCheckBox(resources.getString("chkNewDayVesselPilotPoolFill.text"));
        chkNewDayVesselPilotPoolFill.setToolTipText(resources.getString("chkNewDayVesselPilotPoolFill.toolTipText"));
        chkNewDayVesselPilotPoolFill.setName("chkNewDayVesselPilotPoolFill");

        chkNewDayVesselGunnerPoolFill = new JCheckBox(resources.getString("chkNewDayVesselGunnerPoolFill.text"));
        chkNewDayVesselGunnerPoolFill.setToolTipText(resources.getString("chkNewDayVesselGunnerPoolFill.toolTipText"));
        chkNewDayVesselGunnerPoolFill.setName("chkNewDayVesselGunnerPoolFill");

        chkNewDayVesselCrewPoolFill = new JCheckBox(resources.getString("chkNewDayVesselCrewPoolFill.text"));
        chkNewDayVesselCrewPoolFill.setToolTipText(resources.getString("chkNewDayVesselCrewPoolFill.toolTipText"));
        chkNewDayVesselCrewPoolFill.setName("chkNewDayVesselCrewPoolFill");

        chkNewDayMRMS = new JCheckBox(resources.getString("chkNewDayMRMS.text"));
        chkNewDayMRMS.setToolTipText(resources.getString("chkNewDayMRMS.toolTipText"));
        chkNewDayMRMS.setName("chkNewDayMRMS");

        chkNewDayOptimizeMedicalAssignments = new JCheckBox(resources.getString(
              "chkNewDayOptimizeMedicalAssignments.text"));
        chkNewDayOptimizeMedicalAssignments.setToolTipText(resources.getString(
              "chkNewDayOptimizeMedicalAssignments.toolTipText"));
        chkNewDayOptimizeMedicalAssignments.setName("chkNewDayOptimizeMedicalAssignments.text");

        chkNewDayAutomaticallyAssignUnmaintainedUnits = new JCheckBox(resources.getString(
              "chkNewDayAutomaticallyAssignUnmaintainedUnits.text"));
        chkNewDayAutomaticallyAssignUnmaintainedUnits.setToolTipText(wordWrap(resources.getString(
              "chkNewDayAutomaticallyAssignUnmaintainedUnits.toolTipText")));
        chkNewDayAutomaticallyAssignUnmaintainedUnits.setName("chkNewDayAutomaticallyAssignUnmaintainedUnits.text");

        chkNewMonthQuickTrain = new JCheckBox(resources.getString(
              "chkNewMonthQuickTrain.text"));
        chkNewMonthQuickTrain.setToolTipText(resources.getString(
              "chkNewMonthQuickTrain.toolTipText"));
        chkNewMonthQuickTrain.setName("chkNewMonthQuickTrain.text");

        chkSelfCorrectMaintenance = new JCheckBox(resources.getString(
              "chkSelfCorrectMaintenance.text"));
        chkSelfCorrectMaintenance.setToolTipText(resources.getString(
              "chkSelfCorrectMaintenance.toolTipText"));
        chkSelfCorrectMaintenance.setName("chkSelfCorrectMaintenance.text");

        chkNewDayFormationIconOperationalStatus = new JCheckBox(resources.getString(
              "chkNewDayFormationIconOperationalStatus.text"));
        chkNewDayFormationIconOperationalStatus.setToolTipText(resources.getString(
              "chkNewDayFormationIconOperationalStatus.toolTipText"));
        chkNewDayFormationIconOperationalStatus.setName("chkNewDayFormationIconOperationalStatus");
        chkNewDayFormationIconOperationalStatus.addActionListener(evt -> {
            final boolean selected = chkNewDayFormationIconOperationalStatus.isSelected();
            lblNewDayFormationIconOperationalStatusStyle.setEnabled(selected);
            comboNewDayFormationIconOperationalStatusStyle.setEnabled(selected);
        });

        lblNewDayFormationIconOperationalStatusStyle.setToolTipText(resources.getString(
              "lblNewDayFormationIconOperationalStatusStyle.toolTipText"));
        lblNewDayFormationIconOperationalStatusStyle.setName("lblNewDayFormationIconOperationalStatusStyle");

        comboNewDayFormationIconOperationalStatusStyle = new MMComboBox<>("comboNewDayFormationIconOperationalStatusStyle",
              FormationIconOperationalStatusStyle.values());
        comboNewDayFormationIconOperationalStatusStyle.setToolTipText(resources.getString(
              "lblNewDayFormationIconOperationalStatusStyle.toolTipText"));
        comboNewDayFormationIconOperationalStatusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FormationIconOperationalStatusStyle) {
                    list.setToolTipText(((FormationIconOperationalStatusStyle) value).getToolTipText());
                }
                return this;
            }
        });

        // Programmatically Assign Accessibility Labels
        lblNewDayFormationIconOperationalStatusStyle.setLabelFor(comboNewDayFormationIconOperationalStatusStyle);

        // Disable Panel Portions by Default
        chkNewDayFormationIconOperationalStatus.setSelected(true);
        chkNewDayFormationIconOperationalStatus.doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("newDayPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(chkNewDayAsTechPoolFill)
                                      .addComponent(chkNewDayMedicPoolFill)
                                      .addComponent(chkNewDaySoldierPoolFill)
                                      .addComponent(chkNewDayBattleArmorPoolFill)
                                      .addComponent(chkNewDayVehicleCrewGroundPoolFill)
                                      .addComponent(chkNewDayVehicleCrewVTOLPoolFill)
                                      .addComponent(chkNewDayVehicleCrewNavalPoolFill)
                                      .addComponent(chkNewDayVesselPilotPoolFill)
                                      .addComponent(chkNewDayVesselGunnerPoolFill)
                                      .addComponent(chkNewDayVesselCrewPoolFill)
                                      .addComponent(chkNewDayMRMS)
                                      .addComponent(chkNewDayOptimizeMedicalAssignments)
                                      .addComponent(chkNewDayAutomaticallyAssignUnmaintainedUnits)
                                      .addComponent(chkNewMonthQuickTrain)
                                      .addComponent(chkSelfCorrectMaintenance)
                                      .addComponent(chkNewDayFormationIconOperationalStatus)
                                      .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                      .addComponent(lblNewDayFormationIconOperationalStatusStyle)
                                                      .addComponent(comboNewDayFormationIconOperationalStatusStyle,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(chkNewDayAsTechPoolFill)
                                        .addComponent(chkNewDayMedicPoolFill)
                                        .addComponent(chkNewDaySoldierPoolFill)
                                        .addComponent(chkNewDayBattleArmorPoolFill)
                                        .addComponent(chkNewDayVehicleCrewGroundPoolFill)
                                        .addComponent(chkNewDayVehicleCrewVTOLPoolFill)
                                        .addComponent(chkNewDayVehicleCrewNavalPoolFill)
                                        .addComponent(chkNewDayVesselPilotPoolFill)
                                        .addComponent(chkNewDayVesselGunnerPoolFill)
                                        .addComponent(chkNewDayVesselCrewPoolFill)
                                        .addComponent(chkNewDayMRMS)
                                        .addComponent(chkNewDayOptimizeMedicalAssignments)
                                        .addComponent(chkNewDayAutomaticallyAssignUnmaintainedUnits)
                                        .addComponent(chkNewMonthQuickTrain)
                                        .addComponent(chkSelfCorrectMaintenance)
                                        .addComponent(chkNewDayFormationIconOperationalStatus)
                                        .addGroup(layout.createSequentialGroup()
                                                        .addComponent(lblNewDayFormationIconOperationalStatusStyle)
                                                        .addComponent(comboNewDayFormationIconOperationalStatusStyle)));

        return panel;
    }

    private JPanel createCampaignXMLSaveTab() {
        // Create Panel Components
        optionPreferGzippedOutput = new JCheckBox(resources.getString("optionPreferGzippedOutput.text"));
        optionPreferGzippedOutput.setToolTipText(resources.getString("optionPreferGzippedOutput.toolTipText"));

        optionWriteCustomsToXML = new JCheckBox(resources.getString("optionWriteCustomsToXML.text"));
        optionWriteCustomsToXML.setMnemonic(KeyEvent.VK_C);

        optionWriteAllUnitsToXML = new JCheckBox(resources.getString("optionWriteAllUnitsToXML.text"));
        optionWriteAllUnitsToXML.setMnemonic(KeyEvent.VK_A);

        optionSaveMothballState = new JCheckBox(resources.getString("optionSaveMothballState.text"));
        optionSaveMothballState.setToolTipText(resources.getString("optionSaveMothballState.toolTipText"));
        optionSaveMothballState.setMnemonic(KeyEvent.VK_U);

        // Layout the UI
        JPanel body = new JPanel();
        GroupLayout layout = new GroupLayout(body);
        body.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(optionPreferGzippedOutput)
                                      .addComponent(optionWriteCustomsToXML)
                                      .addComponent(optionWriteAllUnitsToXML)
                                      .addComponent(optionSaveMothballState));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(optionPreferGzippedOutput)
                                        .addComponent(optionWriteCustomsToXML)
                                        .addComponent(optionWriteAllUnitsToXML)
                                        .addComponent(optionSaveMothballState));

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

        optionHRStrainNag = new JCheckBox(resources.getString("optionAdminStrainNag.text"));
        optionHRStrainNag.setToolTipText(resources.getString("optionAdminStrainNag.toolTipText"));
        optionHRStrainNag.setName("optionAdminStrainNag");

        optionUntreatedPersonnelNag = new JCheckBox(resources.getString("optionUntreatedPersonnelNag.text"));
        optionUntreatedPersonnelNag.setToolTipText(resources.getString("optionUntreatedPersonnelNag.toolTipText"));
        optionUntreatedPersonnelNag.setName("optionUntreatedPersonnelNag");

        optionNoCommanderNag = new JCheckBox(resources.getString("optionNoCommanderNag.text"));
        optionNoCommanderNag.setToolTipText(resources.getString("optionNoCommanderNag.toolTipText"));
        optionNoCommanderNag.setName("optionNoCommanderNag");

        optionContractEndedNag = new JCheckBox(resources.getString("optionContractEndedNag.text"));
        optionContractEndedNag.setToolTipText(resources.getString("optionContractEndedNag.toolTipText"));
        optionContractEndedNag.setName("optionContractEndedNag");

        optionSingleDropNag = new JCheckBox(resources.getString("optionSingleDropNag.text"));
        optionSingleDropNag.setToolTipText(resources.getString("optionSingleDropNag.toolTipText"));
        optionSingleDropNag.setName("optionSingleDropNag");

        optionInsufficientAsTechsNag = new JCheckBox(resources.getString("optionInsufficientAstechsNag.text"));
        optionInsufficientAsTechsNag.setToolTipText(resources.getString("optionInsufficientAstechsNag.toolTipText"));
        optionInsufficientAsTechsNag.setName("optionInsufficientAsTechsNag");

        optionInsufficientAsTechTimeNag = new JCheckBox(resources.getString("optionInsufficientAstechTimeNag.text"));
        optionInsufficientAsTechTimeNag.setToolTipText(resources.getString("optionInsufficientAstechTimeNag.toolTipText"));
        optionInsufficientAsTechTimeNag.setName("optionInsufficientAsTechTimeNag");

        optionInsufficientMedicsNag = new JCheckBox(resources.getString("optionInsufficientMedicsNag.text"));
        optionInsufficientMedicsNag.setToolTipText(resources.getString("optionInsufficientMedicsNag.toolTipText"));
        optionInsufficientMedicsNag.setName("optionInsufficientMedicsNag");

        optionShortDeploymentNag = new JCheckBox(resources.getString("optionShortDeploymentNag.text"));
        optionShortDeploymentNag.setToolTipText(resources.getString("optionShortDeploymentNag.toolTipText"));
        optionShortDeploymentNag.setName("optionShortDeploymentNag");

        optionCombatChallengeNag = new JCheckBox(resources.getString("optionCombatChallengeNag.text"));
        optionCombatChallengeNag.setToolTipText(resources.getString("optionCombatChallengeNag.toolTipText"));
        optionCombatChallengeNag.setName("optionCombatChallengeNag");

        optionUnresolvedStratConContactsNag = new JCheckBox(resources.getString(
              "optionUnresolvedStratConContactsNag.text"));
        optionUnresolvedStratConContactsNag.setToolTipText(resources.getString(
              "optionUnresolvedStratConContactsNag.toolTipText"));
        optionUnresolvedStratConContactsNag.setName("optionUnresolvedStratConContactsNag");

        optionOutstandingScenariosNag = new JCheckBox(resources.getString("optionOutstandingScenariosNag.text"));
        optionOutstandingScenariosNag.setToolTipText(resources.getString("optionOutstandingScenariosNag.toolTipText"));
        optionOutstandingScenariosNag.setName("optionOutstandingScenariosNag");

        optionInvalidFactionNag = new JCheckBox(resources.getString("optionInvalidFactionNag.text"));
        optionInvalidFactionNag.setToolTipText(resources.getString("optionInvalidFactionNag.toolTipText"));
        optionInvalidFactionNag.setName("optionInvalidFactionNag");

        optionUnableToAffordExpensesNag = new JCheckBox(resources.getString("optionUnableToAffordExpensesNag.text"));
        optionUnableToAffordExpensesNag.setToolTipText(resources.getString("optionUnableToAffordExpensesNag.toolTipText"));
        optionUnableToAffordExpensesNag.setName("optionUnableToAffordExpensesNag");

        optionUnableToAffordRentNag = new JCheckBox(resources.getString("optionUnableToAffordRentNag.text"));
        optionUnableToAffordRentNag.setToolTipText(resources.getString("optionUnableToAffordRentNag.toolTipText"));
        optionUnableToAffordRentNag.setName("optionUnableToAffordRentNag");

        optionUnableToAffordLoanPaymentNag = new JCheckBox(resources.getString("optionUnableToAffordLoanPaymentNag.text"));
        optionUnableToAffordLoanPaymentNag.setToolTipText(resources.getString(
              "optionUnableToAffordLoanPaymentNag.toolTipText"));
        optionUnableToAffordLoanPaymentNag.setName("optionUnableToAffordLoanPaymentNag");

        optionUnableToAffordJumpNag = new JCheckBox(resources.getString("optionUnableToAffordJumpNag.text"));
        optionUnableToAffordJumpNag.setToolTipText(resources.getString("optionUnableToAffordJumpNag.toolTipText"));
        optionUnableToAffordJumpNag.setName("optionUnableToAffordJumpNag");

        optionUnableToAffordShoppingListNag = new JCheckBox(resources.getString(
              "optionUnableToAffordShoppingListNag.text"));
        optionUnableToAffordShoppingListNag.setToolTipText(resources.getString(
              "optionUnableToAffordShoppingListNag.toolTipText"));
        optionUnableToAffordShoppingListNag.setName("optionUnableToAffordShoppingListNag");

        optionContractRentalConfirmation = new JCheckBox(resources.getString(
              "optionContractRentalConfirmation.text"));
        optionContractRentalConfirmation.setToolTipText(resources.getString(
              "optionContractRentalConfirmation.toolTipText"));
        optionContractRentalConfirmation.setName("optionContractRentalConfirmation");

        optionFactionStandingsUltimatumConfirmation = new JCheckBox(resources.getString(
              "optionFactionStandingsUltimatumConfirmation.text"));
        optionFactionStandingsUltimatumConfirmation.setToolTipText(resources.getString(
              "optionFactionStandingsUltimatumConfirmation.toolTipText"));
        optionFactionStandingsUltimatumConfirmation.setName("optionFactionStandingsUltimatumConfirmation");

        optionBeginTransitConfirmation = new JCheckBox(resources.getString(
              "optionBeginTransitConfirmation.text"));
        optionBeginTransitConfirmation.setToolTipText(resources.getString(
              "optionBeginTransitConfirmation.toolTipText"));
        optionBeginTransitConfirmation.setName("optionBeginTransitConfirmation");

        optionStratConBatchallBreachConfirmation = new JCheckBox(resources.getString(
              "optionStratConBatchallBreachConfirmation.text"));
        optionStratConBatchallBreachConfirmation.setToolTipText(resources.getString(
              "optionStratConBatchallBreachConfirmation.toolTipText"));
        optionStratConBatchallBreachConfirmation.setName("optionStratConBatchallBreachConfirmation");

        optionStratConDeployConfirmation = new JCheckBox(resources.getString(
              "optionStratConDeployConfirmation.text"));
        optionStratConDeployConfirmation.setToolTipText(resources.getString(
              "optionStratConDeployConfirmation.toolTipText"));
        optionStratConDeployConfirmation.setName("optionStratConDeployConfirmation");

        optionAbandonUnitsConfirmation = new JCheckBox(resources.getString(
              "optionAbandonUnitsConfirmation.text"));
        optionAbandonUnitsConfirmation.setToolTipText(resources.getString(
              "optionAbandonUnitsConfirmation.toolTipText"));
        optionAbandonUnitsConfirmation.setName("optionAbandonUnitsConfirmation");


        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("nagPanel");
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addComponent(optionUnmaintainedUnitsNag)
                                      .addComponent(optionPregnantCombatantNag)
                                      .addComponent(optionPrisonersNag)
                                      .addComponent(optionHRStrainNag)
                                      .addComponent(optionUntreatedPersonnelNag)
                                      .addComponent(optionNoCommanderNag)
                                      .addComponent(optionContractEndedNag)
                                      .addComponent(optionSingleDropNag)
                                      .addComponent(optionInsufficientAsTechsNag)
                                      .addComponent(optionInsufficientAsTechTimeNag)
                                      .addComponent(optionInsufficientMedicsNag)
                                      .addComponent(optionShortDeploymentNag)
                                      .addComponent(optionCombatChallengeNag)
                                      .addComponent(optionUnresolvedStratConContactsNag)
                                      .addComponent(optionOutstandingScenariosNag)
                                      .addComponent(optionInvalidFactionNag)
                                      .addComponent(optionUnableToAffordExpensesNag)
                                      .addComponent(optionUnableToAffordRentNag)
                                      .addComponent(optionUnableToAffordLoanPaymentNag)
                                      .addComponent(optionUnableToAffordJumpNag)
                                      .addComponent(optionUnableToAffordShoppingListNag)
                                      .addComponent(optionContractRentalConfirmation)
                                      .addComponent(optionFactionStandingsUltimatumConfirmation)
                                      .addComponent(optionBeginTransitConfirmation)
                                      .addComponent(optionStratConBatchallBreachConfirmation)
                                      .addComponent(optionStratConDeployConfirmation)
                                      .addComponent(optionAbandonUnitsConfirmation));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(optionUnmaintainedUnitsNag)
                                        .addComponent(optionPregnantCombatantNag)
                                        .addComponent(optionPrisonersNag)
                                        .addComponent(optionHRStrainNag)
                                        .addComponent(optionUntreatedPersonnelNag)
                                        .addComponent(optionNoCommanderNag)
                                        .addComponent(optionContractEndedNag)
                                        .addComponent(optionSingleDropNag)
                                        .addComponent(optionInsufficientAsTechsNag)
                                        .addComponent(optionInsufficientAsTechTimeNag)
                                        .addComponent(optionInsufficientMedicsNag)
                                        .addComponent(optionShortDeploymentNag)
                                        .addComponent(optionCombatChallengeNag)
                                        .addComponent(optionUnresolvedStratConContactsNag)
                                        .addComponent(optionOutstandingScenariosNag)
                                        .addComponent(optionInvalidFactionNag)
                                        .addComponent(optionUnableToAffordExpensesNag)
                                        .addComponent(optionUnableToAffordRentNag)
                                        .addComponent(optionUnableToAffordLoanPaymentNag)
                                        .addComponent(optionUnableToAffordJumpNag)
                                        .addComponent(optionUnableToAffordShoppingListNag)
                                        .addComponent(optionContractRentalConfirmation)
                                        .addComponent(optionFactionStandingsUltimatumConfirmation)
                                        .addComponent(optionBeginTransitConfirmation)
                                        .addComponent(optionStratConBatchallBreachConfirmation)
                                        .addComponent(optionStratConDeployConfirmation)
                                        .addComponent(optionAbandonUnitsConfirmation));

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
            LOGGER.error("Could not find the user data directory readme file at {}", MMConstants.USER_DIR_README_FILE);
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

        final JLabel lblStartGameBotClientRetryCount = new JLabel(resources.getString(
              "lblStartGameBotClientRetryCount.text"));
        lblStartGameBotClientRetryCount.setToolTipText(resources.getString("lblStartGameBotClientRetryCount.toolTipText"));
        lblStartGameBotClientRetryCount.setName("lblStartGameBotClientRetryCount");

        spnStartGameBotClientRetryCount = new JSpinner(new SpinnerNumberModel(250, 100, 2500, 50));
        spnStartGameBotClientRetryCount.setToolTipText(resources.getString("lblStartGameBotClientRetryCount.toolTipText"));
        spnStartGameBotClientRetryCount.setName("spnStartGameBotClientRetryCount");

        final JLabel lblDefaultCompanyGenerationMethod = new JLabel(resources.getString(
              "lblDefaultCompanyGenerationMethod.text"));
        lblDefaultCompanyGenerationMethod.setToolTipText(resources.getString(
              "lblDefaultCompanyGenerationMethod.toolTipText"));
        lblDefaultCompanyGenerationMethod.setName("lblDefaultCompanyGenerationMethod");

        comboDefaultCompanyGenerationMethod = new MMComboBox<>("comboDefaultCompanyGenerationMethod",
              CompanyGenerationMethod.values());
        comboDefaultCompanyGenerationMethod.setToolTipText(resources.getString(
              "lblDefaultCompanyGenerationMethod.toolTipText"));
        comboDefaultCompanyGenerationMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
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

        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblUserDir)
                                                      .addComponent(txtUserDir)
                                                      .addComponent(userDirChooser)
                                                      .addComponent(userDirHelp,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblStartGameDelay)
                                                      .addComponent(spnStartGameDelay,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblStartGameClientDelay)
                                                      .addComponent(spnStartGameClientDelay,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblStartGameClientRetryCount)
                                                      .addComponent(spnStartGameClientRetryCount,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblStartGameBotClientDelay)
                                                      .addComponent(spnStartGameBotClientDelay,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblStartGameBotClientRetryCount)
                                                      .addComponent(spnStartGameBotClientRetryCount,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            GroupLayout.DEFAULT_SIZE,
                                                            40))
                                      .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                      .addComponent(lblDefaultCompanyGenerationMethod)
                                                      .addComponent(comboDefaultCompanyGenerationMethod)));

        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
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
                                                        .addComponent(comboDefaultCompanyGenerationMethod)));

        return body;
    }
    // endregion Initialization

    @Override
    protected void okAction() {
        MHQOptions options = MekHQ.getMHQOptions();

        if (GUIPreferences.getInstance().getGUIScale() * 10 != guiScale.getValue()) {
            GUIPreferences.getInstance().setValue(GUIPreferences.GUI_SCALE, 0.1 * guiScale.getValue());
            MekHQ.updateGuiScaling();
        }
        if (validateDateFormat(optionDisplayDateFormat.getText())) {
            options.setDisplayDateFormat(optionDisplayDateFormat.getText());
        }

        if (validateDateFormat(optionLongDisplayDateFormat.getText())) {
            options.setLongDisplayDateFormat(optionLongDisplayDateFormat.getText());
        }
        options.setHideUnitFluff(optionHideUnitFluff.isSelected());
        options.setHistoricalDailyLog(optionHistoricalDailyLog.isSelected());
        options.setCompanyGeneratorStartup(chkCompanyGeneratorStartup.isSelected());
        options.setShowCompanyGenerator(chkShowCompanyGenerator.isSelected());
        options.setShowUnitPicturesOnTOE(chkShowUnitPicturesOnTOE.isSelected());

        // Command Center Tab
        options.setCommandCenterMRMS(optionCommandCenterMRMS.isSelected());

        // Interstellar Map Tab
        options.setInterstellarMapShowJumpRadius(chkInterstellarMapShowJumpRadius.isSelected());
        options
              .setInterstellarMapShowJumpRadiusMinimumZoom((Double) spnInterstellarMapShowJumpRadiusMinimumZoom.getValue());
        options.setInterstellarMapJumpRadiusColour(btnInterstellarMapJumpRadiusColour.getColour());
        options
              .setInterstellarMapShowPlanetaryAcquisitionRadius(chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected());
        options
              .setInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom((Double) spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.getValue());
        options
              .setInterstellarMapPlanetaryAcquisitionRadiusColour(btnInterstellarMapPlanetaryAcquisitionRadiusColour.getColour());
        options
              .setInterstellarMapShowContractSearchRadius(chkInterstellarMapShowContractSearchRadius.isSelected());
        options
              .setInterstellarMapContractSearchRadiusColour(btnInterstellarMapContractSearchRadiusColour.getColour());

        // Personnel Tab
        options
              .setPersonnelFilterStyle((PersonnelFilterStyle) Objects.requireNonNull(optionPersonnelFilterStyle.getSelectedItem()));
        options.setPersonnelFilterOnPrimaryRole(optionPersonnelFilterOnPrimaryRole.isSelected());
        options.setUnifiedDailyReport(chkUnifiedDailyReport.isSelected());

        // Colours
        options.setDeployedForeground(optionDeployedForeground.getColour());
        options.setDeployedBackground(optionDeployedBackground.getColour());
        options.setBelowContractMinimumForeground(optionBelowContractMinimumForeground.getColour());
        options.setBelowContractMinimumBackground(optionBelowContractMinimumBackground.getColour());
        options.setInTransitForeground(optionInTransitForeground.getColour());
        options.setInTransitBackground(optionInTransitBackground.getColour());
        options.setRefittingForeground(optionRefittingForeground.getColour());
        options.setRefittingBackground(optionRefittingBackground.getColour());
        options.setMothballingForeground(optionMothballingForeground.getColour());
        options.setMothballingBackground(optionMothballingBackground.getColour());
        options.setMothballedForeground(optionMothballedForeground.getColour());
        options.setMothballedBackground(optionMothballedBackground.getColour());
        options.setNotRepairableForeground(optionNotRepairableForeground.getColour());
        options.setNotRepairableBackground(optionNotRepairableBackground.getColour());
        options.setNonFunctionalForeground(optionNonFunctionalForeground.getColour());
        options.setNonFunctionalBackground(optionNonFunctionalBackground.getColour());
        options.setNeedsPartsFixedForeground(optionNeedsPartsFixedForeground.getColour());
        options.setNeedsPartsFixedBackground(optionNeedsPartsFixedBackground.getColour());
        options.setUnmaintainedForeground(optionUnmaintainedForeground.getColour());
        options.setUnmaintainedBackground(optionUnmaintainedBackground.getColour());
        options.setUncrewedForeground(optionUncrewedForeground.getColour());
        options.setUncrewedBackground(optionUncrewedBackground.getColour());
        options.setLoanOverdueForeground(optionLoanOverdueForeground.getColour());
        options.setLoanOverdueBackground(optionLoanOverdueBackground.getColour());
        options.setInjuredForeground(optionInjuredForeground.getColour());
        options.setInjuredBackground(optionInjuredBackground.getColour());
        options.setHealedInjuriesForeground(optionHealedInjuriesForeground.getColour());
        options.setHealedInjuriesBackground(optionHealedInjuriesBackground.getColour());
        options.setPregnantForeground(optionPregnantForeground.getColour());
        options.setPregnantBackground(optionPregnantBackground.getColour());
        options.setGoneForeground(optionGoneForeground.getColour());
        options.setGoneBackground(optionGoneBackground.getColour());
        options.setAbsentForeground(optionAbsentForeground.getColour());
        options.setAbsentBackground(optionAbsentBackground.getColour());
        options.setFatiguedForeground(optionFatiguedForeground.getColour());
        options.setFatiguedBackground(optionFatiguedBackground.getColour());
        options.setStratConHexCoordForeground(optionStratConHexCoordForeground.getColour());
        options.setFontColorNegative(optionFontColorNegative.getColour());
        options.setFontColorWarning(optionFontColorWarning.getColour());
        options.setFontColorAmazing(optionFontColorAmazing.getColour());
        options.setFontColorPositive(optionFontColorPositive.getColour());
        options.setFontColorSkillUltraGreen(optionFontColorSkillUltraGreen.getColour());
        options.setFontColorSkillGreen(optionFontColorSkillGreen.getColour());
        options.setFontColorSkillRegular(optionFontColorSkillRegular.getColour());
        options.setFontColorSkillVeteran(optionFontColorSkillVeteran.getColour());
        options.setFontColorSkillElite(optionFontColorSkillElite.getColour());

        options
              .setMedicalViewDialogHandwritingFont(comboMedicalViewDialogHandwritingFont.getFont().getFamily());

        options.setNoAutosaveValue(optionNoSave.isSelected());
        options.setAutosaveDailyValue(optionSaveDaily.isSelected());
        options.setAutosaveWeeklyValue(optionSaveWeekly.isSelected());
        options.setAutosaveMonthlyValue(optionSaveMonthly.isSelected());
        options.setAutosaveYearlyValue(optionSaveYearly.isSelected());
        options.setAutosaveBeforeScenariosValue(checkSaveBeforeScenarios.isSelected());
        options.setAutosaveBeforeMissionEndValue(checkSaveBeforeContractEnd.isSelected());
        options.setMaximumNumberOfAutoSavesValue((Integer) spinnerSavedGamesCount.getValue());

        options.setNewDayAsTechPoolFill(chkNewDayAsTechPoolFill.isSelected());
        options.setNewDayMedicPoolFill(chkNewDayMedicPoolFill.isSelected());
        options.setNewDayMRMS(chkNewDayMRMS.isSelected());
        options.setNewDayOptimizeMedicalAssignments(chkNewDayOptimizeMedicalAssignments.isSelected());
        options.setNewDaySoldierPoolFill(chkNewDaySoldierPoolFill.isSelected());
        options.setNewDayBattleArmorPoolFill(chkNewDayBattleArmorPoolFill.isSelected());
        options.setNewDayVehicleCrewGroundPoolFill(chkNewDayVehicleCrewGroundPoolFill.isSelected());
        options.setNewDayVehicleCrewVTOLPoolFill(chkNewDayVehicleCrewVTOLPoolFill.isSelected());
        options.setNewDayVehicleCrewNavalPoolFill(chkNewDayVehicleCrewNavalPoolFill.isSelected());
        options.setNewDayVesselPilotPoolFill(chkNewDayVesselPilotPoolFill.isSelected());
        options.setNewDayVesselGunnerPoolFill(chkNewDayVesselGunnerPoolFill.isSelected());
        options.setNewDayVesselCrewPoolFill(chkNewDayVesselCrewPoolFill.isSelected());
        options
              .setNewDayAutomaticallyAssignUnmaintainedUnits(chkNewDayAutomaticallyAssignUnmaintainedUnits.isSelected());
        options.setNewMonthQuickTrain(chkNewMonthQuickTrain.isSelected());
        options.setSelfCorrectMaintenance(chkSelfCorrectMaintenance.isSelected());
        options.setNewDayFormationIconOperationalStatus(chkNewDayFormationIconOperationalStatus.isSelected());
        options
              .setNewDayFormationIconOperationalStatusStyle(Objects.requireNonNull(
                    comboNewDayFormationIconOperationalStatusStyle.getSelectedItem()));

        options.setPreferGzippedOutput(optionPreferGzippedOutput.isSelected());
        options.setWriteCustomsToXML(optionWriteCustomsToXML.isSelected());
        options.setWriteAllUnitsToXML(optionWriteAllUnitsToXML.isSelected());
        options.setSaveMothballState(optionSaveMothballState.isSelected());

        options
              .setNagDialogIgnore(MHQConstants.NAG_UNMAINTAINED_UNITS, optionUnmaintainedUnitsNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_PREGNANT_COMBATANT, optionPregnantCombatantNag.isSelected());
        options.setNagDialogIgnore(MHQConstants.NAG_PRISONERS, optionPrisonersNag.isSelected());
        options.setNagDialogIgnore(MHQConstants.NAG_HR_STRAIN, optionHRStrainNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNTREATED_PERSONNEL, optionUntreatedPersonnelNag.isSelected());
        options.setNagDialogIgnore(MHQConstants.NAG_NO_COMMANDER, optionNoCommanderNag.isSelected());
        options.setNagDialogIgnore(MHQConstants.NAG_CONTRACT_ENDED, optionContractEndedNag.isSelected());
        options.setNagDialogIgnore(MHQConstants.NAG_SINGLE_DROP_SET_UP, optionSingleDropNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_AS_TECHS, optionInsufficientAsTechsNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_AS_TECH_TIME,
                    optionInsufficientAsTechTimeNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_MEDICS, optionInsufficientMedicsNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_SHORT_DEPLOYMENT, optionShortDeploymentNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_COMBAT_CHALLENGE, optionCombatChallengeNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNRESOLVED_STRAT_CON_CONTACTS,
                    optionUnresolvedStratConContactsNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_OUTSTANDING_SCENARIOS, optionOutstandingScenariosNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_INVALID_FACTION, optionInvalidFactionNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES,
                    optionUnableToAffordExpensesNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_RENT,
                    optionUnableToAffordRentNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT,
                    optionUnableToAffordLoanPaymentNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP, optionUnableToAffordJumpNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_SHOPPING_LIST,
                    optionUnableToAffordShoppingListNag.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_CONTRACT_RENTAL,
                    optionContractRentalConfirmation.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_FACTION_STANDINGS_ULTIMATUM,
                    optionFactionStandingsUltimatumConfirmation.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_BEGIN_TRANSIT,
                    optionBeginTransitConfirmation.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_STRATCON_BATCHALL_BREACH,
                    optionStratConBatchallBreachConfirmation.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_STRATCON_DEPLOY,
                    optionStratConDeployConfirmation.isSelected());
        options
              .setNagDialogIgnore(MHQConstants.CONFIRMATION_ABANDON_UNITS,
                    optionAbandonUnitsConfirmation.isSelected());

        PreferenceManager.getClientPreferences().setUserDir(txtUserDir.getText());
        PreferenceManager.getInstance().save();
        options.setStartGameDelay((Integer) spnStartGameDelay.getValue());
        options.setStartGameClientDelay((Integer) spnStartGameClientDelay.getValue());
        options.setStartGameClientRetryCount((Integer) spnStartGameClientRetryCount.getValue());
        options.setStartGameBotClientDelay((Integer) spnStartGameBotClientDelay.getValue());
        options.setStartGameBotClientRetryCount((Integer) spnStartGameBotClientRetryCount.getValue());
        options
              .setDefaultCompanyGenerationMethod(Objects.requireNonNull(comboDefaultCompanyGenerationMethod.getSelectedItem()));

        MekHQ.triggerEvent(new MHQOptionsChangedEvent());
    }

    private void setInitialState() {
        MHQOptions options = MekHQ.getMHQOptions();

        guiScale.setValue((int) (GUIPreferences.getInstance().getGUIScale() * 10));
        optionDisplayDateFormat.setText(options.getDisplayDateFormat());
        optionLongDisplayDateFormat.setText(options.getLongDisplayDateFormat());
        optionHideUnitFluff.setSelected(options.getHideUnitFluff());
        optionHistoricalDailyLog.setSelected(options.getHistoricalDailyLog());
        chkCompanyGeneratorStartup.setSelected(options.getCompanyGeneratorStartup());
        chkShowCompanyGenerator.setSelected(options.getShowCompanyGenerator());
        chkShowUnitPicturesOnTOE.setSelected(options.getShowUnitPicturesOnTOE());

        // Command Center Tab
        optionCommandCenterMRMS.setSelected(options.getCommandCenterMRMS());

        // Interstellar Map Tab
        if (chkInterstellarMapShowJumpRadius.isSelected() != options.getInterstellarMapShowJumpRadius()) {
            chkInterstellarMapShowJumpRadius.doClick();
        }
        spnInterstellarMapShowJumpRadiusMinimumZoom.setValue(options
                                                                   .getInterstellarMapShowJumpRadiusMinimumZoom());
        btnInterstellarMapJumpRadiusColour.setColour(options.getInterstellarMapJumpRadiusColour());
        if (chkInterstellarMapShowPlanetaryAcquisitionRadius.isSelected() !=
                  options.getInterstellarMapShowPlanetaryAcquisitionRadius()) {
            chkInterstellarMapShowPlanetaryAcquisitionRadius.doClick();
        }
        spnInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom.setValue(options
                                                                                   .getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom());
        btnInterstellarMapPlanetaryAcquisitionRadiusColour.setColour(options
                                                                           .getInterstellarMapPlanetaryAcquisitionRadiusColour());
        if (chkInterstellarMapShowContractSearchRadius.isSelected() !=
                  options.getInterstellarMapShowContractSearchRadius()) {
            chkInterstellarMapShowContractSearchRadius.doClick();
        }
        btnInterstellarMapContractSearchRadiusColour.setColour(options
                                                                     .getInterstellarMapContractSearchRadiusColour());

        // Personnel Tab
        optionPersonnelFilterStyle.setSelectedItem(options.getPersonnelFilterStyle());
        optionPersonnelFilterOnPrimaryRole.setSelected(options.getPersonnelFilterOnPrimaryRole());
        chkUnifiedDailyReport.setSelected(options.getUnifiedDailyReport());

        // Colours
        optionDeployedForeground.setColour(options.getDeployedForeground());
        optionDeployedBackground.setColour(options.getDeployedBackground());
        optionBelowContractMinimumForeground.setColour(options.getBelowContractMinimumForeground());
        optionBelowContractMinimumBackground.setColour(options.getBelowContractMinimumBackground());
        optionInTransitForeground.setColour(options.getInTransitForeground());
        optionInTransitBackground.setColour(options.getInTransitBackground());
        optionRefittingForeground.setColour(options.getRefittingForeground());
        optionRefittingBackground.setColour(options.getRefittingBackground());
        optionMothballingForeground.setColour(options.getMothballingForeground());
        optionMothballingBackground.setColour(options.getMothballingBackground());
        optionMothballedForeground.setColour(options.getMothballedForeground());
        optionMothballedBackground.setColour(options.getMothballedBackground());
        optionNotRepairableForeground.setColour(options.getNotRepairableForeground());
        optionNotRepairableBackground.setColour(options.getNotRepairableBackground());
        optionNonFunctionalForeground.setColour(options.getNonFunctionalForeground());
        optionNonFunctionalBackground.setColour(options.getNonFunctionalBackground());
        optionNeedsPartsFixedForeground.setColour(options.getNeedsPartsFixedForeground());
        optionNeedsPartsFixedBackground.setColour(options.getNeedsPartsFixedBackground());
        optionUnmaintainedForeground.setColour(options.getUnmaintainedForeground());
        optionUnmaintainedBackground.setColour(options.getUnmaintainedBackground());
        optionUncrewedForeground.setColour(options.getUncrewedForeground());
        optionUncrewedBackground.setColour(options.getUncrewedBackground());
        optionLoanOverdueForeground.setColour(options.getLoanOverdueForeground());
        optionLoanOverdueBackground.setColour(options.getLoanOverdueBackground());
        optionInjuredForeground.setColour(options.getInjuredForeground());
        optionInjuredBackground.setColour(options.getInjuredBackground());
        optionHealedInjuriesForeground.setColour(options.getHealedInjuriesForeground());
        optionHealedInjuriesBackground.setColour(options.getHealedInjuriesBackground());
        optionPregnantForeground.setColour(options.getPregnantForeground());
        optionPregnantBackground.setColour(options.getPregnantBackground());
        optionGoneForeground.setColour(options.getGoneForeground());
        optionGoneBackground.setColour(options.getGoneBackground());
        optionAbsentForeground.setColour(options.getAbsentForeground());
        optionAbsentBackground.setColour(options.getAbsentBackground());
        optionFatiguedForeground.setColour(options.getFatiguedForeground());
        optionFatiguedBackground.setColour(options.getFatiguedBackground());
        optionStratConHexCoordForeground.setColour(options.getStratConHexCoordForeground());
        optionFontColorNegative.setColour(options.getFontColorNegative());
        optionFontColorWarning.setColour(options.getFontColorWarning());
        optionFontColorAmazing.setColour(options.getFontColorAmazing());
        optionFontColorPositive.setColour(options.getFontColorPositive());
        optionFontColorSkillUltraGreen.setColour(options.getFontColorSkillUltraGreen());
        optionFontColorSkillGreen.setColour(options.getFontColorSkillGreen());
        optionFontColorSkillRegular.setColour(options.getFontColorSkillRegular());
        optionFontColorSkillVeteran.setColour(options.getFontColorSkillVeteran());
        optionFontColorSkillElite.setColour(options.getFontColorSkillElite());

        comboMedicalViewDialogHandwritingFont.setSelectedItem(new FontDisplay(options
                                                                                    .getMedicalViewDialogHandwritingFont()));

        optionNoSave.setSelected(options.getNoAutosaveValue());
        optionSaveDaily.setSelected(options.getAutosaveDailyValue());
        optionSaveWeekly.setSelected(options.getAutosaveWeeklyValue());
        optionSaveMonthly.setSelected(options.getAutosaveMonthlyValue());
        optionSaveYearly.setSelected(options.getAutosaveYearlyValue());
        checkSaveBeforeScenarios.setSelected(options.getAutosaveBeforeScenariosValue());
        checkSaveBeforeContractEnd.setSelected(options.getAutosaveBeforeMissionEndValue());
        spinnerSavedGamesCount.setValue(options.getMaximumNumberOfAutoSavesValue());

        chkNewDayAsTechPoolFill.setSelected(options.getNewDayAsTechPoolFill());
        chkNewDayMedicPoolFill.setSelected(options.getNewDayMedicPoolFill());
        chkNewDaySoldierPoolFill.setSelected(MekHQ.getMHQOptions().getNewDaySoldierPoolFill());
        chkNewDayBattleArmorPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayBattleArmorPoolFill());
        chkNewDayVehicleCrewGroundPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVehicleCrewGroundPoolFill());
        chkNewDayVehicleCrewVTOLPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVehicleCrewVTOLPoolFill());
        chkNewDayVehicleCrewNavalPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVehicleCrewNavalPoolFill());
        chkNewDayVesselPilotPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVesselPilotPoolFill());
        chkNewDayVesselGunnerPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVesselGunnerPoolFill());
        chkNewDayVesselCrewPoolFill.setSelected(MekHQ.getMHQOptions().getNewDayVesselCrewPoolFill());
        chkNewDayMRMS.setSelected(options.getNewDayMRMS());
        chkNewDayOptimizeMedicalAssignments.setSelected(options.getNewDayOptimizeMedicalAssignments());
        chkNewDayAutomaticallyAssignUnmaintainedUnits.setSelected(options
                                                                        .getNewDayAutomaticallyAssignUnmaintainedUnits());
        chkNewMonthQuickTrain.setSelected(options.getNewMonthQuickTrain());
        chkSelfCorrectMaintenance.setSelected(options.getSelfCorrectMaintenance());
        if (chkNewDayFormationIconOperationalStatus.isSelected() !=
                  options.getNewDayFormationIconOperationalStatus()) {
            chkNewDayFormationIconOperationalStatus.doClick();
        }
        comboNewDayFormationIconOperationalStatusStyle.setSelectedItem(options
                                                                         .getNewDayFormationIconOperationalStatusStyle());

        optionPreferGzippedOutput.setSelected(options.getPreferGzippedOutput());
        optionWriteCustomsToXML.setSelected(options.getWriteCustomsToXML());
        optionWriteAllUnitsToXML.setSelected(options.getWriteAllUnitsToXML());
        optionSaveMothballState.setSelected(options.getSaveMothballState());

        optionUnmaintainedUnitsNag.setSelected(options
                                                     .getNagDialogIgnore(MHQConstants.NAG_UNMAINTAINED_UNITS));
        optionPregnantCombatantNag.setSelected(options
                                                     .getNagDialogIgnore(MHQConstants.NAG_PREGNANT_COMBATANT));
        optionPrisonersNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_PRISONERS));
        optionHRStrainNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_HR_STRAIN));
        optionUntreatedPersonnelNag.setSelected(options
                                                      .getNagDialogIgnore(MHQConstants.NAG_UNTREATED_PERSONNEL));
        optionNoCommanderNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_NO_COMMANDER));
        optionContractEndedNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_CONTRACT_ENDED));
        optionSingleDropNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_SINGLE_DROP_SET_UP));
        optionInsufficientAsTechsNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_AS_TECHS));
        optionInsufficientAsTechTimeNag.setSelected(options
                                                          .getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_AS_TECH_TIME));
        optionInsufficientMedicsNag.setSelected(options
                                                      .getNagDialogIgnore(MHQConstants.NAG_INSUFFICIENT_MEDICS));
        optionShortDeploymentNag.setSelected(options
                                                   .getNagDialogIgnore(MHQConstants.NAG_SHORT_DEPLOYMENT));
        optionCombatChallengeNag.setSelected(options
                                                   .getNagDialogIgnore(MHQConstants.NAG_COMBAT_CHALLENGE));
        optionUnresolvedStratConContactsNag.setSelected(options
                                                              .getNagDialogIgnore(MHQConstants.NAG_UNRESOLVED_STRAT_CON_CONTACTS));
        optionOutstandingScenariosNag.setSelected(options
                                                        .getNagDialogIgnore(MHQConstants.NAG_OUTSTANDING_SCENARIOS));
        optionInvalidFactionNag.setSelected(options.getNagDialogIgnore(MHQConstants.NAG_INVALID_FACTION));
        optionUnableToAffordExpensesNag.setSelected(options
                                                          .getNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES));
        optionUnableToAffordRentNag.setSelected(options
                                                      .getNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_RENT));
        optionUnableToAffordLoanPaymentNag.setSelected(options
                                                             .getNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT));
        optionUnableToAffordJumpNag.setSelected(options
                                                      .getNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP));
        optionUnableToAffordShoppingListNag.setSelected(options
                                                              .getNagDialogIgnore(MHQConstants.NAG_UNABLE_TO_AFFORD_SHOPPING_LIST));

        optionContractRentalConfirmation.setSelected(options
                                                           .getNagDialogIgnore(MHQConstants.CONFIRMATION_CONTRACT_RENTAL));

        optionFactionStandingsUltimatumConfirmation.setSelected(options
                                                                      .getNagDialogIgnore(MHQConstants.CONFIRMATION_FACTION_STANDINGS_ULTIMATUM));

        optionBeginTransitConfirmation.setSelected(options
                                                         .getNagDialogIgnore(MHQConstants.CONFIRMATION_BEGIN_TRANSIT));

        optionStratConBatchallBreachConfirmation.setSelected(options
                                                                   .getNagDialogIgnore(MHQConstants.CONFIRMATION_STRATCON_BATCHALL_BREACH));

        optionStratConDeployConfirmation.setSelected(options
                                                           .getNagDialogIgnore(MHQConstants.CONFIRMATION_STRATCON_DEPLOY));
        optionAbandonUnitsConfirmation.setSelected(options
                                                         .getNagDialogIgnore(MHQConstants.CONFIRMATION_ABANDON_UNITS));
        txtUserDir.setText(PreferenceManager.getClientPreferences().getUserDir());
        spnStartGameDelay.setValue(options.getStartGameDelay());
        spnStartGameClientDelay.setValue(options.getStartGameClientDelay());
        spnStartGameClientRetryCount.setValue(options.getStartGameClientRetryCount());
        spnStartGameBotClientDelay.setValue(options.getStartGameBotClientDelay());
        spnStartGameBotClientRetryCount.setValue(options.getStartGameBotClientRetryCount());
        comboDefaultCompanyGenerationMethod.setSelectedItem(options.getDefaultCompanyGenerationMethod());
    }

    // region Data Validation
    private boolean validateDateFormat(final String format) {
        try {
            LocalDate.now()
                  .format(DateTimeFormatter.ofPattern(format).withLocale(MekHQ.getMHQOptions().getDateLocale()));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
    // endregion Data Validation
}
