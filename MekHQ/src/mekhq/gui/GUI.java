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

import java.awt.FileDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import mekhq.MekHQ;
import mekhq.io.FileType;

/**
 * GUI/Swing utility methods
 */
public class GUI {
    private static final String PROP_KEY_FILE_DIALOG_KIND       = "gui.file.dialogs"; //$NON-NLS-1$
    private static final String PROP_VAL_FILE_DIALOG_KIND_AWT   = "awt"; //$NON-NLS-1$
    private static final String PROP_VAL_FILE_DIALOG_KIND_SWING = "swing"; //$NON-NLS-1$

    // Please bear with this for now :-) this stuff is going into MekHQ
    // when that class is cleaned up (ie: "soon")
    //
    // LATER use the preferences from MekHQ
    //       also, document this property (eg as below)
    //
    //    # Determines which open/save file dialogs MekHQ uses.
    //    #
    //    # Supported values:
    //    #  awt:   use awt FileDialog (default)
    //    #  swing: use swing JFileChooser
    //    #
    //    gui.file.dialogs=awt
    private static Properties mhqPreferences;
    static {
        mhqPreferences = new Properties();
        try {
            mhqPreferences.load(new FileInputStream(MekHQ.PREFERENCES_FILE));
        } catch (@SuppressWarnings("unused") IOException e) {
            // ignored
        }
    }

    private GUI() {
        // no instances, only static methods on this class
    }

    /**
     * Displays a dialog window from which the user can select a file to open.
     *
     * @return the file selected, if any
     */
    public static Optional<File> fileDialogOpen(JFrame parent, String title, FileType fileType, String directoryPath) {
        return fileDialog(parent, title, fileType, directoryPath, null);
    }

    /**
     * Displays a dialog window from which the user can select a file to save to.
     *
     * @return the file selected, if any
     */
    public static Optional<File> fileDialogSave(JFrame parent, String title, FileType fileType, String directoryPath, String saveFilename) {
        return fileDialog(parent, title, fileType, directoryPath, saveFilename);
    }

    private static Optional<File> fileDialog(JFrame parent, String title, FileType fileType, String directoryPath, String saveFilename ) {
        String property = mhqPreferences.getProperty(PROP_KEY_FILE_DIALOG_KIND, PROP_VAL_FILE_DIALOG_KIND_AWT).trim().toLowerCase();
        switch (property) {
            case PROP_VAL_FILE_DIALOG_KIND_SWING:
                return swingFileDialog(parent, title, fileType, directoryPath, saveFilename);
            case PROP_VAL_FILE_DIALOG_KIND_AWT:
            default:
                return awtFileDialog(parent, title, fileType, directoryPath, saveFilename);
        }
    }

    private static Optional<File> awtFileDialog(JFrame parent, String title, FileType fileType, String directoryPath, String saveFilename) {
        FileDialog fd = new FileDialog(parent, title);
        fd.setDirectory(directoryPath);
        if (saveFilename != null) {
            fd.setMode(FileDialog.SAVE);
            fd.setFile(saveFilename);
        } else {
            fd.setMode(FileDialog.LOAD);
        }
        fd.setFilenameFilter((dir, file) -> fileType.getNameFilter().test(file));
        fd.setVisible(true);
        String f = fd.getFile();
        String d = fd.getDirectory();
        if ((f != null) && (d != null)) {
            return Optional.of(new File(d, f));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<File> swingFileDialog(JFrame parent, String title, FileType fileType, String directoryPath, String saveFilename) {
        JFileChooser fd = new JFileChooser(directoryPath);
        fd.setDialogTitle(title);
        if (saveFilename != null) {
            fd.setSelectedFile(new File(saveFilename));
        }
        fd.addChoosableFileFilter(new FileNameExtensionFilter(fileType.getDescription(), fileType.getExtensions().toArray(new String[0])));
        int buttonClicked = saveFilename != null
                          ? fd.showSaveDialog(parent)
                          : fd.showOpenDialog(parent);
        return buttonClicked == JFileChooser.APPROVE_OPTION
             ? Optional.ofNullable(fd.getSelectedFile())
             : Optional.empty();
    }
}
