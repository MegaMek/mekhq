package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.swing.util.FlatLafStyleBuilder;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.DefaultEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

public class CampaignOptionsUtilities {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";

    /**
     * @return the image directory
     */
    static String getImageDirectory() {
        return IMAGE_DIRECTORY;
    }

    static JCheckBox createCheckBox(String name) {
        return createCheckBox(name, null);
    }

    /**
     * Returns a new {@link JCheckBox} object.
     * <p>
     * The {@link JCheckBox} will be named {@code "chk" + name}, and use the following resource bundle references:
     * {@code "lbl" + name + ".text"} and {@code "lbl" + name + ".tooltip"}.
     *
     * @param name    the name of the checkbox
     * @param customWrapSize    the maximum number of characters (including whitespaces) on each
     *                         line of the tooltip (or 100, if {@code null}).
     * @return a new {@link JCheckBox} object with the specified name, label, and tooltip
     */
    static JCheckBox createCheckBox(String name, @Nullable Integer customWrapSize) {
        customWrapSize = processWrapSize(customWrapSize);

        JCheckBox checkBox = new JCheckBox(String.format("<html>%s</html>",
            resources.getString("lbl" + name + ".text")));
        checkBox.setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"), customWrapSize));
        checkBox.setName("chk" + name);

        Dimension size = checkBox.getPreferredSize();
        checkBox.setMaximumSize(size);
        checkBox.setMinimumSize(size);

        return checkBox;
    }

    /**
     * Creates a {@link JSpinner} object.
     * <p>
     * The name of the {@link JSpinner} will be {@code "spn" + name},
     * and it will use the {@code "lbl" + name + ".tooltip"} resource bundle item
     *
     * @param name             a string representing the name of the objects.
     * @param defaultValue     The default value of the spinner
     * @param minimum          The minimum value of the spinner
     * @param maximum          The maximum value of the spinner
     * @param stepSize         The step size of the spinner
     * @return The created {@link JSpinner}.
     */
    static JSpinner createSpinner(String name, double defaultValue, double minimum, double maximum,
                                  double stepSize) {
        return createSpinner(name, null, defaultValue, minimum, maximum, stepSize);
    }

    /**
     * Creates a {@link JSpinner} object.
     * <p>
     * The name of the {@link JSpinner} will be {@code "spn" + name},
     * and it will use the {@code "lbl" + name + ".tooltip"} resource bundle item
     *
     * @param name             a string representing the name of the objects.
     * @param customWrapSize   the maximum number of characters (including whitespaces) on each
     *                        line of the tooltip (or 100, if {@code null}).
     * @param defaultValue     The default value of the spinner
     * @param minimum          The minimum value of the spinner
     * @param maximum          The maximum value of the spinner
     * @param stepSize         The step size of the spinner
     * @return The created {@link JSpinner}.
     */
    static JSpinner createSpinner(String name, @Nullable Integer customWrapSize, double defaultValue,
                                  double minimum, double maximum, double stepSize) {
        customWrapSize = processWrapSize(customWrapSize);

        JSpinner jSpinner = new JSpinner();
        jSpinner.setModel(new SpinnerNumberModel(defaultValue, minimum, maximum, stepSize));
        jSpinner.setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"), customWrapSize));
        jSpinner.setName("spn" + name);

        DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

        Dimension size = jSpinner.getPreferredSize();
        jSpinner.setMaximumSize(size);
        jSpinner.setMinimumSize(size);

        return jSpinner;
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
     * @return a new {@link JLabel} object
     */
    static JLabel createLabel(String name) {
        return createLabel(name, null);
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
    static JLabel createLabel(String name, @Nullable Integer customWrapSize) {
        customWrapSize = processWrapSize(customWrapSize);

        JLabel jLabel = new JLabel(String.format("<html>%s</html>",
            resources.getString("lbl" + name + ".text")));
        jLabel.setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"), customWrapSize));
        jLabel.setName("lbl" + name);

        Dimension size = jLabel.getPreferredSize();
        jLabel.setMinimumSize(size);
        jLabel.setMaximumSize(size);

        return jLabel;
    }

    /**
     * Creates a {@link JTextField} object.
     * <p>
     * The name of the {@link JTextField} will be {@code}, and it will use the following resource bundle reference:
     * {@code "lbl" + name + ".tooltip"}.
     *
     * @param name                the name of the object.
     * @param width               The width of the {@link JTextField}.
     * @return a map containing a {@link JLabel} key and a {@link JTextField} value.
     */
    static JTextField createTextField(String name, int width) {
        return createTextField(name, null, width);
    }

    /**
     * Creates a {@link JTextField} object.
     * <p>
     * The name of the {@link JTextField} will be {@code}, and it will use the following resource bundle reference:
     * {@code "lbl" + name + ".tooltip"}.
     *
     * @param name                the name of the object.
     * @param customWrapSize      the maximum number of characters (including whitespaces) on each
     *                            line of the tooltip.
     *                            If {@code null}, the default wrap size of 100 is used.
     * @param width               The width of the {@link JTextField}.
     * @return a map containing a {@link JLabel} key and a {@link JTextField} value.
     */
    static JTextField createTextField(String name, @Nullable Integer customWrapSize, int width) {
        customWrapSize = processWrapSize(customWrapSize);

        JTextField jTextField = new JTextField();
        jTextField.setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"), customWrapSize));
        jTextField.setName("txt" + name);

        int preferredHeight = jTextField.getPreferredSize().height;
        jTextField.setMinimumSize(new Dimension(UIUtil.scaleForGUI(width), preferredHeight));
        jTextField.setMaximumSize(new Dimension(UIUtil.scaleForGUI(width), preferredHeight));

        return jTextField;
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
     * <p>
     * If {@code borderTitle} isn't empty the resource bundle reference, used to fetch the border's
     * title, will be {@code "lbl" + borderTitle + ".text"}
     *
     * @param name         the name of the panel.
     * @param includeBorder whether the panel should have a border.
     * @param borderTitle  The resource string that should be used as the title of the border.
     *                    Can be empty to generate an untitled border.
     *
     * @return a {@link JPanel} with a titled border and {@link GroupLayout} as its layout manager
     */
    static JPanel createStandardPanel(String name, boolean includeBorder, String borderTitle) {
        borderTitle = borderTitle.isBlank() ? "" : resources.getString("lbl" + borderTitle + ".text");

        JPanel panel = new JPanel();

        if (includeBorder) {
            panel.setBorder(BorderFactory.createTitledBorder(
                String.format(String.format("<html>%s</html>", borderTitle))));
        }

        panel.setName(name);

        Dimension size = panel.getPreferredSize();
        int sizeHeight = size.height;
        int sizeWidth = size.width;
        for (Component component : panel.getComponents()) {
            sizeHeight += component.getPreferredSize().height;
            sizeWidth += component.getPreferredSize().width;
        }

        if (sizeWidth < UIUtil.scaleForGUI(750)) {
            sizeWidth = UIUtil.scaleForGUI(750);
        }

        panel.setMaximumSize(new Dimension(sizeWidth, sizeHeight));

        return panel;
    }

    /**
     * Creates a {@link JPanel} consisting of a {@link JLabel} above an image.
     * If {@code includeBodyText} is {@code true} a second {@link JLabel} is placed after the first.
     * <p>
     * The {@link JPanel} will be named {@code "pnl" + name + "HeaderPanel"}.
     * The resource bundle references for the first {@link JLabel} will be {@code "lbl" + name + ".text"}.
     * The optional second {@link JLabel} is assigned the name {@code ""lbl" + name + "Body"}
     * and uses the following resource bundle reference: {@code "lbl" + name + "Body.text"}.
     *
     * @param name           the name of the header panel.
     * @param imageAddress   the file path of the image to be displayed in the panel
     * @param includeBorder  whether the panel should have a border
     * @param borderTitle    the title of the border; can be empty for an untitled border
     * @param includeBodyText    if {@code true}, include a second {@link JLabel}.
     * @return a {@link JPanel} representing the header panel
     */
    static JPanel createHeaderPanel(String name, String imageAddress, boolean includeBorder,
                                           String borderTitle, boolean includeBodyText) {
        ImageIcon imageIcon = new ImageIcon(imageAddress);
        JLabel imageLabel = new JLabel(imageIcon);

        final JLabel lblHeader = new JLabel(resources.getString("lbl" + name + ".text"), SwingConstants.CENTER);
        new FlatLafStyleBuilder().font(MHQConstants.PROJECT_NAME).bold().size(3).apply(lblHeader);
        lblHeader.setName("lbl" + name);

        JLabel lblBody = new JLabel();
        if (includeBodyText) {
            lblBody = new JLabel(String.format("<html><p align='justify'>%s</p></html>",
                resources.getString("lbl" + name + "Body.text")), SwingConstants.CENTER);
            lblBody.setName("lbl" + name + "Body");
            Dimension size = lblBody.getPreferredSize();
            lblBody.setMaximumSize(new Dimension(UIUtil.scaleForGUI(750), size.height));
        }

        final JPanel panel = createStandardPanel("pnl" + name + "HeaderPanel", includeBorder, borderTitle);
        Dimension size = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(UIUtil.scaleForGUI(750), size.height));

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
    static GroupLayout createStandardLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }

    /**
     * Creates a parent panel for the provided {@link JPanel}.
     *
     * @param panel the panel to be added to the parent panel
     * @param name the name of the parent panel
     * @return the created {@link JPanel}
     */
    static JPanel createParentPanel(JPanel panel, String name) {
        // Create Panel
        final JPanel parentPanel = createStandardPanel(name, false, "");
        final GroupLayout parentLayout = createStandardLayout(parentPanel);

        // Set Dimensions
        int widthNew = parentPanel.getMinimumSize().width;

        if (widthNew < UIUtil.scaleForGUI(750)) {
            widthNew = UIUtil.scaleForGUI(750);
        }

        int height = parentPanel.getPreferredSize().height;
        for (Component component : panel.getComponents()) {
            if (component instanceof JPanel) {
                height += component.getPreferredSize().height;
            }
        }

        Dimension size = new Dimension(widthNew, height);
        parentPanel.setMinimumSize(size);
        parentPanel.setMaximumSize(size);

        // Layout
        parentPanel.setLayout(parentLayout);

        parentLayout.setVerticalGroup(
            parentLayout.createSequentialGroup()
                .addComponent(panel));

        parentLayout.setHorizontalGroup(
            parentLayout.createParallelGroup(Alignment.CENTER)
                .addComponent(panel));

        return parentPanel;
    }

    /**
     * Creates a new instance of {@link JTabbedPane} with the supplied panels as tabs.
     * <p>
     * The resource bundle reference for the individual tabs will be {@code panel.getName() + ".title"}
     *
     * @param panels a map containing the names of the panels as keys and the corresponding
     *              {@link JPanel} objects as values
     * @return a {@link JTabbedPane} with the supplied panels as tabs
     */
    static JTabbedPane createSubTabs(Map<String, JPanel> panels) {
        // We use a list here to ensure that the tabs always display in the same order,
        // and that order might as well be alphabetic.
        List<String> tabNames = new ArrayList<>(panels.keySet());
        tabNames.sort(String.CASE_INSENSITIVE_ORDER);

        // This is a special case handler to ensure 'general options' tabs always appear first
        int indexToMoveToFront = -1;
        for (int i=0; i<tabNames.size(); i++) {
            if (tabNames.get(i).contains("GeneralTab")) {
                indexToMoveToFront = i;
                break;
            }
        }

        if (indexToMoveToFront != -1) {
            String tabName = tabNames.remove(indexToMoveToFront);
            tabNames.add(0, tabName);
        }

        JTabbedPane tabbedPane = new JTabbedPane();

        for (String tabName : tabNames) {
            JPanel mainPanel = panels.get(tabName);

            // Create a panel for the quote
            JPanel quotePanel = new JPanel(new GridBagLayout());
            JLabel quote = new JLabel(String.format(
                "<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
                UIUtil.scaleForGUI(650), resources.getString(tabName + ".border")));

            GridBagConstraints quoteConstraints = new GridBagConstraints();
            quoteConstraints.gridx = GridBagConstraints.RELATIVE;
            quoteConstraints.gridy = GridBagConstraints.RELATIVE;
            quotePanel.add(quote, quoteConstraints);

            // Create a BorderLayout panel for mainPanel
            JPanel mainPanelHolder = new JPanel(new GridBagLayout());
            GridBagConstraints mainConstraints = new GridBagConstraints();
            mainConstraints.gridx = GridBagConstraints.RELATIVE;
            mainConstraints.gridy = GridBagConstraints.RELATIVE;
            mainPanelHolder.add(mainPanel, mainConstraints);

            // Reorganize mainPanel to include quotePanel at bottom
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setName(tabName);
            contentPanel.add(mainPanelHolder, BorderLayout.CENTER);

            contentPanel.add(quotePanel, BorderLayout.SOUTH);

            // Create a wrapper panel for its easy alignment controls
            JPanel wrapperPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;

            wrapperPanel.add(contentPanel, gbc);

            tabbedPane.addTab(resources.getString(tabName + ".title"), wrapperPanel);
        }

        return tabbedPane;
    }

    /**
     * Creates a new {@link JButton}.
     * <p>
     * The name of the created {@link JButton} is {@code "btn" + name}
     * The resource bundle references for the created {@link JButton} are {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"}.
     *
     * @param name the name of the button, used to generate the button's name and resource bundle references
     * @return a new {@link JButton} object
     */
    static JButton createButton(String name) {
        JButton jButton = new JButton(resources.getString("lbl" + name + ".text"));
        jButton.setToolTipText(resources.getString("lbl" + name + ".tooltip"));
        jButton.setName("btn" + name);

        Dimension size = jButton.getPreferredSize();
        jButton.setMinimumSize(size);
        jButton.setMaximumSize(size);

        return jButton;
    }
}
