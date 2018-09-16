/*
 * Export.java
 * 
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.TableModel;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Planets;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;
import mekhq.io.FileType;

public class Export {

    private static IExport exportCSV = new ExportCSV();
    private static IExport exportXML = new ExportXML();


    /**
     * Normalizes the given file to one ending with the specified extension.
     * @param file
     * @param format
     * @return File
     */
    private static File checkFileEnding(File file, String format) {
        String path = file.getPath();
        if (!path.endsWith("." + format)) {
            path += "." + format;
            file = new File(path);
        }
        return file;
    }

    /**
     * Checks if a file already exists, if so it makes a backup copy.
     * Returns true if a backup is made, false if otherwise.
     * @param file
     * @param backupDir
     * @return boolean
     */
    private static boolean backupFileInto(File file, String backupDir) {
        // check for existing file and make a back-up if found
        String path2 = backupDir + "_backup";
        File backupFile = new File(path2);
        if (file.exists()) {
            try {
                Files.move(Paths.get(file.getPath()), Paths.get(backupFile.getPath()));
                return true;
            } catch (IOException e) {
                MekHQ.getLogger().error(Export.class, "backupFileInto", "IOException backing up file.");
            }
        }
        return false;
    }

    /**
     * Exports JTable to a file (CSV) TODO: (XML, JSON, HTML)
     * @param table
     * @param format
     * @param dialogTitle
     * @param frame
     * @param campaign
     */
    public static void exportTable(TableModel table, FileType format, String dialogTitle, JFrame frame, Campaign campaign) {
        Optional<File> maybeFile = FileDialogs.saveTable(frame, dialogTitle, format, campaign);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.CSV) {
            report = exportCSV.exportTable(table, file);
        } else if (format == FileType.XML) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.JSON) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.HTML) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        JOptionPane.showMessageDialog(frame, report);
    }

    /**
     * Exports Finances to a file (CSV) TODO: (HTML, XML, XML)
     * @param format
     * @param frame
     * @param campaign
     */
    public static void exportFinances(FileType format, JFrame frame, Campaign campaign) {
        Optional<File> maybeFile = FileDialogs.saveFinances(frame, campaign, format);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.CSV) {
            report = exportCSV.exportFinances(file, format.getRecommendedExtension(), campaign);
        } else if (format == FileType.JSON) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.HTML) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.XML) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        JOptionPane.showMessageDialog(frame, report);
    }

    /**
     * Exports Planets to a file (XML) TODO: (HTML, CSV, JSON)
     * @param format
     * @param frame
     */
    public static void exportPlanets(FileType format, JFrame frame, Campaign campaign) {
        Optional<File> maybeFile = FileDialogs.savePlanets(frame, campaign, format);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.XML) {
            report = Planets.getInstance().exportPlanets(file.getPath(), format.getRecommendedExtension());
        } else if (format == FileType.JSON) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.HTML) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.CSV) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        JOptionPane.showMessageDialog(frame, report);
    }

    /**
     * Save campaign options to an XML file
     * @param format
     * @param frame
     * @param campaign
     * 
     */
    public static void saveOptionsFile(FileType format, JFrame frame, Campaign campaign) throws IOException {
        Optional<File> maybeFile = FileDialogs.saveCampaignOptions(frame);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.XML) {
            report = exportXML.exportCampaignOptions(file, campaign);
        }
        JOptionPane.showMessageDialog(frame, report);
    }

    /**
     * Save selected warehouse parts to a file (PARTS) TODO: (JSON, CSV, HTML)
     * @param format
     * @param frame
     * @param campaign
     * @param gui
     */
    public static void savePartsFile(FileType format, JFrame frame, Campaign campaign, CampaignGUI gui) throws IOException {
        Optional<File> maybeFile = FileDialogs.saveParts(frame, campaign, format);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.PARTS) { //.parts is XML
            report = exportXML.exportParts(file, gui);
        } else if (format == FileType.CSV) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.HTML) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.JSON) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        JOptionPane.showMessageDialog(frame, report);
    }

    /**
     * Save one or more selected personnel to a file (PRSX) TODO: (HTML, CSV, JSON)
     * @param format
     * @param frame
     * @param campaign
     * @param gui
     */
    public static void savePersonnelFile(FileType format, JFrame frame, Campaign campaign, CampaignGUI gui) throws IOException {
        Optional<File> maybeFile = FileDialogs.savePersonnel(frame, campaign, format);

        if (!maybeFile.isPresent()) {
            return;
        }
        File file = maybeFile.get();

        file = checkFileEnding(file, format.getRecommendedExtension());
        backupFileInto(file, file.getPath());

        String report = "";
        if (format == FileType.PRSX) { //.prsx is XML
            report = exportXML.exportPersonnelFile(file, gui);
        } else if (format == FileType.JSON) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.CSV) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (format == FileType.HTML) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
        JOptionPane.showMessageDialog(frame, report);
    }
}
