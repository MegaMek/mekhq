package mekhq.gui;

import java.awt.FileDialog;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JFrame;

import mekhq.MekHQ;

/**
 * GUI utility methods
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
    public static Optional<File> openFileDialog( JFrame parent,
                                                 String title,
                                                 File initialDirectory,
                                                 Predicate<File> fileFilter ) {
        return fileDialog(parent, title, fileFilter, fd -> {
            fd.setDirectory(initialDirectory.getAbsolutePath());
        });
    }

    /**
     * Displays a dialog window from which the user can select a file to save to.
     * 
     * @return the file selected, if any
     */
    public static Optional<File> saveFileDialog( JFrame parent,
                                                 String title,
                                                 File initialFile,
                                                 Predicate<File> fileFilter ) {
        return fileDialog(parent, title, fileFilter, fd -> {
            fd.setDirectory(initialFile.getParentFile().getAbsolutePath());
            fd.setFile(initialFile.getName());
        });
    }

    private static Optional<File> fileDialog( JFrame parent,
                                              String title,
                                              Predicate<File> fileFilter,
                                              Consumer<FileDialog> setPath ) {
        // Yes, FileDialog is an old AWT class :)
        // Swing's JFileChooser is much less usable that FileDialog
        FileDialog fd = new FileDialog(Objects.requireNonNull(parent), Objects.requireNonNull(title));
        setPath.accept(fd);
        fd.setMode(fd.getFile() == null ? FileDialog.LOAD : FileDialog.SAVE);
        fd.setFilenameFilter((dir, file) -> fileFilter.test(new File(dir, file)));
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        return (dir != null) && (file != null)
             ? Optional.of(new File(dir, file))
             : Optional.empty();
    }

}
