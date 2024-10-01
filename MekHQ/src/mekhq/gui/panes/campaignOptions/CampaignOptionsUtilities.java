package mekhq.gui.panes.campaignOptions;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

public class CampaignOptionsUtilities {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsPane.class);
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    final static int WIDTH_MULTIPLIER = 3; // This seems to be the sweet spot

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

        JCheckBox checkBox = new JCheckBox(String.format("<html><b>%s</b></html>",
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

        final JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>",
            resources.getString(name + ".text")));
        jLabel.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jLabel.setName("lbl" + name);

        JSpinner jSpinner = new JSpinner(new SpinnerNumberModel(defaultValue, minimum, maximum, stepSize));
        jSpinner.setToolTipText(wordWrap(resources.getString(name + ".tooltip"), customWrapSize));
        jSpinner.setName("spn" + name);

        FontMetrics fontMetrics = jSpinner.getFontMetrics(jSpinner.getFont());
        int width = fontMetrics.stringWidth(Double.toString(maximum));
        width = width * WIDTH_MULTIPLIER;

        jSpinner.setMaximumSize(new Dimension(width, 30));
        jSpinner.setMinimumSize(new Dimension(width, 30));

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

        JLabel jLabel = new JLabel(String.format("<html>%s</html>",
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

        JLabel jLabel = new JLabel(String.format("<html><b>%s</b></html>",
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
                String.format(String.format("<html><b>%s</b></html>", borderTitle))));
        }

        panel.setName(name);

        return panel;
    }

    /**
     * Creates a {@link JPanel} consisting of a {@link JLabel} above an image.
     * If {@code includeBodyText} is {@code true} a second {@link JLabel} is placed after the first.
     *
     * @param name           the name of the header panel.
     *                      The {@link JLabel} will have 'lbl' appended. This will be appended with
     *                      '.text' to fetch the label contents from the resource bundle.
     *                      The {@link JPanel} is appended with 'txt'.
     * @param imageAddress   the file path of the image to be displayed in the panel
     * @param includeBorder  whether the panel should have a border
     * @param borderTitle    the title of the border; can be empty for an untitled border
     * @param includeBodyText    if {@code true}, include a second {@link JLabel} named {@code name + "Body"}.
     *                          The resource bundle reference is {@code name + "Body.text"}
     * @return a JPanel representing the header panel
     */
    public static JPanel createHeaderPanel(String name, String imageAddress, boolean includeBorder,
                                           String borderTitle, boolean includeBodyText) {
        ImageIcon imageIcon = new ImageIcon(imageAddress);
        JLabel imageLabel = new JLabel(imageIcon);

        final JLabel lblHeader = new JLabel(String.format("<html>%s</html>",
            resources.getString("lbl" + name + ".text")), SwingConstants.CENTER);
        lblHeader.setName("lbl" + name);

        JLabel lblBody = new JLabel();
        if (includeBodyText) {
            lblBody = new JLabel(String.format("<html>%s</html>",
                resources.getString("lbl" + name + "Body.text")), SwingConstants.CENTER);
            lblHeader.setName("lbl" + name);
        }

        final JPanel panel = createStandardPanel("pnl" + name, includeBorder, borderTitle);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(lblHeader)
                .addComponent(lblBody)
                .addComponent(imageLabel));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.CENTER)
                .addComponent(lblHeader)
                .addComponent(lblBody)
                .addComponent(imageLabel));

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
    /**
     * Creates a parent panel for the provided {@link JPanel} with a specified name, maximum width,
     * and maximum height.
     *
     * @param panel the panel to be added to the parent panel
     * @param name the name of the parent panel
     * @param maximumWidth the maximum width of the parent panel
     * @param maximumHeight the maximum height of the parent panel
     * @return the created {@link JPanel}
     */
    public static JPanel createParentPanel(JPanel panel, String name, int maximumWidth, int maximumHeight) {
        final JPanel parentPanel = createStandardPanel(name, true, "");
        final GroupLayout parentLayout = createStandardLayout(parentPanel);
        Dimension preferredSize = new Dimension(maximumWidth, maximumHeight);
        panel.setMaximumSize(preferredSize);
        parentPanel.setLayout(parentLayout);

        parentLayout.setVerticalGroup(
            parentLayout.createSequentialGroup()
                .addComponent(panel));

        parentLayout.setHorizontalGroup(
            parentLayout.createParallelGroup(Alignment.LEADING)
                .addComponent(panel));

        return parentPanel;
    }

    /**
     * Creates a new instance of {@link JTabbedPane} with the supplied panels as tabs.
     *
     * @param panels a map containing the names of the panels as keys and the corresponding
     *              {@link JPanel} objects as values
     * @return a {@link JTabbedPane} with the supplied panels as tabs
     */
    public static JTabbedPane createSubTabs(Map<String, JPanel> panels) {
        // We use a list here to ensure that the tabs always display in the same order,
        // and that order might as well be alphabetic.
        List<String> tabNames = new ArrayList<>(panels.keySet());
        tabNames.sort(String.CASE_INSENSITIVE_ORDER);

        JTabbedPane tabbedPane = new JTabbedPane();

        for (String tabName : tabNames) {
            JPanel panel = panels.get(tabName);
            tabbedPane.addTab(resources.getString(panel.getName() + ".title"), panel);
        }

        return tabbedPane;
    }
}
