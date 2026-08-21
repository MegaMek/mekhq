/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.swing.JFrame;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.gui.utilities.ObservableString;
import mekhq.io.FileType;
import mekhq.utilities.MHQInternationalization;

/**
 * Utility class with methods to show the various open/save file dialogs
 */
public class FileDialogs {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.FileDialogs";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
          DateTimeFormatter.ofPattern(MHQConstants.FILENAME_DATE_FORMAT)
                .withLocale(MekHQ.getMHQOptions().getDateLocale());

    private FileDialogs() {
        // no instances
    }

    /**
     * Displays a dialog window from which the user can select an <code>.xml</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openPersonnel(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Personnel",
              FileType.PRSX,
              MekHQ.getPersonnelDirectory().getValue());

        value.ifPresent(x -> MekHQ.getPersonnelDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select an <code>.xml</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> savePersonnel(JFrame frame, Campaign campaign) {

        String fileName = String.format(
              "%s%s_ExportedPersonnel.prsx",
              campaign.getPlayerForce().getName(),
              campaign.getLocalDate().format(DATE_TIME_FORMATTER));

        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Save Personnel",
              FileType.PRSX,
              MekHQ.getPersonnelDirectory().getValue(),
              fileName);

        value.ifPresent(x -> MekHQ.getPersonnelDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select an <code>.xml</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openRankSystems(final JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(frame, "Load Rank Systems",
              FileType.XML, MekHQ.getMHQOptions().getRankSystemsPath());
        value.ifPresent(x -> MekHQ.getMHQOptions().setRankSystemsPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.xml</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveRankSystems(final JFrame frame) {
        Optional<File> value = GUI.fileDialogSave(frame, "Save Rank Systems", FileType.XML,
              MekHQ.getMHQOptions().getRankSystemsPath(), "rankSystem.xml");
        value.ifPresent(x -> MekHQ.getMHQOptions().setRankSystemsPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select an <code>.xml</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openIndividualRankSystem(final JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(frame, "Load Individual Rank System",
              FileType.XML, MekHQ.getMHQOptions().getIndividualRankSystemPath());
        value.ifPresent(x -> MekHQ.getMHQOptions().setIndividualRankSystemPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.xml</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveIndividualRankSystem(final JFrame frame) {
        Optional<File> value = GUI.fileDialogSave(frame, "Save Individual Rank System",
              FileType.XML, MekHQ.getMHQOptions().getIndividualRankSystemPath(),
              "individualRankSystem.xml");
        value.ifPresent(x -> MekHQ.getMHQOptions().setIndividualRankSystemPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.png</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> exportLayeredFormationIcon(final JFrame frame) {
        Optional<File> value = GUI.fileDialogSave(frame, "Export Layered Formation Icon",
              FileType.PNG, MekHQ.getMHQOptions().getLayeredFormationIconPath(),
              "layeredFormationIcon.png");
        value.ifPresent(x -> MekHQ.getMHQOptions().setLayeredFormationIconPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.parts</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openParts(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Parts",
              FileType.PARTS,
              MekHQ.getPartsDirectory().getValue());

        value.ifPresent(x -> MekHQ.getPartsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.parts</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveParts(JFrame frame, Campaign campaign) {
        String fileName = String.format(
              "%s%s_ExportedParts.parts",
              campaign.getPlayerForce().getName(),
              campaign.getLocalDate().format(DATE_TIME_FORMATTER));

        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Save Parts",
              FileType.PARTS,
              MekHQ.getPartsDirectory().getValue(),
              fileName);

        value.ifPresent(x -> MekHQ.getPartsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.mul</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openUnits(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Units",
              FileType.MUL,
              MekHQ.getUnitsDirectory().getValue());

        value.ifPresent(x -> MekHQ.getUnitsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.mul</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveDeployUnits(JFrame frame, Scenario scenario, String name) {
        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Deploy Units",
              FileType.MUL,
              MekHQ.getUnitsDirectory().getValue(),
              scenario.getName() + " - " + name + ".mul");

        value.ifPresent(x -> MekHQ.getUnitsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.mul</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveUnits(JFrame frame, String name) {
        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Save Units",
              FileType.MUL,
              MekHQ.getUnitsDirectory().getValue(),
              name + ".mul");

        value.ifPresent(x -> MekHQ.getUnitsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a campaign file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openCampaign(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Campaign",
              FileType.CPNX,
              MekHQ.getCampaignsDirectory().getValue());

        value.ifPresent(x -> MekHQ.getCampaignsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a campaign file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveCampaign(JFrame frame, Campaign campaign) {
        String fileName = String.format("%s%s.%s", campaign.getPlayerForce().getName(),
              campaign.getLocalDate().format(DATE_TIME_FORMATTER),
              MekHQ.getMHQOptions().getPreferGzippedOutput() ? "cpnx.gz" : "cpnx");

        Optional<File> value = GUI.fileDialogSave(frame, "Save Campaign", FileType.CPNX,
              MekHQ.getCampaignsDirectory().getValue(), fileName);

        value.ifPresent(x -> MekHQ.getCampaignsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a scenario template file to open
     *
     * @return the file selected, if any
     */
    public static Optional<File> openScenarioTemplate(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Scenario Template",
              FileType.SCENARIO_TEMPLATE,
              scenarioTemplateStartDirectory());

        value.ifPresent(x -> MekHQ.getScenarioTemplatesDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * The directory the scenario template file dialogs open at: the user's remembered location once they have picked
     * one, otherwise the canonical {@code mm-data/data/scenariotemplates} tree (falling back to {@code ./data} in a
     * packaged release). Mirrors how the other StratCon/scenario Developer Tools default to the authoritative data.
     *
     * @return the starting directory path
     */
    private static String scenarioTemplateStartDirectory() {
        String remembered = MekHQ.getScenarioTemplatesDirectory().getValue();
        if ((remembered == null) || remembered.isBlank() || remembered.equals(".")) {
            return developerDataDirectory("scenariotemplates");
        }
        return remembered;
    }

    /**
     * Displays a dialog window from which the user can select a scenario template file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveScenarioTemplate(JFrame frame, ScenarioTemplate template) {
        String fileName = String.format(
              "%s.json",
              template.name);

        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Save Scenario Template",
              FileType.SCENARIO_TEMPLATE,
              scenarioTemplateStartDirectory(),
              fileName);

        value.ifPresent(x -> MekHQ.getScenarioTemplatesDirectory().setValue(x.getParent()));
        return value;
    }

    private static final String SCENARIO_MODIFIER_DIRECTORY = developerDataDirectory("scenariomodifiers");
    private static final String CONTRACT_DEFINITION_DIRECTORY = developerDataDirectory("stratconcontractdefinitions");
    private static final String STRAT_CON_FACILITY_DIRECTORY = developerDataDirectory("stratconfacilities");

    /**
     * Resolves a data subdirectory for the StratCon/scenario Developer Tools editors. In a source checkout the
     * canonical {@code mm-data/data} tree sits two levels above the MekHQ working directory; editing there keeps the
     * authoritative files in sync rather than the disposable staged {@code ./data} copy (which a build overwrites from
     * mm-data). Packaged releases have no sibling {@code mm-data}, so this falls back to the runtime {@code ./data}
     * tree.
     *
     * @param subdirectory the data subdirectory name (e.g. {@code "scenariomodifiers"})
     *
     * @return the canonical mm-data path when it exists, otherwise the {@code ./data} path
     */
    private static String developerDataDirectory(String subdirectory) {
        File canonical = new File("../../mm-data/data/" + subdirectory);
        if (canonical.isDirectory()) {
            return canonical.getPath();
        }
        return "./data/" + subdirectory;
    }

    /**
     * Displays a dialog window from which the user can select a scenario modifier file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openScenarioModifier(JFrame frame) {
        return GUI.fileDialogOpen(frame, "Load Scenario Modifier", FileType.JSON, SCENARIO_MODIFIER_DIRECTORY);
    }

    /**
     * Displays a dialog window from which the user can select a scenario modifier file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveScenarioModifier(JFrame frame, AtBScenarioModifier modifier) {
        String fileName = (modifier.getModifierName() == null ? "modifier" : modifier.getModifierName()) + ".json";
        return GUI.fileDialogSave(frame,
              "Save Scenario Modifier",
              FileType.JSON,
              SCENARIO_MODIFIER_DIRECTORY,
              fileName);
    }

    /**
     * Displays a dialog window from which the user can select a contract definition file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openContractDefinition(JFrame frame) {
        return GUI.fileDialogOpen(frame, "Load Contract Definition", FileType.JSON, CONTRACT_DEFINITION_DIRECTORY);
    }

    /**
     * Displays a dialog window from which the user can select a contract definition file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveContractDefinition(JFrame frame, StratConContractDefinition definition) {
        String name = definition.getContractTypeName();
        String fileName = ((name == null) || name.isBlank() ? "contract" : name) + ".json";
        return GUI.fileDialogSave(frame, "Save Contract Definition", FileType.JSON, CONTRACT_DEFINITION_DIRECTORY,
              fileName);
    }

    /**
     * Displays a dialog window from which the user can select a StratCon facility file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openStratConFacility(JFrame frame) {
        return GUI.fileDialogOpen(frame, "Load StratCon Facility", FileType.JSON, STRAT_CON_FACILITY_DIRECTORY);
    }

    /**
     * Displays a dialog window from which the user can select a StratCon facility file to save to. The suggested file
     * name follows the shipped convention of an owner prefix plus the display name (e.g. {@code AlliedAirBase.json}).
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveStratConFacility(JFrame frame, StratConFacility facility) {
        String display = facility.getDisplayableName();
        String fileName;
        if ((display == null) || display.isBlank()) {
            fileName = "facility";
        } else {
            String prefix = facility.isOwnerAlliedToPlayer() ? "Allied" : "Hostile";
            fileName = prefix + display.replaceAll("[^A-Za-z0-9]", "");
        }
        return GUI.fileDialogSave(frame, "Save StratCon Facility", FileType.JSON, STRAT_CON_FACILITY_DIRECTORY,
              fileName + ".json");
    }

    /**
     * Displays a dialog window from which the user can select a <code>.tsv</code> file to open.
     *
     * @return the file selected, if any
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static Optional<File> openPlanetsTsv(JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(
              frame,
              "Load Planets from SUCS format TSV file",
              FileType.TSV,
              MekHQ.getPlanetsDirectory().getValue());

        value.ifPresent(x -> MekHQ.getPlanetsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.png</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveStarMap(JFrame frame) {
        Optional<File> value = GUI.fileDialogSave(
              frame,
              "Save star map to PNG file",
              FileType.PNG,
              MekHQ.getStarMapsDirectory().getValue(),
              "starmap.png");

        value.ifPresent(x -> MekHQ.getStarMapsDirectory().setValue(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.csv</code> file to save personnel to. Uses
     * <code>[Campaign Name]</code><code>[Date]</code>_ExportPersonnel.csv as default filename.
     */
    public static Optional<File> savePersonnelCSV(JFrame frame, Campaign campaign) {
        return saveWithBackup(frame, getTextAt("dlgSavePersonnelCSV.title"), MekHQ.getPersonnelDirectory(),
              getDefaultFilename(campaign, getTextAt("dlgSavePersonnelCSV.fileSuffix")), FileType.CSV);
    }

    /**
     * Displays a dialog window from which the user can select a <code>.csv</code> file to save units to. Uses
     * <code>[Campaign Name]</code><code>[Date]</code>_ExportUnits.csv as default filename.
     */
    public static Optional<File> saveUnitsCSV(JFrame frame, Campaign campaign) {
        return saveWithBackup(frame, getTextAt("dlgSaveUnitsCSV.title"), MekHQ.getUnitsDirectory(),
              getDefaultFilename(campaign, getTextAt("dlgSaveUnitsCSV.fileSuffix")), FileType.CSV);
    }

    /**
     * Displays a dialog window from which the user can select a <code>.csv</code> file to save finances to. Uses
     * <code>[Campaign Name]</code><code>[Date]</code>_ExportFinances.csv as default filename.
     */
    public static Optional<File> saveFinancesCSV(JFrame frame, Campaign campaign) {
        return saveWithBackup(frame, getTextAt("dlgSaveFinancesCSV.title"), MekHQ.getFinancesDirectory(),
              getDefaultFilename(campaign, getTextAt("dlgSaveFinancesCSV.fileSuffix")), FileType.CSV);
    }

    /**
     * Displays a dialog pointing at the default directory where the user can save a file, ensures its extension, and
     * creates a backup if it already exists.
     *
     * <p>
     * To streamline UX, the dialog show default directory and pre-populates output file. After file selection is done,
     * makes file's parent directory default.
     * </p>
     *
     * @param frame           dialog parent frame
     * @param dialogTitle     title of the dialog frame
     * @param defaultDir      default save directory
     * @param defaultFilename default file name, excluding extension
     * @param fileType        file type to be enforced
     *
     * @return a file user chose to save to, <code>Optional.empty</code> if the dialog was canceled
     */
    public static Optional<File> saveWithBackup(JFrame frame, String dialogTitle,
          ObservableString defaultDir, String defaultFilename, FileType fileType) {
        String saveFilename = defaultFilename + '.' + fileType.getRecommendedExtension();
        Optional<File> selectedFile = GUI.fileDialogSave(frame,
              dialogTitle,
              fileType,
              defaultDir.getValue(),
              saveFilename);
        Optional<File> outputFile = selectedFile.map(file -> enforceFileExtension(file, fileType));

        outputFile.ifPresent(file -> defaultDir.setValue(file.getParent()));
        // if the file already exists, make a backup copy
        outputFile.filter(File::exists)
              .ifPresent(file -> Utilities.copyfile(file, new File(file.getPath() + "_backup")));

        return outputFile;
    }

    /**
     * Ensures that the file has the appropriate file type extension.
     *
     * @param file     the file to check
     * @param fileType enforced file type
     *
     * @return File with the appropriate file type extension
     */
    private static File enforceFileExtension(File file, FileType fileType) {
        String path = file.getPath();
        if (!path.endsWith('.' + fileType.getRecommendedExtension())) {
            path += '.' + fileType.getRecommendedExtension();
            file = new File(path);
        }
        return file;
    }

    /**
     * Displays a dialog window from which the user can select an <code>.xml</code> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openCompanyGenerationOptions(final JFrame frame) {
        Optional<File> value = GUI.fileDialogOpen(frame, "Load Company Generation Options",
              FileType.XML, MekHQ.getMHQOptions().getCompanyGenerationDirectoryPath());

        value.ifPresent(x -> MekHQ.getMHQOptions().setCompanyGenerationDirectoryPath(x.getParent()));
        return value;
    }

    /**
     * Displays a dialog window from which the user can select a <code>.xml</code> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveCompanyGenerationOptions(final JFrame frame) {
        Optional<File> value = GUI.fileDialogSave(frame, "Save Company Generation Options",
              FileType.XML, MekHQ.getMHQOptions().getCompanyGenerationDirectoryPath(),
              "myoptions.xml");

        value.ifPresent(x -> MekHQ.getMHQOptions().setCompanyGenerationDirectoryPath(x.getParent()));
        return value;
    }

    private static String getDefaultFilename(Campaign campaign, String filenameSuffix) {
        return campaign.getPlayerForce().getName() + campaign.getLocalDate().format(DATE_TIME_FORMATTER) + "_" + filenameSuffix;
    }


    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

}
