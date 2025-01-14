package mekhq.gui.campaignOptions.factoryClasses;

import megamek.common.annotations.Nullable;

import javax.swing.*;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * This class provides a custom {@link JCheckBox} for campaign options.
 * The checkbox name and tooltips are fetched from a resource bundle based on the provided name.
 */
public class CampaignOptionsCheckBox extends JCheckBox {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    public CampaignOptionsCheckBox(String name) {
        this(name, null);
    }

    /**
     * Returns a new {@link JCheckBox} object with a custom wrap size.
     * <p>
     * The {@link JCheckBox} will be named {@code "chk" + name}, and use the following resource
     * bundle references: {@code "lbl" + name + ".text"} and {@code "lbl" + name + ".tooltip"}.
     *
     * @param name    the name of the checkbox
     * @param customWrapSize    the maximum number of characters (including whitespaces) on each
     *                         line of the tooltip (or 100, if {@code null}).
     */
    public CampaignOptionsCheckBox(String name, @Nullable Integer customWrapSize) {
        super(String.format("<html>%s</html>", resources.getString("lbl" + name + ".text")));
        setName("chk" + name);
        setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
            processWrapSize(customWrapSize)));

        setFontScaling(this, false, 1);
    }
}
