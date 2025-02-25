package mekhq.gui.dialog.helpDialogs;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.common.internationalization.Internationalization;
import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class AutoResolveBehaviorSettingsHelpDialog extends AbstractHelpDialog {

    /**
     * Creates a new instance of AutoResolveBehaviorSettingsHelpDialog.
     * This screen opens a help dialog, using the megamek help dialog, which open an HTML file
     * @param frame  parent frame
     */
    public AutoResolveBehaviorSettingsHelpDialog(final JFrame frame) {
        super(frame, Internationalization.getText("AutoResolveBehaviorSettingsDialog.title"),
            Internationalization.getText("AutoResolveBehaviorSettingsDialog.autoResolveHelpPath"));

        setMinimumSize(new Dimension(400, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
    }

}
