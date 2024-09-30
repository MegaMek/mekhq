package mekhq.gui.panes.campaignOptions;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.panes.campaignOptions.panes.GeneralTab;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

public class CampaignOptionsDialogController extends AbstractMHQTabbedPane {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsDialogController.class);
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;

    public CampaignOptionsDialogController(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, resources, "campaignOptionsDialog");
        this.campaign = campaign;

        initialize();
    }

    @Override
    protected void initialize() {
        GeneralTab generalTab = new GeneralTab(campaign, getFrame(), "generalTab");
        addTab(String.format("<html><b>%s</html></b>", resources.getString("generalPanel.title")),
            generalTab.createGeneralTab());

        setPreferences();
    }

    private void setOptions() {
        // TODO this is where we update the dialog based on current campaign settings.
    }

    private void updateOptions() {
        // TODO this is where we update campaign values based on the dialog values
    }

    /**
     * Returns a new {@link JCheckBox} object with the specified name, label, and tooltip.
     * <p>
     * Please note that 'name' is also used to fetch the resources associated with this object.
     * For the label text 'name' is appended by '.text'.
     * For the tooltip 'name' is appended with '.tooltip'.
     * These values must exist in the resource bundle otherwise an error will be thrown.
     *
     * @param name    the name of the checkbox
     * @param customWrapSize    the maximum number of characters (including whitespaces) on each
     *                         line of the tooltip (or 100, if {@code null}).
     * @return a new {@link JCheckBox} object with the specified name, label, and tooltip
     */
    public static JCheckBox createCheckBox(String name, @Nullable Integer customWrapSize) {
        customWrapSize = processWrapSize(customWrapSize);

        JCheckBox checkBox = new JCheckBox(String.format("<html><b>%s</html></b>",
            resources.getString(name + ".text")));
        checkBox.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        checkBox.setName(name);

        return checkBox;
    }

    /**
     * Creates a map containing a {@link JLabel} and a {@link JSpinner} object.
     * <p>
     * Please note that 'name' is also used to fetch the resources associated with these objects.
     * For the label text 'name' is appended by '.text'.
     * For the tooltips 'name' is appended with '.tooltip'.
     * These values must exist in the resource bundle otherwise an error will be thrown.
     *
     * @param name             a string representing the name of the objects.
     *                        The {@link JLabel} will have 'lbl' appended.
     *                        The {@link JSpinner} is appended with 'spn'.
     * @param customWrapSize   the maximum number of characters (including whitespaces) on each
     *                        line of the tooltip (or 100, if {@code null}).
     * @param defaultValue     a double representing the default value of the spinner
     * @param minimum          a double representing the minimum value of the spinner
     * @param maximum          a double representing the maximum value of the spinner
     * @param stepSize         a double representing the step size of the spinner
     * @return a map containing a {@link JLabel} key and a {@link JSpinner} value.
     */
    public static Map<JLabel, JSpinner> createLabeledSpinner(String name, @Nullable Integer customWrapSize,
                                                      double defaultValue, double minimum,
                                                      double maximum, double stepSize) {
        customWrapSize = processWrapSize(customWrapSize);

        final JLabel jLabel = new JLabel(String.format("<html><b>%s</html></b>",
            resources.getString(name + ".text")));
        jLabel.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jLabel.setName("lbl" + name);

        JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, minimum, maximum, stepSize));
        jSpinner.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jSpinner.setName("spn" + name);

        return Map.of(jLabel, jSpinner);
    }

    /**
     * Creates a {@link JLabel} with the specified name and optional customWrapSize.
     * <p>
     * Please note that 'name' is also used to fetch the resources associated with this label.
     * For the label text 'name' is appended by '.text'.
     * For the label tooltip 'name' is appended with '.tooltip'.
     * These values must exist in the resource bundle otherwise an error will be thrown.
     *
     * @param name             the name of the label
     * @param customWrapSize   the maximum number of characters (including whitespaces) on each line
     *                        of the tooltip; defaults to 100 if {@code null}
     * @return a new {@link JLabel} object
     */
    public static JLabel createLabel(String name, @Nullable Integer customWrapSize) {
        customWrapSize = processWrapSize(customWrapSize);

        JLabel jLabel = new JLabel(String.format("<html><b>%s</html></b>",
            resources.getString(name + ".text")));
        jLabel.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jLabel.setName(name);

        return jLabel;
    }

    /**
     * Creates a map containing a {@link JLabel} and a {@link JTextField} object.
     * <p>
     * Please note that 'name' is also used to fetch the resources associated with these objects.
     * For the label text 'name' is appended by '.text'.
     * For the tooltips 'name' is appended with '.tooltip'.
     * These values must exist in the resource bundle otherwise an error will be thrown.
     *
     * @param name                 the name of the objects.
     *                            The {@link JLabel} will have 'lbl' appended.
     *                            The {@link JTextField} is appended with 'txt'.
     * @param customWrapSize       the maximum number of characters (including whitespaces) on each
     *                            line of the tooltip.
     *                            If {@code null}, the default wrap size of 100 is used.
     * @param minimumSizeWidth     the minimum width of the {@link JTextField}.
     * @param minimumSizeHeight    the minimum height of the {@link JTextField}.
     * @param maximumSizeWidth     the maximum width of the {@link JTextField}.
     *                            If {@code null}, the minimum size width is used.
     * @param maximumSizeHeight    the maximum height of the {@link JTextField}.
     *                            If {@code null}, the minimum size height is used.
     * @return a map containing a {@link JLabel} key and a {@link JTextField} value.
     */
    public static Map<JLabel, JTextField> createLabeledTextField(String name,
                                                @Nullable Integer customWrapSize, int minimumSizeWidth,
                                                int minimumSizeHeight, @Nullable Integer maximumSizeWidth,
                                                @Nullable Integer maximumSizeHeight) {
        customWrapSize = processWrapSize(customWrapSize);

        JLabel jLabel = new JLabel(String.format("<html><b>%s</html></b>",
            resources.getString(name + ".text")));
        jLabel.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jLabel.setName("lbl" + name);

        JTextField jTextField = new JTextField();
        jTextField.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jTextField.setName("txt" + name);

        jTextField.setMinimumSize(new Dimension(minimumSizeWidth, minimumSizeHeight));

        maximumSizeWidth = maximumSizeWidth == null ? minimumSizeWidth : maximumSizeWidth;
        maximumSizeHeight = maximumSizeHeight == null ? minimumSizeHeight : maximumSizeHeight;
        jTextField.setPreferredSize(new Dimension(maximumSizeWidth, maximumSizeHeight));

        return Map.of(jLabel, jTextField);
    }

    /**
     * Returns the maximum number of characters on each line of a tooltip.
     * If a custom wrap size is provided, it is returned.
     * Otherwise, the default wrap size of 100 is returned.
     *
     * @param customWrapSize the maximum number of characters (including whitespaces) on each line
     *                      of the tooltip, or {@code null} if no custom wrap size is specified
     * @return the maximum number of characters on each line of a tooltip
     */
    private static int processWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? 100 : customWrapSize;
    }

    /**
     * Creates a standard {@link JPanel} with a titled border.
     * <p>
     * {@code createStandardLayout} should also be called and the resulting {@link GroupLayout}
     * assigned to the panel via {@code setLayout}.
     *
     * @param name         the name of the panel.
     * @param includeBorder whether the panel should have a border.
     * @param borderTitle  The resource string that should be used as the title of the border.
     *                    Can be empty to generate an untitled border.
     *
     * @return a JPanel with a titled border and GroupLayout as its layout manager
     */
    public static JPanel createStandardPanel(String name, boolean includeBorder, String borderTitle) {
        borderTitle = borderTitle.isBlank() ? "" : resources.getString(borderTitle);

        JPanel panel = new JPanel();

        if (includeBorder) {
            panel.setBorder(BorderFactory.createTitledBorder(
                String.format(String.format("<html><b>%s</html></b>", borderTitle))));
        }

        panel.setName(name);

        return panel;
    }

    /**
     * Creates a {@link GroupLayout} object for the specified {@link JPanel}.
     * <p>
     * Written to be paired with {@code createStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GroupLayout} is created
     * @return the created {@link GroupLayout} object
     */
    public static GroupLayout createStandardLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }
}
