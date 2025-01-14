package mekhq.gui.campaignOptions.factoryClasses;

import megamek.common.annotations.Nullable;

import javax.swing.*;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * This class provides a custom {@link JTextField} for campaign options.
 * The text field name and tooltips are fetched from a resource bundle based on the provided name.
 */
public class CampaignOptionsTextField extends JTextField {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a {@link JTextField} object.
     * <p>
     * The name of the {@link JTextField} will be {@code}, and it will use the following resource
     * bundle reference: {@code "lbl" + name + ".tooltip"}.
     *
     * @param name                the name of the object.
     */
    public CampaignOptionsTextField(String name) {
        this(name, null);
    }

    /**
     * Creates a {@link JTextField} object with a custom word wrap width.
     * <p>
     * The name of the {@link JTextField} will be {@code}, and it will use the following resource
     * bundle reference: {@code "lbl" + name + ".tooltip"}.
     *
     * @param name                the name of the object.
     * @param customWrapSize      the maximum number of characters (including whitespaces) on each
     *                            line of the tooltip.
     *                            If {@code null}, the default wrap size of 100 is used.
     */
    public CampaignOptionsTextField(String name, @Nullable Integer customWrapSize) {
        super();
        setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
            processWrapSize(customWrapSize)));
        setName("lbl" + name);

        setFontScaling(this, false, 1);
    }
}
