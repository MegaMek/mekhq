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

import static java.util.Objects.requireNonNull;

import java.awt.FileDialog;
import java.io.File;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JFrame;

import mekhq.io.FileType;

/**
 * GUI/Swing utility methods
 */
public class GUI {

    private GUI() {
        // no instances
    }

    /**
     * Displays a dialog window from which the user can select a file to open.
     * 
     * @return the file selected, if any
     */
    public static Optional<File> fileDialogOpen( JFrame parent,
                                                 String title,
                                                 File initialDirectory,
                                                 FileType fileType ) {
        return fileDialog(parent, title, fileType, fd -> {
            fd.setDirectory(initialDirectory.getAbsolutePath());
        });
    }

    /**
     * Displays a dialog window from which the user can select a file to save to.
     * 
     * @return the file selected, if any
     */
    public static Optional<File> fileDialogSave( JFrame parent,
                                                 String title,
                                                 File initialFile,
                                                 FileType fileType ) {
        return fileDialog(parent, title, fileType, fd -> {
            fd.setDirectory(initialFile.getParentFile().getAbsolutePath());
            fd.setFile(initialFile.getName());
        });
    }

    private static Optional<File> fileDialog( JFrame parent,
                                              String title,
                                              FileType fileType,
                                              Consumer<FileDialog> setPath ) {
        // Yes, FileDialog is an old AWT class :)
        // Swing's JFileChooser is much less usable that FileDialog
        FileDialog fd = new FileDialog(parent, requireNonNull(title));
        setPath.accept(fd);
        fd.setMode(fd.getFile() == null ? FileDialog.LOAD : FileDialog.SAVE);
        fd.setFilenameFilter((dir, file) -> fileType.getNameFilter().test(file.toLowerCase()));
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        return (dir != null) && (file != null)
             ? Optional.of(new File(dir, file))
             : Optional.empty();
    }

}
