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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.preference.PreferenceManager;
import mekhq.MHQConstants;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.gui.enums.FormationIconOperationalStatusStyle;
import mekhq.gui.enums.PersonnelFilterStyle;

/**
 * Plain data-transfer object holding an editable snapshot of every MekHQ client option shown by {@link MHQOptionsPane},
 * mirroring the model pattern the Campaign Options dialog uses (for example
 * {@link mekhq.gui.campaignOptions.contents.PersonnelOptionsModel}). The constructor reads the current values from
 * {@link MHQOptions} (and the two stores that back options which do not live on {@code MHQOptions} - the GUI scale in
 * {@link GUIPreferences} and the user directory in {@link PreferenceManager}); {@link #applyTo(MHQOptions)} writes the
 * edited values back to those same stores.
 *
 * <p>The pane's controls read from and write to these fields via each page's {@code readFromModel}/{@code writeToModel}
 * methods, so the controls never touch {@code MHQOptions} directly and nothing is committed until the dialog is
 * confirmed.</p>
 */
class MHQOptionsModel {
    /** Nag/confirmation dialog keys persisted through {@link MHQOptions#getNagDialogIgnore(String)}. */
    static final String[] NAG_IGNORE_KEYS = {
          MHQConstants.NAG_UNMAINTAINED_UNITS,
          MHQConstants.NAG_PREGNANT_COMBATANT,
          MHQConstants.NAG_PRISONERS,
          MHQConstants.NAG_HR_STRAIN,
          MHQConstants.NAG_UNTREATED_PERSONNEL,
          MHQConstants.NAG_NO_COMMANDER,
          MHQConstants.NAG_CONTRACT_ENDED,
          MHQConstants.NAG_SINGLE_DROP_SET_UP,
          MHQConstants.NAG_INSUFFICIENT_AS_TECHS,
          MHQConstants.NAG_INSUFFICIENT_AS_TECH_TIME,
          MHQConstants.NAG_INSUFFICIENT_MEDICS,
          MHQConstants.NAG_SHORT_DEPLOYMENT,
          MHQConstants.NAG_COMBAT_CHALLENGE,
          MHQConstants.NAG_UNRESOLVED_STRAT_CON_CONTACTS,
          MHQConstants.NAG_OUTSTANDING_SCENARIOS,
          MHQConstants.NAG_INVALID_FACTION,
          MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES,
          MHQConstants.NAG_UNABLE_TO_AFFORD_RENT,
          MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT,
          MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP,
          MHQConstants.NAG_UNABLE_TO_AFFORD_SHOPPING_LIST,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_COMBAT,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_TECH,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER,
          MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_RETIREE,
          MHQConstants.CONFIRMATION_CONTRACT_RENTAL,
          MHQConstants.CONFIRMATION_FACTION_STANDINGS_ULTIMATUM,
          MHQConstants.CONFIRMATION_BEGIN_TRANSIT,
          MHQConstants.CONFIRMATION_STRATCON_BATCHALL_BREACH,
          MHQConstants.CONFIRMATION_STRATCON_DEPLOY,
          MHQConstants.CONFIRMATION_RESOLVE_SCENARIO,
          MHQConstants.CONFIRMATION_ABANDON_UNITS,
          MHQConstants.CONFIRMATION_ASSIGN_TECHS,
    };

    // region Fonts
    String medicalViewDialogHandwritingFont;
    // endregion Fonts

    // region Display - General
    String displayDateFormat;
    String longDisplayDateFormat;
    int guiScaleValue;
    boolean hideUnitFluff;
    boolean historicalDailyLog;
    boolean companyGeneratorStartup;
    boolean showCompanyGenerator;
    boolean showUnitPicturesOnTOE;
    // endregion Display - General

    // region Display - Interstellar Map
    boolean interstellarMapShowJumpRadius;
    double interstellarMapShowJumpRadiusMinimumZoom;
    Color interstellarMapJumpRadiusColour;
    boolean interstellarMapShowPlanetaryAcquisitionRadius;
    double interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom;
    Color interstellarMapPlanetaryAcquisitionRadiusColour;
    boolean interstellarMapShowContractSearchRadius;
    Color interstellarMapContractSearchRadiusColour;
    // endregion Display - Interstellar Map

    // region Display - Personnel List
    PersonnelFilterStyle personnelFilterStyle;
    boolean personnelFilterOnPrimaryRole;
    boolean unifiedDailyReport;
    boolean aggregateDailyReport;
    // endregion Display - Personnel List

    // region Colours (keyed by the control's resource name)
    final Map<String, Color> statusColours = new HashMap<>();
    // endregion Colours

    // region Autosave
    boolean noAutosave;
    boolean autosaveDaily;
    boolean autosaveWeekly;
    boolean autosaveMonthly;
    boolean autosaveYearly;
    boolean autosaveBeforeScenarios;
    boolean autosaveBeforeMissionEnd;
    int maximumNumberOfAutoSaves;
    // endregion Autosave

    // region New Day - Personnel Pools (keyed by the control's resource name)
    final Map<String, Boolean> newDayPools = new HashMap<>();
    // endregion New Day - Personnel Pools

    // region New Day - Automation
    boolean newDayAutoLogistics;
    boolean newDayMRMS;
    boolean newDayOptimizeMedicalAssignments;
    boolean newDayAutomaticallyAssignUnmaintainedUnits;
    boolean selfCorrectMaintenance;
    // endregion New Day - Automation

    // region New Day - Training
    boolean newMonthQuickTrain;
    int quickTrainTarget;
    boolean levelArtillery;
    boolean levelScouting;
    boolean levelEscape;
    boolean levelLeadership;
    boolean levelTraining;
    boolean levelOtherCommand;
    // endregion New Day - Training

    // region New Day - Formation Icons
    boolean newDayFormationIconOperationalStatus;
    FormationIconOperationalStatusStyle newDayFormationIconOperationalStatusStyle;
    // endregion New Day - Formation Icons

    // region Campaign Save
    boolean preferGzippedOutput;
    boolean writeCustomsToXML;
    boolean writeAllUnitsToXML;
    boolean saveMothballState;
    // endregion Campaign Save

    // region Reminders & Confirmations (keyed by MHQConstants nag/confirmation key)
    final Map<String, Boolean> nagIgnores = new HashMap<>();
    // endregion Reminders & Confirmations

    // region Advanced
    String userDir;
    int startGameDelay;
    int startGameClientDelay;
    int startGameClientRetryCount;
    int startGameBotClientDelay;
    int startGameBotClientRetryCount;
    CompanyGenerationMethod defaultCompanyGenerationMethod;
    // endregion Advanced

    MHQOptionsModel(MHQOptions options) {
        // Fonts
        medicalViewDialogHandwritingFont = options.getMedicalViewDialogHandwritingFont();

        // Display - General
        displayDateFormat = options.getDisplayDateFormat();
        longDisplayDateFormat = options.getLongDisplayDateFormat();
        guiScaleValue = (int) (GUIPreferences.getInstance().getGUIScale() * 10);
        hideUnitFluff = options.getHideUnitFluff();
        historicalDailyLog = options.getHistoricalDailyLog();
        companyGeneratorStartup = options.getCompanyGeneratorStartup();
        showCompanyGenerator = options.getShowCompanyGenerator();
        showUnitPicturesOnTOE = options.getShowUnitPicturesOnTOE();

        // Display - Interstellar Map
        interstellarMapShowJumpRadius = options.getInterstellarMapShowJumpRadius();
        interstellarMapShowJumpRadiusMinimumZoom = options.getInterstellarMapShowJumpRadiusMinimumZoom();
        interstellarMapJumpRadiusColour = options.getInterstellarMapJumpRadiusColour();
        interstellarMapShowPlanetaryAcquisitionRadius = options.getInterstellarMapShowPlanetaryAcquisitionRadius();
        interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom =
              options.getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom();
        interstellarMapPlanetaryAcquisitionRadiusColour = options.getInterstellarMapPlanetaryAcquisitionRadiusColour();
        interstellarMapShowContractSearchRadius = options.getInterstellarMapShowContractSearchRadius();
        interstellarMapContractSearchRadiusColour = options.getInterstellarMapContractSearchRadiusColour();

        // Display - Personnel List
        personnelFilterStyle = options.getPersonnelFilterStyle();
        personnelFilterOnPrimaryRole = options.getPersonnelFilterOnPrimaryRole();
        unifiedDailyReport = options.getUnifiedDailyReport();
        aggregateDailyReport = options.isUseAggregateDailyReport();

        // Colours
        statusColours.put("optionDeployedForeground", options.getDeployedForeground());
        statusColours.put("optionDeployedBackground", options.getDeployedBackground());
        statusColours.put("optionBelowContractMinimumForeground", options.getBelowContractMinimumForeground());
        statusColours.put("optionBelowContractMinimumBackground", options.getBelowContractMinimumBackground());
        statusColours.put("optionInTransitForeground", options.getInTransitForeground());
        statusColours.put("optionInTransitBackground", options.getInTransitBackground());
        statusColours.put("optionQueuedForTravelForeground", options.getQueuedForTravelForeground());
        statusColours.put("optionQueuedForTravelBackground", options.getQueuedForTravelBackground());
        statusColours.put("optionRefittingForeground", options.getRefittingForeground());
        statusColours.put("optionRefittingBackground", options.getRefittingBackground());
        statusColours.put("optionMothballingForeground", options.getMothballingForeground());
        statusColours.put("optionMothballingBackground", options.getMothballingBackground());
        statusColours.put("optionMothballedForeground", options.getMothballedForeground());
        statusColours.put("optionMothballedBackground", options.getMothballedBackground());
        statusColours.put("optionNotRepairableForeground", options.getNotRepairableForeground());
        statusColours.put("optionNotRepairableBackground", options.getNotRepairableBackground());
        statusColours.put("optionNonFunctionalForeground", options.getNonFunctionalForeground());
        statusColours.put("optionNonFunctionalBackground", options.getNonFunctionalBackground());
        statusColours.put("optionNeedsPartsFixedForeground", options.getNeedsPartsFixedForeground());
        statusColours.put("optionNeedsPartsFixedBackground", options.getNeedsPartsFixedBackground());
        statusColours.put("optionUnmaintainedForeground", options.getUnmaintainedForeground());
        statusColours.put("optionUnmaintainedBackground", options.getUnmaintainedBackground());
        statusColours.put("optionUncrewedForeground", options.getUncrewedForeground());
        statusColours.put("optionUncrewedBackground", options.getUncrewedBackground());
        statusColours.put("optionLoanOverdueForeground", options.getLoanOverdueForeground());
        statusColours.put("optionLoanOverdueBackground", options.getLoanOverdueBackground());
        statusColours.put("optionInjuredForeground", options.getInjuredForeground());
        statusColours.put("optionInjuredBackground", options.getInjuredBackground());
        statusColours.put("optionHealedInjuriesForeground", options.getHealedInjuriesForeground());
        statusColours.put("optionHealedInjuriesBackground", options.getHealedInjuriesBackground());
        statusColours.put("optionPregnantForeground", options.getPregnantForeground());
        statusColours.put("optionPregnantBackground", options.getPregnantBackground());
        statusColours.put("optionGoneForeground", options.getGoneForeground());
        statusColours.put("optionGoneBackground", options.getGoneBackground());
        statusColours.put("optionAbsentForeground", options.getAbsentForeground());
        statusColours.put("optionAbsentBackground", options.getAbsentBackground());
        statusColours.put("optionFatiguedForeground", options.getFatiguedForeground());
        statusColours.put("optionFatiguedBackground", options.getFatiguedBackground());
        statusColours.put("optionAwayFromMainForceForeground", options.getAwayFromMainForceForeground());
        statusColours.put("optionAwayFromMainForceBackground", options.getAwayFromMainForceBackground());
        statusColours.put("optionStratConHexCoordForeground", options.getStratConHexCoordForeground());
        statusColours.put("optionFontColorNegative", options.getFontColorNegative());
        statusColours.put("optionFontColorWarning", options.getFontColorWarning());
        statusColours.put("optionFontColorPositive", options.getFontColorPositive());
        statusColours.put("optionFontColorAmazing", options.getFontColorAmazing());
        statusColours.put("optionFontColorSkillUltraGreen", options.getFontColorSkillUltraGreen());
        statusColours.put("optionFontColorSkillGreen", options.getFontColorSkillGreen());
        statusColours.put("optionFontColorSkillRegular", options.getFontColorSkillRegular());
        statusColours.put("optionFontColorSkillVeteran", options.getFontColorSkillVeteran());
        statusColours.put("optionFontColorSkillElite", options.getFontColorSkillElite());

        // Autosave
        noAutosave = options.getNoAutosaveValue();
        autosaveDaily = options.getAutosaveDailyValue();
        autosaveWeekly = options.getAutosaveWeeklyValue();
        autosaveMonthly = options.getAutosaveMonthlyValue();
        autosaveYearly = options.getAutosaveYearlyValue();
        autosaveBeforeScenarios = options.getAutosaveBeforeScenariosValue();
        autosaveBeforeMissionEnd = options.getAutosaveBeforeMissionEndValue();
        maximumNumberOfAutoSaves = options.getMaximumNumberOfAutoSavesValue();

        // New Day - Personnel Pools
        newDayPools.put("chkNewDayAstechPoolFill", options.getNewDayAsTechPoolFill());
        newDayPools.put("chkNewDayAstechPoolNoRelease", options.getNewDayAsTechPoolNoRelease());
        newDayPools.put("chkNewDayMedicPoolFill", options.getNewDayMedicPoolFill());
        newDayPools.put("chkNewDayMedicPoolNoRelease", options.getNewDayMedicPoolNoRelease());
        newDayPools.put("chkNewDaySoldierPoolFill", options.getNewDaySoldierPoolFill());
        newDayPools.put("chkNewDaySoldierPoolNoRelease", options.getNewDaySoldierPoolNoRelease());
        newDayPools.put("chkNewDayBattleArmorPoolFill", options.getNewDayBattleArmorPoolFill());
        newDayPools.put("chkNewDayBattleArmorPoolNoRelease", options.getNewDayBattleArmorPoolNoRelease());
        newDayPools.put("chkNewDayVehicleCrewGroundPoolFill", options.getNewDayVehicleCrewGroundPoolFill());
        newDayPools.put("chkNewDayVehicleCrewGroundPoolNoRelease", options.getNewDayVehicleCrewGroundPoolNoRelease());
        newDayPools.put("chkNewDayVehicleCrewVTOLPoolFill", options.getNewDayVehicleCrewVTOLPoolFill());
        newDayPools.put("chkNewDayVehicleCrewVTOLPoolNoRelease", options.getNewDayVehicleCrewVTOLPoolNoRelease());
        newDayPools.put("chkNewDayVehicleCrewNavalPoolFill", options.getNewDayVehicleCrewNavalPoolFill());
        newDayPools.put("chkNewDayVehicleCrewNavalPoolNoRelease", options.getNewDayVehicleCrewNavalPoolNoRelease());
        newDayPools.put("chkNewDayVesselPilotPoolFill", options.getNewDayVesselPilotPoolFill());
        newDayPools.put("chkNewDayVesselPilotPoolNoRelease", options.getNewDayVesselPilotPoolNoRelease());
        newDayPools.put("chkNewDayVesselGunnerPoolFill", options.getNewDayVesselGunnerPoolFill());
        newDayPools.put("chkNewDayVesselGunnerPoolNoRelease", options.getNewDayVesselGunnerPoolNoRelease());
        newDayPools.put("chkNewDayVesselCrewPoolFill", options.getNewDayVesselCrewPoolFill());
        newDayPools.put("chkNewDayVesselCrewPoolNoRelease", options.getNewDayVesselCrewPoolNoRelease());

        // New Day - Automation
        newDayAutoLogistics = options.getNewDayAutoLogistics();
        newDayMRMS = options.getNewDayMRMS();
        newDayOptimizeMedicalAssignments = options.getNewDayOptimizeMedicalAssignments();
        newDayAutomaticallyAssignUnmaintainedUnits = options.getNewDayAutomaticallyAssignUnmaintainedUnits();
        selfCorrectMaintenance = options.getSelfCorrectMaintenance();

        // New Day - Training
        newMonthQuickTrain = options.getNewMonthQuickTrain();
        quickTrainTarget = options.getQuickTrainTarget();
        levelArtillery = options.getLevelArtillery();
        levelScouting = options.getLevelScouting();
        levelEscape = options.getLevelEscape();
        levelLeadership = options.getLevelLeadership();
        levelTraining = options.getLevelTraining();
        levelOtherCommand = options.getLevelOtherCommand();

        // New Day - Formation Icons
        newDayFormationIconOperationalStatus = options.getNewDayFormationIconOperationalStatus();
        newDayFormationIconOperationalStatusStyle = options.getNewDayFormationIconOperationalStatusStyle();

        // Campaign Save
        preferGzippedOutput = options.getPreferGzippedOutput();
        writeCustomsToXML = options.getWriteCustomsToXML();
        writeAllUnitsToXML = options.getWriteAllUnitsToXML();
        saveMothballState = options.getSaveMothballState();

        // Reminders & Confirmations
        for (String key : NAG_IGNORE_KEYS) {
            nagIgnores.put(key, options.getNagDialogIgnore(key));
        }

        // Advanced
        userDir = PreferenceManager.getClientPreferences().getUserDir();
        startGameDelay = options.getStartGameDelay();
        startGameClientDelay = options.getStartGameClientDelay();
        startGameClientRetryCount = options.getStartGameClientRetryCount();
        startGameBotClientDelay = options.getStartGameBotClientDelay();
        startGameBotClientRetryCount = options.getStartGameBotClientRetryCount();
        defaultCompanyGenerationMethod = options.getDefaultCompanyGenerationMethod();
    }

    /**
     * Writes every edited value in this model back to its backing store: most go to the supplied {@link MHQOptions},
     * while the GUI scale is written to {@link GUIPreferences} (triggering a look-and-feel rescale when it changed) and
     * the user directory is written to {@link PreferenceManager}.
     *
     * @param options the options instance to write the model's values into
     */
    void applyTo(MHQOptions options) {
        // Fonts
        options.setMedicalViewDialogHandwritingFont(medicalViewDialogHandwritingFont);

        // Display - General
        options.setDisplayDateFormat(displayDateFormat);
        options.setLongDisplayDateFormat(longDisplayDateFormat);
        // Compare on the integer scale (the slider's units) so floating-point noise in the stored scale cannot trigger
        // a spurious rescale; the look-and-feel is only refreshed when the value actually changed.
        if ((int) (GUIPreferences.getInstance().getGUIScale() * 10) != guiScaleValue) {
            GUIPreferences.getInstance().setValue(GUIPreferences.GUI_SCALE, 0.1 * guiScaleValue);
            MekHQ.updateGuiScaling();
        }
        options.setHideUnitFluff(hideUnitFluff);
        options.setHistoricalDailyLog(historicalDailyLog);
        options.setCompanyGeneratorStartup(companyGeneratorStartup);
        options.setShowCompanyGenerator(showCompanyGenerator);
        options.setShowUnitPicturesOnTOE(showUnitPicturesOnTOE);

        // Display - Interstellar Map
        options.setInterstellarMapShowJumpRadius(interstellarMapShowJumpRadius);
        options.setInterstellarMapShowJumpRadiusMinimumZoom(interstellarMapShowJumpRadiusMinimumZoom);
        options.setInterstellarMapJumpRadiusColour(interstellarMapJumpRadiusColour);
        options.setInterstellarMapShowPlanetaryAcquisitionRadius(interstellarMapShowPlanetaryAcquisitionRadius);
        options.setInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom(
              interstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom);
        options.setInterstellarMapPlanetaryAcquisitionRadiusColour(interstellarMapPlanetaryAcquisitionRadiusColour);
        options.setInterstellarMapShowContractSearchRadius(interstellarMapShowContractSearchRadius);
        options.setInterstellarMapContractSearchRadiusColour(interstellarMapContractSearchRadiusColour);

        // Display - Personnel List
        options.setPersonnelFilterStyle(personnelFilterStyle);
        options.setPersonnelFilterOnPrimaryRole(personnelFilterOnPrimaryRole);
        options.setUnifiedDailyReport(unifiedDailyReport);
        options.setAggregateDailyReport(aggregateDailyReport);

        // Colours
        options.setDeployedForeground(statusColours.get("optionDeployedForeground"));
        options.setDeployedBackground(statusColours.get("optionDeployedBackground"));
        options.setBelowContractMinimumForeground(statusColours.get("optionBelowContractMinimumForeground"));
        options.setBelowContractMinimumBackground(statusColours.get("optionBelowContractMinimumBackground"));
        options.setInTransitForeground(statusColours.get("optionInTransitForeground"));
        options.setInTransitBackground(statusColours.get("optionInTransitBackground"));
        options.setQueuedForTravelForeground(statusColours.get("optionQueuedForTravelForeground"));
        options.setQueuedForTravelBackground(statusColours.get("optionQueuedForTravelBackground"));
        options.setRefittingForeground(statusColours.get("optionRefittingForeground"));
        options.setRefittingBackground(statusColours.get("optionRefittingBackground"));
        options.setMothballingForeground(statusColours.get("optionMothballingForeground"));
        options.setMothballingBackground(statusColours.get("optionMothballingBackground"));
        options.setMothballedForeground(statusColours.get("optionMothballedForeground"));
        options.setMothballedBackground(statusColours.get("optionMothballedBackground"));
        options.setNotRepairableForeground(statusColours.get("optionNotRepairableForeground"));
        options.setNotRepairableBackground(statusColours.get("optionNotRepairableBackground"));
        options.setNonFunctionalForeground(statusColours.get("optionNonFunctionalForeground"));
        options.setNonFunctionalBackground(statusColours.get("optionNonFunctionalBackground"));
        options.setNeedsPartsFixedForeground(statusColours.get("optionNeedsPartsFixedForeground"));
        options.setNeedsPartsFixedBackground(statusColours.get("optionNeedsPartsFixedBackground"));
        options.setUnmaintainedForeground(statusColours.get("optionUnmaintainedForeground"));
        options.setUnmaintainedBackground(statusColours.get("optionUnmaintainedBackground"));
        options.setUncrewedForeground(statusColours.get("optionUncrewedForeground"));
        options.setUncrewedBackground(statusColours.get("optionUncrewedBackground"));
        options.setLoanOverdueForeground(statusColours.get("optionLoanOverdueForeground"));
        options.setLoanOverdueBackground(statusColours.get("optionLoanOverdueBackground"));
        options.setInjuredForeground(statusColours.get("optionInjuredForeground"));
        options.setInjuredBackground(statusColours.get("optionInjuredBackground"));
        options.setHealedInjuriesForeground(statusColours.get("optionHealedInjuriesForeground"));
        options.setHealedInjuriesBackground(statusColours.get("optionHealedInjuriesBackground"));
        options.setPregnantForeground(statusColours.get("optionPregnantForeground"));
        options.setPregnantBackground(statusColours.get("optionPregnantBackground"));
        options.setGoneForeground(statusColours.get("optionGoneForeground"));
        options.setGoneBackground(statusColours.get("optionGoneBackground"));
        options.setAbsentForeground(statusColours.get("optionAbsentForeground"));
        options.setAbsentBackground(statusColours.get("optionAbsentBackground"));
        options.setFatiguedForeground(statusColours.get("optionFatiguedForeground"));
        options.setFatiguedBackground(statusColours.get("optionFatiguedBackground"));
        options.setAwayFromMainForceForeground(statusColours.get("optionAwayFromMainForceForeground"));
        options.setAwayFromMainForceBackground(statusColours.get("optionAwayFromMainForceBackground"));
        options.setStratConHexCoordForeground(statusColours.get("optionStratConHexCoordForeground"));
        options.setFontColorNegative(statusColours.get("optionFontColorNegative"));
        options.setFontColorWarning(statusColours.get("optionFontColorWarning"));
        options.setFontColorPositive(statusColours.get("optionFontColorPositive"));
        options.setFontColorAmazing(statusColours.get("optionFontColorAmazing"));
        options.setFontColorSkillUltraGreen(statusColours.get("optionFontColorSkillUltraGreen"));
        options.setFontColorSkillGreen(statusColours.get("optionFontColorSkillGreen"));
        options.setFontColorSkillRegular(statusColours.get("optionFontColorSkillRegular"));
        options.setFontColorSkillVeteran(statusColours.get("optionFontColorSkillVeteran"));
        options.setFontColorSkillElite(statusColours.get("optionFontColorSkillElite"));

        // Autosave
        options.setNoAutosaveValue(noAutosave);
        options.setAutosaveDailyValue(autosaveDaily);
        options.setAutosaveWeeklyValue(autosaveWeekly);
        options.setAutosaveMonthlyValue(autosaveMonthly);
        options.setAutosaveYearlyValue(autosaveYearly);
        options.setAutosaveBeforeScenariosValue(autosaveBeforeScenarios);
        options.setAutosaveBeforeMissionEndValue(autosaveBeforeMissionEnd);
        options.setMaximumNumberOfAutoSavesValue(maximumNumberOfAutoSaves);

        // New Day - Personnel Pools
        options.setNewDayAsTechPoolFill(newDayPools.get("chkNewDayAstechPoolFill"));
        options.setNewDayAsTechPoolNoRelease(newDayPools.get("chkNewDayAstechPoolNoRelease"));
        options.setNewDayMedicPoolFill(newDayPools.get("chkNewDayMedicPoolFill"));
        options.setNewDayMedicPoolNoRelease(newDayPools.get("chkNewDayMedicPoolNoRelease"));
        options.setNewDaySoldierPoolFill(newDayPools.get("chkNewDaySoldierPoolFill"));
        options.setNewDaySoldierPoolNoRelease(newDayPools.get("chkNewDaySoldierPoolNoRelease"));
        options.setNewDayBattleArmorPoolFill(newDayPools.get("chkNewDayBattleArmorPoolFill"));
        options.setNewDayBattleArmorPoolNoRelease(newDayPools.get("chkNewDayBattleArmorPoolNoRelease"));
        options.setNewDayVehicleCrewGroundPoolFill(newDayPools.get("chkNewDayVehicleCrewGroundPoolFill"));
        options.setNewDayVehicleCrewGroundPoolNoRelease(newDayPools.get("chkNewDayVehicleCrewGroundPoolNoRelease"));
        options.setNewDayVehicleCrewVTOLPoolFill(newDayPools.get("chkNewDayVehicleCrewVTOLPoolFill"));
        options.setNewDayVehicleCrewVTOLPoolNoRelease(newDayPools.get("chkNewDayVehicleCrewVTOLPoolNoRelease"));
        options.setNewDayVehicleCrewNavalPoolFill(newDayPools.get("chkNewDayVehicleCrewNavalPoolFill"));
        options.setNewDayVehicleCrewNavalPoolNoRelease(newDayPools.get("chkNewDayVehicleCrewNavalPoolNoRelease"));
        options.setNewDayVesselPilotPoolFill(newDayPools.get("chkNewDayVesselPilotPoolFill"));
        options.setNewDayVesselPilotPoolNoRelease(newDayPools.get("chkNewDayVesselPilotPoolNoRelease"));
        options.setNewDayVesselGunnerPoolFill(newDayPools.get("chkNewDayVesselGunnerPoolFill"));
        options.setNewDayVesselGunnerPoolNoRelease(newDayPools.get("chkNewDayVesselGunnerPoolNoRelease"));
        options.setNewDayVesselCrewPoolFill(newDayPools.get("chkNewDayVesselCrewPoolFill"));
        options.setNewDayVesselCrewPoolNoRelease(newDayPools.get("chkNewDayVesselCrewPoolNoRelease"));

        // New Day - Automation
        options.setNewDayAutoLogistics(newDayAutoLogistics);
        options.setNewDayMRMS(newDayMRMS);
        options.setNewDayOptimizeMedicalAssignments(newDayOptimizeMedicalAssignments);
        options.setNewDayAutomaticallyAssignUnmaintainedUnits(newDayAutomaticallyAssignUnmaintainedUnits);
        options.setSelfCorrectMaintenance(selfCorrectMaintenance);

        // New Day - Training
        options.setNewMonthQuickTrain(newMonthQuickTrain);
        options.setQuickTrainTarget(quickTrainTarget);
        options.setLevelArtillery(levelArtillery);
        options.setLevelScouting(levelScouting);
        options.setLevelEscape(levelEscape);
        options.setLevelLeadership(levelLeadership);
        options.setLevelTraining(levelTraining);
        options.setLevelOtherCommand(levelOtherCommand);

        // New Day - Formation Icons
        options.setNewDayFormationIconOperationalStatus(newDayFormationIconOperationalStatus);
        options.setNewDayFormationIconOperationalStatusStyle(newDayFormationIconOperationalStatusStyle);

        // Campaign Save
        options.setPreferGzippedOutput(preferGzippedOutput);
        options.setWriteCustomsToXML(writeCustomsToXML);
        options.setWriteAllUnitsToXML(writeAllUnitsToXML);
        options.setSaveMothballState(saveMothballState);

        // Reminders & Confirmations
        for (String key : NAG_IGNORE_KEYS) {
            options.setNagDialogIgnore(key, nagIgnores.get(key));
        }

        // Advanced
        PreferenceManager.getClientPreferences().setUserDir(userDir);
        PreferenceManager.getInstance().save();
        options.setStartGameDelay(startGameDelay);
        options.setStartGameClientDelay(startGameClientDelay);
        options.setStartGameClientRetryCount(startGameClientRetryCount);
        options.setStartGameBotClientDelay(startGameBotClientDelay);
        options.setStartGameBotClientRetryCount(startGameBotClientRetryCount);
        options.setDefaultCompanyGenerationMethod(defaultCompanyGenerationMethod);
    }
}
