package mekhq.gui.dialog.helpDialogs;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import mekhq.utilities.Internationalization;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AutoResolveSimulationLogDialog extends AbstractHelpDialog {

    public AutoResolveSimulationLogDialog(final JFrame frame, File logFile) {
        super(frame, Internationalization.getText("AutoResolveSimulationLogDialog.title"),
            logFile.getAbsolutePath());

        setMinimumSize(new Dimension(800, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
    }

}
