/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.io.File;
import java.util.Optional;

import javax.swing.JFrame;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.io.FileType;

/**
 * Utility class with methods to show the various open/save file dialogs
 */
public class FileDialogs {

    private FileDialogs() {
        // no instances
    }

    /**
     * Displays a dialog window from which the user can select an <tt>.xml</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openPersonnel(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Personnel",
                                   new File("."), //$NON-NLS-1$
                                   FileType.PRSX );
    }

    /**
     * Displays a dialog window from which the user can select an <tt>.xml</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> savePersonnel(JFrame frame, Campaign campaign) {

        String fileName = String.format( "%s%_ExportedPersonnel.prsx", //$NON-NLS-1$
                                         campaign.getName(),
                                         campaign.getShortDateAsString() );

        return GUI.fileDialogSave( frame,
                                   "Save Personnel",
                                   new File(".", fileName), //$NON-NLS-1$
                                   FileType.PRSX);
    }

    /**
     * Displays a dialog window from which the user can select an <tt>.xml</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openCampaignOptions(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Campaign Options",
                                   new File("."), //$NON-NLS-1$
                                   FileType.XML );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.parts</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openParts(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Pards",
                                   new File("."), //$NON-NLS-1$
                                   FileType.PARTS );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.parts</tt> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveParts(JFrame frame, Campaign campaign) {

        String fileName = String.format( "%s%s_ExportedParts.parts", //$NON-NLS-1$
                                         campaign.getName(),
                                         campaign.getShortDateAsString() );

        return GUI.fileDialogSave( frame,
                                   "Save Pards",
                                   new File(".", fileName), //$NON-NLS-1$
                                   FileType.PARTS );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.tsv</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openPlanetsTsv(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Planets from SUCS format TSV file",
                                   new File("."), //$NON-NLS-1$
                                   FileType.TSV );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.mul</tt> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveCampaignOptions(JFrame frame) {
        return GUI.fileDialogSave( frame,
                                   "Save Campaign Options as Presets",
                                   new File(MekHQ.PRESET_DIR, "myoptions.xml"), //$NON-NLS-1$
                                   FileType.XML );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.png</tt> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveStarMap(JFrame frame) {
        return GUI.fileDialogSave( frame,
                                   "",
                                   new File(".", "starmap.png"), //$NON-NLS-1$ //$NON-NLS-2$
                                   FileType.PNG );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.mul</tt> file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openUnits(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Units",
                                   new File("."), //$NON-NLS-1$
                                   FileType.MUL );
    }

    /**
     * Displays a dialog window from which the user can select a <tt>.mul</tt> file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveDeployUnits(JFrame frame, Scenario scenario) {
        return GUI.fileDialogSave( frame,
                                   "Deploy Units",
                                   new File(".", scenario.getName() + ".mul"), //$NON-NLS-1$ //$NON-NLS-2$
                                   FileType.MUL );
    }

    /**
     * Displays a dialog window from which the user can select a campaign file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> openCampaign(JFrame frame) {
        return GUI.fileDialogOpen( frame,
                                   "Load Campaign",
                                   new File(MekHQ.CAMPAIGN_DIRECTORY),
                                   FileType.CPNX );
    }

    /**
     * Displays a dialog window from which the user can select a campaign file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> saveCampaign(JFrame frame, Campaign campaign) {

        String fileName = String.format( "%s%s.%s", //$NON-NLS-1$
                                         campaign.getName(),
                                         campaign.getShortDateAsString(),
                                         campaign.getPreferGzippedOutput() ? "cpnx.gz" : "cpnx" );

        return GUI.fileDialogSave( frame,
                                   "Save Campaign",
                                   new File(MekHQ.CAMPAIGN_DIRECTORY, fileName),
                                   FileType.CPNX );
    }

}
