package mekhq.gui.dialog.helpDialogs;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import mekhq.MekHQ;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class AutoResolveBehaviorSettingsHelpDialog extends AbstractHelpDialog {

    private static final ResourceBundle resourceMap = ResourceBundle.getBundle(
        "mekhq.resources.AutoResolveBehaviorSettingsDialog",
        MekHQ.getMHQOptions().getLocale());


    /**
     * Creates a new instance of AutoResolveBehaviorSettingsHelpDialog.
     * <p>
     * This screen opens a help dialog, using the megamek help dialog, which open an HTML file
     * </p>
     * @param frame  parent frame
     */
    public AutoResolveBehaviorSettingsHelpDialog(final JFrame frame) {
        super(frame, resourceMap.getString("AutoResolveBehaviorSettingsDialog.title"),
            resourceMap.getString("AutoResolveBehaviorSettingsDialog.autoResolveHelpPath"));

        setMinimumSize(new Dimension(400, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
    }

}
