package mekhq.gui.dialog.campaignOptions;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.List;
import java.util.*;

import static java.lang.Math.round;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;

public class CampaignOptionsUtilities {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);
    final static String IMAGE_DIRECTORY = "data/images/universe/factions/";

    /**
     * @return the image directory
     */
    static String getImageDirectory() {
        return IMAGE_DIRECTORY;
    }

    /**
     * This class provides a custom {@link JCheckBox} for campaign options.
     * The checkbox name and tooltips are fetched from a resource bundle based on the provided name.
     */
    static class CampaignOptionsCheckBox extends JCheckBox {
        /**
         * Returns a new {@link JCheckBox} object.
         * <p>
         * The {@link JCheckBox} will be named {@code "chk" + name}, and use the following resource
         * bundle references: {@code "lbl" + name + ".text"} and {@code "lbl" + name + ".tooltip"}.
         *
         * @param name    the name of the checkbox
         */
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

    /**
     * This class provides a custom {@link JSpinner} for campaign options.
     * The spinner name and tooltips are fetched from a resource bundle based on the provided name.
     */
    static class CampaignOptionsSpinner extends JSpinner {

        /**
         * Creates a {@link JSpinner} object.
         * <p>
         * The name of the {@link JSpinner} will be {@code "spn" + name},
         * and it will use the {@code "lbl" + name + ".tooltip"} resource bundle item.
         *
         * @param name           a string representing the name of the object.
         * @param customWrapSize the maximum number of characters (including spaces) on each
         *                       line of the tooltip (or {@code 100}, if {@code null}).
         * @param defaultValue   The default value of the spinner (integer or double).
         * @param minimum        The minimum value of the spinner (integer or double).
         * @param maximum        The maximum value of the spinner (integer or double).
         * @param stepSize       The step size of the spinner (integer or double).
         * @param noTooltip      {@code true} if the component should be created without a tooltip.
         */
        public CampaignOptionsSpinner(String name, @Nullable Integer customWrapSize,
                                      Number defaultValue, Number minimum,
                                      Number maximum, Number stepSize, boolean noTooltip) {
            super(createSpinnerModel(defaultValue, minimum, maximum, stepSize));

            if (!noTooltip) {
                setToolTipText(wordWrap(getTooltipText(name), processWrapSize(customWrapSize)));
            }

            configureSpinner(name);
        }

        /**
         * Creates a {@link JSpinner} object with integer values.
         * <p>
         * This constructor assumes a default {@code null} for wrap size and tooltip enabled.
         *
         * @param name         a string representing the name of the object.
         * @param defaultValue The default value of the spinner (integer).
         * @param minimum      The minimum value of the spinner (integer).
         * @param maximum      The maximum value of the spinner (integer).
         * @param stepSize     The step size of the spinner (integer).
         */
        public CampaignOptionsSpinner(String name, int defaultValue, int minimum,
                                      int maximum, int stepSize) {
            this(name, null, defaultValue, minimum, maximum, stepSize, false);
        }

        /**
         * Creates a {@link JSpinner} object with double values.
         * <p>
         * This constructor assumes a default {@code null} for wrap size and tooltip enabled.
         *
         * @param name         a string representing the name of the object.
         * @param defaultValue The default value of the spinner (double).
         * @param minimum      The minimum value of the spinner (double).
         * @param maximum      The maximum value of the spinner (double).
         * @param stepSize     The step size of the spinner (double).
         */
        public CampaignOptionsSpinner(String name, double defaultValue, double minimum,
                                      double maximum, double stepSize) {
            this(name, null, defaultValue, minimum, maximum, stepSize, false);
        }

        /**
         * A helper method to create the appropriate {@link SpinnerNumberModel} based on numeric types (integer or double).
         *
         * @param defaultValue The default value (integer or double).
         * @param minimum      The minimum value (integer or double).
         * @param maximum      The maximum value (integer or double).
         * @param stepSize     The step size (integer or double).
         * @return A configured {@link SpinnerNumberModel}.
         */
        private static SpinnerNumberModel createSpinnerModel(Number defaultValue, Number minimum,
                                                             Number maximum, Number stepSize) {
            if (defaultValue instanceof Double || minimum instanceof Double ||
                maximum instanceof Double || stepSize instanceof Double) {
                // If any value is a double, use a double-based SpinnerNumberModel
                return new SpinnerNumberModel(
                    defaultValue.doubleValue(), minimum.doubleValue(), maximum.doubleValue(), stepSize.doubleValue()
                );
            } else {
                // Otherwise, use an integer-based SpinnerNumberModel
                return new SpinnerNumberModel(
                    defaultValue.intValue(), minimum.intValue(), maximum.intValue(), stepSize.intValue()
                );
            }
        }

        /**
         * A helper method to configure repeated spinner settings (name, tooltip, etc.).
         *
         * @param name The base name of the spinner.
         */
        private void configureSpinner(String name) {
            setName("spn" + name);
            setFontScaling(this, false, 1);

            DefaultEditor editor = (DefaultEditor) this.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
        }

        /**
         * A helper method to get the tooltip text based on the spinner's name. Falls back to an empty string
         * if the tooltip resource is missing.
         *
         * @param name Name of the spinner.
         * @return Tooltip text.
         */
        private String getTooltipText(String name) {
            try {
                return resources.getString("lbl" + name + ".tooltip");
            } catch (MissingResourceException e) {
                return ""; // Default to no tooltip if the resource is missing
            }
        }
    }

    /**
     * This class provides a custom {@link JLabel} for campaign options.
     * The label name and tooltips are fetched from a resource bundle based on the provided name.
     */
    static class CampaignOptionsLabel extends JLabel {
        /**
         * Creates a {@link JLabel} with the specified name.
         * <p>
         * Please note that 'name' is also used to fetch the resources associated with this label.
         * For the label text 'name' is appended by '.text'.
         * For the label tooltip 'name' is appended with '.tooltip'.
         * These values must exist in the resource bundle otherwise an error will be thrown.
         *
         * @param name             the name of the label
         */
        public CampaignOptionsLabel(String name) {
            this(name, null, false);
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
         * @param noTooltip        if {@code true} the component will be created without a tooltip.
         */
        public CampaignOptionsLabel(String name, @Nullable Integer customWrapSize, boolean noTooltip) {
            super(String.format("<html>%s</html>",
                resources.getString("lbl" + name + ".text")));

            if (!noTooltip) {
                setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
                    processWrapSize(customWrapSize)));
            }

            setName("lbl" + name);

            setFontScaling(this, false, 1);
        }
    }

    /**
     * This class provides a custom {@link JTextField} for campaign options.
     * The text field name and tooltips are fetched from a resource bundle based on the provided name.
     */
    static class CampaignOptionsTextField extends JTextField {
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

    /**
     * This class provides a custom {@link JButton} for campaign options.
     * The button name and tooltips are fetched from a resource bundle based on the provided name.
     */
    static class CampaignOptionsButton extends JButton {
        /**
         * Creates a new {@link JButton}.
         * <p>
         * The name of the created {@link JButton} is {@code "btn" + name}
         * The resource bundle references for the created {@link JButton} are {@code "lbl" + name + ".text"}
         * and {@code "lbl" + name + ".tooltip"}.
         *
         * @param name the name of the button, used to generate the button's name and resource bundle references
         */
        public CampaignOptionsButton(String name) {
            this(name, null);
        }

        /**
         * Creates a new {@link JButton} with a custom tooltip wrap size.
         * <p>
         * The name of the created {@link JButton} is {@code "btn" + name}
         * The resource bundle references for the created {@link JButton} are {@code "lbl" + name + ".text"}
         * and {@code "lbl" + name + ".tooltip"}.
         *
         * @param name the name of the button, used for text and tooltip generation
         * @param customWrapSize the maximum number of characters for line wrapping in the tooltip,
         *                       or {@code null} if the default wrap size is to be used
         */
        public CampaignOptionsButton(String name, @Nullable Integer customWrapSize) {
            super(resources.getString("lbl" + name + ".text"));
            setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
                processWrapSize(customWrapSize)));
            setName("btn" + name);

            setFontScaling(this, false, 1);
        }
    }

    /**
     * This class provides a custom {@link JPanel} for campaign options.
     * Offers an optional untitled border, and the panel name is set to "pnl" + name.
     */
    static class CampaignOptionsStandardPanel extends JPanel {
        /**
         * Creates a standardized {@link JPanel} without a border.
         * <p>
         * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
         * assigned to the panel via {@code setLayout}.
         *
         * @param name         the name of the panel.
         */
        public CampaignOptionsStandardPanel(String name) {
            this(name, false, "");
        }

        /**
         * Creates a standardized {@link JPanel} with an untitled border.
         * <p>
         * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
         * assigned to the panel via {@code setLayout}.
         *
         * @param name         the name of the panel.
         * @param includeBorder whether the panel should have a border.
         */
        public CampaignOptionsStandardPanel(String name, boolean includeBorder) {
            this(name, includeBorder, "");
        }

        /**
         * Creates a standardized {@link JPanel} with a titled border.
         * <p>
         * {@code createGroupLayout} should also be called and the resulting {@link GroupLayout}
         * assigned to the panel via {@code setLayout}.
         * <p>
         * If {@code borderTitle} isn't empty the resource bundle reference, used to fetch the border's
         * title, will be {@code "lbl" + borderTitle + ".text"}
         *
         * @param name         the name of the panel.
         * @param includeBorder whether the panel should have a border.
         */
        public CampaignOptionsStandardPanel(String name, boolean includeBorder, String borderTitle) {
            borderTitle = borderTitle.isBlank() ? "" : resources.getString("lbl" + borderTitle + ".text");

            new JPanel() {
                @Override
                public Dimension getPreferredSize() {
                    Dimension standardSize = super.getPreferredSize();
                    return UIUtil.scaleForGUI((Math.max(standardSize.width, 500)), standardSize.height);
                }
            };

            if (includeBorder) {
                if (borderTitle.isBlank()) {
                    setBorder(BorderFactory.createEtchedBorder());
                } else {
                    setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        String.format("<html>%s</html>", borderTitle)));
                }
            }

            setName("pnl" + name);
        }
    }

    /**
     * This class provides a custom {@link JPanel} for campaign options, consisting of a label and an image.
     * The panel, label and optional body label names are set based on the provided name parameter.
     * The text for the label(s) is fetched from a resource bundle.
     */
    static class CampaignOptionsHeaderPanel extends JPanel {
        /**
         * Creates a {@link JPanel} consisting of a {@link JLabel} above an image.
         * <p>
         * The {@link JPanel} will be named {@code "pnl" + name + "HeaderPanel"}.
         * The resource bundle references for the {@link JLabel} will be {@code "lbl" + name + ".text"}.
         *
         * @param name           the name of the header panel.
         * @param imageAddress   the file path of the image to be displayed in the panel
         */
        public CampaignOptionsHeaderPanel(String name, String imageAddress) {
            this(name, imageAddress, false);
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
         * @param includeBodyText    if {@code true}, include a second {@link JLabel}.
         */
        public CampaignOptionsHeaderPanel(String name, String imageAddress, boolean includeBodyText) {
            // Fetch and scale image
            ImageIcon imageIcon = new ImageIcon(imageAddress);

            int width = (int) UIUtil.scaleForGUI(round(imageIcon.getIconWidth() * .75));
            int height = (int) UIUtil.scaleForGUI(round(imageIcon.getIconHeight() * .75));

            Image image = imageIcon.getImage();
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

            imageIcon = new ImageIcon(scaledImage);

            JLabel lblImage = new JLabel(imageIcon);

            // Create header text
            final JLabel lblHeader = new JLabel(resources.getString("lbl" + name + ".text"), SwingConstants.CENTER);
            lblHeader.setName("lbl" + name);
            setFontScaling(lblHeader, true, 2);

            JLabel lblBody = new JLabel();
            if (includeBodyText) {
                lblBody = new JLabel(String.format("<html><div style='width: %s; text-align:justify;'>%s</div></html>",
                    UIUtil.scaleForGUI(750),
                    resources.getString("lbl" + name + "Body.text")), SwingConstants.CENTER);
                lblBody.setName("lbl" + name + "Body");
                setFontScaling(lblBody, false, 1);
            }

            // Layout panel
            new CampaignOptionsStandardPanel("pnl" + name + "HeaderPanel");
            final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(this);

            layout.gridwidth = 5;
            layout.gridx = 0;
            layout.gridy = 0;
            this.add(lblHeader, layout);

            layout.gridy++;
            layout.gridwidth = 1;
            this.add(lblImage, layout);

            layout.gridy++;
            layout.gridwidth = 1;
            this.add(lblBody, layout);
        }
    }

    /**
     * Creates a {@link GroupLayout} object for the specified {@link JPanel}.
     * <p>
     * Written to be paired with {@code CampaignOptionsStandardPanel}.
     *
     * @param panel the {@link JPanel} for which the {@link GroupLayout} is created
     * @return the created {@link GroupLayout} object
     */
    static GroupLayout createGroupLayout(JPanel panel) {
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        return layout;
    }

    static class CampaignOptionsGridBagConstraints extends GridBagConstraints {
        /**
         * Creates a {@link GridBagConstraints} object for the specified {@link JPanel}.
         * <p>
         * Written to be paired with {@code CampaignOptionsStandardPanel}.
         *
         * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
         */
        public CampaignOptionsGridBagConstraints(JPanel panel) {
            this(panel, null, null);
        }

        /**
         * Creates a {@link GridBagConstraints} object for the specified {@link JPanel} according to the
         * provided settings.
         * <p>
         * Written to be paired with {@code CampaignOptionsStandardPanel}.
         *
         * @param panel the {@link JPanel} for which the {@link GridBagConstraints} is created
         * @param anchor the anchor setting for the {@link GridBagConstraints}, or {@code null} to use
         *              the default value {@link GridBagConstraints#NORTHWEST}
         * @param fill the fill setting for the {@link GridBagConstraints}, or {@code null} to use the
         *            default value {@link GridBagConstraints#NORTHWEST}
         */
        public CampaignOptionsGridBagConstraints(JPanel panel, @Nullable Integer anchor, @Nullable Integer fill) {
            super();
            panel.setLayout(new GridBagLayout());

            this.anchor = Objects.requireNonNullElse(anchor, GridBagConstraints.NORTHWEST);
            this.fill = Objects.requireNonNullElse(fill, GridBagConstraints.BOTH);

            this.insets = new Insets(5, 5, 5, 5);
        }
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
        final JPanel parentPanel = new CampaignOptionsStandardPanel(name);
        final GroupLayout parentLayout = createGroupLayout(parentPanel);

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
                UIUtil.scaleForGUI(mainPanel.getPreferredSize().width),
                resources.getString(tabName + ".border")));

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
     * Returns the maximum number of characters on each line of a tooltip.
     * If a custom wrap size is provided, it is returned.
     * Otherwise, the default wrap size of 100 is returned.
     *
     * @param customWrapSize the maximum number of characters (including whitespaces) on each line
     *                      of the tooltip, or {@code null} if no custom wrap size is specified
     * @return the maximum number of characters on each line of a tooltip
     */
    static int processWrapSize(@Nullable Integer customWrapSize) {
        return customWrapSize == null ? 100 : customWrapSize;
    }
}
