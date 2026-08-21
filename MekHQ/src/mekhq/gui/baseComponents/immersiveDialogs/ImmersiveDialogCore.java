/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.baseComponents.immersiveDialogs;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.icons.Portrait.DEFAULT_PORTRAIT_FILENAME;
import static megamek.common.icons.Portrait.NO_PORTRAIT_NAME;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static mekhq.gui.dialog.glossary.GlossaryDialog.DOCUMENTATION_COMMAND_STRING;
import static mekhq.gui.dialog.glossary.GlossaryDialog.GLOSSARY_COMMAND_STRING;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.plaf.basic.BasicHTML;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Portrait;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.utilities.glossary.DocumentationEntry;
import mekhq.campaign.utilities.glossary.GlossaryEntry;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.dialog.glossary.GlossaryDocumentationEntryDialog;
import mekhq.gui.dialog.glossary.GlossaryEntryDialog;
import mekhq.gui.utilities.WrapLayout;

/**
 * An immersive dialog used in MekHQ to display interactions between speakers, messages, and actions. The dialog
 * supports entities such as speakers, campaign, buttons, and optional details for enhanced storytelling.
 *
 * <p>It allows displaying one or more speakers in a dialog alongside a central message,
 * optional out-of-character notes, and UI buttons for user interaction.</p>
 *
 * <p>The dialog is flexible in terms of panel layout and width adjustments,
 * allowing for dynamic configurations based on the input parameters.</p>
 */
public class ImmersiveDialogCore extends JDialog {
    private static final int RESPONSE_BUTTON_HORIZONTAL_LAYOUT_ALLOWANCE = 4;
    private static final int RESPONSE_BUTTON_VERTICAL_LAYOUT_ALLOWANCE = 2;
    private static final String FLATLAF_CLASS_PREFIX = "com.formdev.flatlaf.";
    private static final String FLATLAF_WINDOW_DECORATIONS_PROPERTY = "flatlaf.useWindowDecorations";
    private static final String USE_WINDOW_DECORATIONS_PROPERTY = "JRootPane.useWindowDecorations";
    private static final String FULL_WINDOW_CONTENT_PROPERTY = "FlatLaf.fullWindowContent";
    private static final String TITLE_BAR_SHOW_TITLE_PROPERTY = "JRootPane.titleBarShowTitle";
    private static final String TITLE_BAR_SHOW_ICON_PROPERTY = "JRootPane.titleBarShowIcon";
    public final static String PERSON_COMMAND_STRING = "PERSON";
    public final static String MISSION_COMMAND_STRING = "MISSION";
    public final static String SCENARIO_COMMAND_STRING = "SCENARIO";

    private final Campaign campaign;

    private int CENTER_WIDTH = scaleForGUI(400);

    private final int PADDING = scaleForGUI(5);
    protected static final int IMAGE_WIDTH = scaleForGUI(200);

    private final JPanel southPanel;
    private final Person leftSpeaker;
    private final Person rightSpeaker;
    private final TransmissionSignalQuality signalQuality;
    private final ResponseActivationController responseActivationController;

    private JSpinner spinner;
    private int spinnerValue;
    private MMComboBox<?> comboBox; // can be null
    private int comboBoxChoiceIndex;
    private FastJScrollPane messageScrollPane;

    private int dialogChoice = 0;

    private static final MMLogger LOGGER = MMLogger.create(ImmersiveDialogCore.class);

    /**
     * Retrieves the user's selected dialog choice.
     * <p>
     * <strong>Usage:</strong> This allows us to keep function code out of the GUI element,
     * making it far easier to test what's happening for any given option selection. Create the dialog, as normal, then
     * fetch whatever decision the user made and perform any code actions required.
     * </p>
     *
     * @return An integer representing the index of the button selected by the user. If the dialog is closed without
     *       selection, this will return the {@code defaultChoiceIndex} defined during construction.
    *       Response activation stores this choice and any supplemental control values before the brief transmission
    *       confirmation; a modal constructor returns after that confirmation closes the dialog.
     */
    public int getDialogChoice() {
        return dialogChoice;
    }

    /**
     * Sets the dialog choice for the current object.
     *
     * @param dialogChoice The integer value representing the dialog choice to set.
     */
    public void setDialogChoice(int dialogChoice) {
        this.dialogChoice = dialogChoice;
    }

    /**
     * Retrieves the current value of the spinner.
     *
     * <p><b>Note:</b> will return 0 if the dialog does not contain a {@link JSpinner} in the supplemental panel.</p>
     *
     * @return The integer value of the spinner.
     */
    public int getSpinnerValue() {
        return spinnerValue;
    }

    /**
     * Sets a new value for the spinner.
     *
     * <p><b>Note:</b> will return 0 if the dialog does not contain a {@link MMComboBox} in the supplemental panel.</p>
     *
     * @param spinnerValue The integer value to set for the spinner.
     */
    public void setSpinnerValue(int spinnerValue) {
        this.spinnerValue = spinnerValue;
    }


    /**
     * Retrieves the current index of the combo box choice.
     *
     * @return The integer value representing the current selected index of the combo box.
     */
    public int getComboBoxChoiceIndex() {
        return comboBoxChoiceIndex;
    }

    /**
     * Sets a new index for the combo box choice.
     *
     * @param comboBoxChoiceIndex The integer value to set as the combo box's selected index.
     */
    public void setComboBoxChoiceIndex(int comboBoxChoiceIndex) {
        this.comboBoxChoiceIndex = comboBoxChoiceIndex;
    }

    /**
     * Retrieves the padding value defined in this object.
     *
     * @return The padding value as an integer.
     */
    protected int getPadding() {
        return PADDING;
    }

    /**
     * Constructs and initializes an immersive dialog with configurable layouts, speakers, actions, and messages.
     *
     * <p>This dialog is designed to provide a rich, immersive interface featuring optional speakers on the
     * left and right, a central message panel with configurable width, a spinner panel, and a list of actionable
     * buttons. An optional out-of-character message can also be displayed below the buttons.</p>
     *
     * @param campaign              The {@link Campaign} instance tied to the dialog, providing contextual information.
     * @param leftSpeaker           Optional left-side {@link Person}; use {@code null} if no left speaker is present.
     * @param rightSpeaker          Optional right-side {@link Person}; use {@code null} if no right speaker is
     *                              present.
     * @param centerMessage         The main {@link String} message displayed in the center panel of the dialog.
     * @param buttons               A {@link List} of {@link ButtonLabelTooltipPair} instances representing actions
     *                              available in the dialog (displayed as buttons). The default option is used if the
     *                              user closes or cancels the dialog.
     * @param outOfCharacterMessage An optional {@link String} message displayed below the buttons; use {@code null} if
     *                              not applicable.
     * @param centerWidth           An optional width for the center panel; uses the default value if {@code null}.
     * @param isVerticalLayout      A {@code boolean} determining the button layout: {@code true} for vertical stacking,
     *                              {@code false} for horizontal layout.
     * @param supplementalPanel     An optional {@link JPanel} containing a {@link JSpinner} and/or a {@link MMComboBox}
     *                              to be displayed in the center panel; use {@code null} if not applicable.
     */
    public ImmersiveDialogCore(Campaign campaign, @Nullable Person leftSpeaker, @Nullable Person rightSpeaker,
          String centerMessage, List<ButtonLabelTooltipPair> buttons, @Nullable String outOfCharacterMessage,
          @Nullable Integer centerWidth, boolean isVerticalLayout, @Nullable JPanel supplementalPanel,
          @Nullable ImageIcon imageIcon, boolean isModal) {
        this(campaign,
              leftSpeaker,
              rightSpeaker,
              centerMessage,
              buttons,
              outOfCharacterMessage,
              centerWidth,
              isVerticalLayout,
              supplementalPanel,
              imageIcon,
              TransmissionSignalQualityResolver.resolve(campaign, leftSpeaker, rightSpeaker),
              isModal);
    }

    /**
     * Constructs an immersive dialog using an explicit video transmission quality override.
     *
     * <p>The remaining parameters inherit the contracts documented by the adjacent constructor.</p>
     *
     * @param signalQuality non-null visual fidelity override applied to speaker portraits
     *
     * @throws NullPointerException if {@code signalQuality} is {@code null}
     */
    public ImmersiveDialogCore(Campaign campaign, @Nullable Person leftSpeaker, @Nullable Person rightSpeaker,
          String centerMessage, List<ButtonLabelTooltipPair> buttons, @Nullable String outOfCharacterMessage,
          @Nullable Integer centerWidth, boolean isVerticalLayout, @Nullable JPanel supplementalPanel,
          @Nullable ImageIcon imageIcon, TransmissionSignalQuality signalQuality, boolean isModal) {
        // Initialize
        this.campaign = campaign;
        this.leftSpeaker = leftSpeaker;
        this.rightSpeaker = rightSpeaker;
        this.signalQuality = Objects.requireNonNull(signalQuality, "signalQuality");
        responseActivationController = new ResponseActivationController(this::dispose);

        CENTER_WIDTH = (centerWidth != null) ? centerWidth : CENTER_WIDTH;

        // Title
        setTitle();

        boolean useFullWindowContent = configureWindowsFullWindowContent();

        JPanel transmissionPanel = ImmersiveDialogStyle.createBackdropPanel();
        transmissionPanel.setBorder(useFullWindowContent
                                          ? BorderFactory.createEmptyBorder()
                                          : new EmptyBorder(PADDING * 2, PADDING * 2, PADDING * 2, PADDING * 2));
        transmissionPanel.add(ImmersiveDialogStyle.createHeaderPanel(
              getText("ImmersiveDialog.header.title"),
              getText(signalQuality.statusResourceKey),
              useFullWindowContent), BorderLayout.NORTH);

        JPanel transmissionBody = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        transmissionBody.setOpaque(false);
        if (useFullWindowContent) {
            transmissionBody.setBorder(new EmptyBorder(0, PADDING * 2, PADDING * 2, PADDING * 2));
        }

        // Main Panel to hold all boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, PADDING, 0, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for speaker details
        if (leftSpeaker != null) {
            JPanel pnlLeftSpeaker = ImmersiveDialogStyle.createSourcePanel(
                  getSourceLabel(leftSpeaker),
                  buildLeftSpeakerPanel(leftSpeaker, campaign));

            // Add pnlLeftSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 0;
            mainPanel.add(pnlLeftSpeaker, constraints);
            gridx++;
        }

        // Center box for the message
        JPanel pnlCenter = createCenterBox(centerMessage, buttons, isVerticalLayout, supplementalPanel, imageIcon);
        TransmissionRevealPanel transmissionReveal = new TransmissionRevealPanel(pnlCenter);
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 2;
        mainPanel.add(transmissionReveal, constraints);
        gridx++;

        // Right box for speaker details
        if (rightSpeaker != null) {
            JPanel pnlRightSpeaker = ImmersiveDialogStyle.createSourcePanel(
                  getSourceLabel(rightSpeaker),
                  buildRightSpeakerPanel(rightSpeaker, campaign));

            // Add pnlRightSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 1;
            mainPanel.add(pnlRightSpeaker, constraints);
        }

        // Add mainPanel to dialog
        transmissionBody.add(mainPanel, BorderLayout.CENTER);

        // Bottom panel, for OOC information
        southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        if (outOfCharacterMessage != null) {
            populateOutOfCharacterPanel(outOfCharacterMessage);
        }

        // Add southPanel to the dialog
        transmissionBody.add(southPanel, BorderLayout.SOUTH);
        transmissionPanel.add(transmissionBody, BorderLayout.CENTER);
        add(transmissionPanel, BorderLayout.CENTER);

        // Dialog settings
        Window locationReference = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        pack();
        applyDynamicDialogSize(locationReference);
        messageScrollPane.getViewport().setViewPosition(new Point(0, 0));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(isModal);
        setLocationRelativeTo(locationReference); // Needs to be after pack
        setVisible(true);
    }

    @Override
    public void dispose() {
        if (responseActivationController != null) {
            responseActivationController.cancel();
        }
        super.dispose();
    }

    @Override
    public void removeNotify() {
        if (responseActivationController != null) {
            responseActivationController.cancel();
        }
        super.removeNotify();
    }

    /**
     * Sets the title of the dialog window using localized text.
     */
    protected void setTitle() {
        setTitle(MHQConstants.PROJECT_NAME);
    }

    private String getSourceLabel(Person speaker) {
        return getText(resolveSourceLabelResourceKey(
              speaker.isCommander(), isPlayerForcePersonnel(campaign, speaker)));
    }

    static boolean isPlayerForcePersonnel(@Nullable Campaign campaign, Person speaker) {
        if (campaign == null) {
            return false;
        }

        PlayerForce playerForce = campaign.getPlayerForce();
        if (playerForce == null) {
            return false;
        }

        ForceHumanResources humanResources = playerForce.getHumanResources();
        if (humanResources == null) {
            return false;
        }

        Collection<Person> personnel = humanResources.getPersonnel();
        return personnel != null && personnel.contains(speaker);
    }

    static String resolveSourceLabelResourceKey(boolean isCommander, boolean isPlayerForcePersonnel) {
        if (isCommander) {
            return "ImmersiveDialog.source.command";
        }
        if (isPlayerForcePersonnel) {
            return "ImmersiveDialog.source.unitChannel";
        }
        return "ImmersiveDialog.source.fieldContact";
    }

    private boolean configureWindowsFullWindowContent() {
        String lookAndFeelClass = UIManager.getLookAndFeel().getClass().getName();
        boolean decorationsDisabled = "false".equalsIgnoreCase(
              System.getProperty(FLATLAF_WINDOW_DECORATIONS_PROPERTY));
        if (!isWindows10OrLater() ||
                  !lookAndFeelClass.startsWith(FLATLAF_CLASS_PREFIX) ||
                  decorationsDisabled) {
            return false;
        }

        JRootPane rootPane = getRootPane();
        rootPane.putClientProperty(USE_WINDOW_DECORATIONS_PROPERTY, Boolean.TRUE);
        rootPane.putClientProperty(FULL_WINDOW_CONTENT_PROPERTY, Boolean.TRUE);
        rootPane.putClientProperty(TITLE_BAR_SHOW_TITLE_PROPERTY, Boolean.FALSE);
        rootPane.putClientProperty(TITLE_BAR_SHOW_ICON_PROPERTY, Boolean.FALSE);
        return true;
    }

    private static boolean isWindows10OrLater() {
        if (!System.getProperty("os.name", "").startsWith("Windows")) {
            return false;
        }

        String version = System.getProperty("os.version", "");
        int separator = version.indexOf('.');
        String majorVersion = (separator < 0) ? version : version.substring(0, separator);
        return MathUtility.parseInt(majorVersion, -1) >= 10;
    }

    /**
     * Creates and returns a central panel containing the main dialog message and a button panel. This panel is designed
     * to display a central message, typically in HTML format, using a {@link JEditorPane}, along with an optional list
     * of buttons displayed below the message.
     * <ul>
     *   <li>The message is placed in the {@link JEditorPane}, styled for a consistent width.</li>
     *   <li>The panel includes a scrollable viewport if the message content overflows.</li>
     *   <li>An additional button panel is added at the bottom of the central panel.</li>
     * </ul>
     *
     * @param centerMessage The main dialog message as a string, typically in HTML format. This can include basic HTML
     *                      for formatting purposes.
     * @param buttons       A list of {@link ButtonLabelTooltipPair} objects defining the buttons to be displayed at the
     *                      bottom of the panel. These buttons can have labels, tooltips, and custom actions.
     *
     * @return A {@link JPanel} with the message displayed in the center and buttons at the bottom.
     */
    private JPanel createCenterBox(String centerMessage, List<ButtonLabelTooltipPair> buttons, boolean isVerticalLayout,
          @Nullable JPanel supplementalPanel, @Nullable ImageIcon imageIcon) {
          JPanel centerPanel = ImmersiveDialogStyle.createAngularSurfacePanel();

        // Buttons panel
        JPanel buttonPanel = populateButtonPanel(buttons, isVerticalLayout);

        // Create a JEditorPane for the center message
        JEditorPane editorPane = getJEditorPane(centerMessage, buttonPanel);
        setFontScaling(editorPane, false, 1.1);

        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        JScrollablePanel viewport = new JScrollablePanel();
        viewport.setLayout(new BorderLayout());
        viewport.setOpaque(false);
        viewport.add(editorPane, BorderLayout.CENTER);
        if (supplementalPanel != null) {
            supplementalPanel.setOpaque(false);
            ImmersiveDialogStyle.applySupplementalControlStyle(supplementalPanel);
            viewport.add(supplementalPanel, BorderLayout.SOUTH);
            fetchSpinnerFromPanel(supplementalPanel);
            fetchComboBoxFromPanel(supplementalPanel);
        }

        FastJScrollPane scrollPane = new FastJScrollPane();
        messageScrollPane = scrollPane;
        scrollPane.setViewportView(viewport);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Create a container with a border for the padding
        JPanel scrollPaneContainer = new JPanel(new BorderLayout());
        scrollPaneContainer.setOpaque(false);
        scrollPaneContainer.add(scrollPane, BorderLayout.CENTER);

        // Create a JLabel for the image above the JEditorPane
        JLabel imageLabel = new JLabel();
        if (imageIcon != null) {
            imageLabel.setIcon(imageIcon);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setBorder(new EmptyBorder(0, 0, PADDING, 0));
        }

        // Create a panel for the image and editorPane
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setOpaque(false);
        if (imageIcon != null) {
            contentPanel.add(imageLabel, BorderLayout.NORTH);
        }
        contentPanel.add(scrollPaneContainer, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(ImmersiveDialogStyle.createSectionSpacingBorder());
        messagePanel.add(ImmersiveDialogStyle.createSectionHeader(
              getText("ImmersiveDialog.message.title"),
              ImmersiveDialogStyle.getSignalColor()), BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);

        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setOpaque(false);
        responsePanel.setBorder(ImmersiveDialogStyle.createSectionSpacingBorder());
        responsePanel.add(ImmersiveDialogStyle.createSectionHeader(
              getText("ImmersiveDialog.response.title"),
              ImmersiveDialogStyle.getSignalColor()), BorderLayout.NORTH);
        responsePanel.add(buttonPanel, BorderLayout.CENTER);

        centerPanel.setLayout(new GridBagLayout());
        GridBagConstraints sectionConstraints = new GridBagConstraints();
        sectionConstraints.gridx = 0;
        sectionConstraints.gridy = 0;
        sectionConstraints.fill = GridBagConstraints.BOTH;
        sectionConstraints.weightx = 1;
        sectionConstraints.weighty = 1;
        centerPanel.add(messagePanel, sectionConstraints);

        sectionConstraints.gridy = 1;
        sectionConstraints.weighty = 0;
        centerPanel.add(responsePanel, sectionConstraints);

        return centerPanel;
    }

    private void applyDynamicDialogSize(@Nullable Window locationReference) {
        if (messageScrollPane == null) {
            return;
        }

        Rectangle usableScreenBounds = getUsableScreenBounds(locationReference);
        Dimension naturalDialogSize = getPreferredSize();
        Dimension naturalViewportSize = messageScrollPane.getPreferredSize();
        ImmersiveDialogSizing.SizingResult sizing = ImmersiveDialogSizing.calculate(
              naturalDialogSize.height,
              naturalViewportSize.height,
              scaleForGUI(120),
              usableScreenBounds.height);

        messageScrollPane.setVerticalScrollBarPolicy(sizing.requiresScrolling()
                                                           ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                                                           : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        messageScrollPane.setPreferredSize(new Dimension(naturalViewportSize.width, sizing.viewportHeight()));

        pack();
        setSize(min(usableScreenBounds.width, getWidth()), min(sizing.dialogHeight(), getHeight()));
    }

    private Rectangle getUsableScreenBounds(@Nullable Window locationReference) {
        GraphicsConfiguration configuration = locationReference == null
                                                    ? null
                                                    : locationReference.getGraphicsConfiguration();
        if (configuration == null) {
            configuration = getGraphicsConfiguration();
        }
        if (configuration == null) {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            configuration = device.getDefaultConfiguration();
        }

        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(bounds.x + insets.left,
              bounds.y + insets.top,
              bounds.width - insets.left - insets.right,
              bounds.height - insets.top - insets.bottom);
    }

    private JEditorPane getJEditorPane(String centerMessage, JPanel buttonPanel) {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.setOpaque(false);
        editorPane.setBorder(new EmptyBorder(0, getPadding(), 0, getPadding()));

        // Use inline CSS to set font family, size, and other style properties
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<html><div style='width: %dpx; %s'>%s</div></html>",
              max(buttonPanel.getPreferredSize().width, CENTER_WIDTH),
              fontStyle,
              centerMessage));
        return editorPane;
    }

    /**
     * Handles hyperlink clicks from HTML content dialog.
     *
     * <p>
     * This method processes the provided hyperlink reference to determine the type of command and executes the
     * appropriate action. It supports commands for displaying a glossary entry or focusing on a specific person in the
     * campaign.
     * </p>
     *
     * <b>Supported Commands:</b>
     * <ul>
     *   <li>{@code GLOSSARY_COMMAND_STRING}: Opens a new {@link GlossaryEntryDialog} to display the
     *   referenced glossary entry.</li>
     *   <li>{@code PERSON_COMMAND_STRING}: Focuses on a specific person in the campaign using
     *   their unique identifier (UUID). If using this, you will need to ensure your dialog has
     *   modal set to {@code false}</li>
     * </ul>
     *
     * <p>
     * If the command is not recognized, no action is performed.
     * </p>
     *
     * @param parent    The parent {@link JDialog} instance to associate with the new dialog, if created.
     * @param campaign  The {@link Campaign} instance that contains application and campaign data.
     * @param reference The hyperlink reference used to determine the command and additional information (e.g., a
     *                  specific glossary term key or a person's UUID).
     */
    public static void handleImmersiveHyperlinkClick(JDialog parent, Campaign campaign, String reference) {
        String[] splitReference = reference.split(":");

        String commandKey = splitReference[0];
        String entryKey = splitReference[1];

        CampaignGUI campaignGUI = campaign.getGUI();

        if (commandKey.equalsIgnoreCase(GLOSSARY_COMMAND_STRING)) {
            GlossaryEntry glossaryEntry = GlossaryEntry.getGlossaryEntryFromLookUpName(entryKey);

            if (glossaryEntry == null) {
                LOGGER.warn("Glossary entry not found: {}", entryKey);
                return;
            }

            new GlossaryEntryDialog(parent, glossaryEntry);
        } else if (commandKey.equalsIgnoreCase(DOCUMENTATION_COMMAND_STRING)) {
            DocumentationEntry documentationEntry = DocumentationEntry.getDocumentationEntryFromLookUpName(entryKey);

            if (documentationEntry == null) {
                LOGGER.warn("Documentation entry not found: {}", entryKey);
                return;
            }

            try {
                new GlossaryDocumentationEntryDialog(parent, documentationEntry);
            } catch (Exception ex) {
                LOGGER.error("Failed to open PDF", ex);
            }
        } else if (commandKey.equalsIgnoreCase(PERSON_COMMAND_STRING)) {
            final UUID id = UUID.fromString(reference.split(":")[1]);
            campaignGUI.focusOnPerson(id);
        } else if (commandKey.equalsIgnoreCase(MISSION_COMMAND_STRING)) {
            try {
                final UUID targetId = UUID.fromString(entryKey);
                campaignGUI.focusOnMission(targetId);
            } catch (Exception e) {
                LOGGER.error("Failed to parse mission ID: {}", entryKey, e);
            }
        } else if (commandKey.equalsIgnoreCase(SCENARIO_COMMAND_STRING)) {
            try {
                final int targetId = MathUtility.parseInt(entryKey, -1);
                campaignGUI.focusOnScenario(targetId);
            } catch (Exception e) {
                LOGGER.error("Failed to parse scenario ID: {}", entryKey, e);
            }
        }
    }

    /**
     * Populates the Out-of-Character (OOC) panel with a specific message, resizing as needed.
     * <p>
     * This method appends a formatted OOC message to the bottom of the dialog, ensuring proper width alignment with any
     * visible speaker panels.
     *
     * @param outOfCharacterMessage The OOC message to display.
     */
    private void populateOutOfCharacterPanel(String outOfCharacterMessage) {
        JPanel pnlOutOfCharacter = ImmersiveDialogStyle.createInformationPanel();
        pnlOutOfCharacter.add(ImmersiveDialogStyle.createSectionHeader(
              getText("ImmersiveDialog.information.title"),
              ImmersiveDialogStyle.getInformationColor()), BorderLayout.NORTH);

        // Create a JEditorPane for the message
        JEditorPane editorPane = getJEditorPane(outOfCharacterMessage);
        setFontScaling(editorPane, false, 1);

        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(this::hyperlinkEventListenerActions);

        // Add the editor pane to the panel
        pnlOutOfCharacter.add(editorPane, BorderLayout.CENTER);

        // Add the panel to the southPanel
        southPanel.add(pnlOutOfCharacter, BorderLayout.CENTER);
    }

    private JEditorPane getJEditorPane(String outOfCharacterMessage) {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.setOpaque(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder());

        int width = CENTER_WIDTH;
        width += leftSpeaker != null ? IMAGE_WIDTH + PADDING : 0;
        width += rightSpeaker != null ? IMAGE_WIDTH + PADDING : 0;

        // Use inline CSS to set font family, size, and other style properties
        editorPane.setText(String.format("<html><head><style>body { margin: 0; } " +
                                               "p { margin-top: 0; margin-bottom: 0; }</style></head>" +
                                               "<body><div style='width: %dpx'>%s</div></body></html>",
              width,
              outOfCharacterMessage));
        return editorPane;
    }

    /**
     * Handles actions triggered by hyperlink events, such as clicks on hyperlinks. This method identifies when the
     * event type is {@code HyperlinkEvent.EventType.ACTIVATED} and processes the event accordingly by delegating to the
     * specified handler.
     *
     * @param evt the {@code HyperlinkEvent} which contains details about the hyperlink interaction. It could represent
     *            events such as entering, exiting, or activating a hyperlink.
     */
    protected void hyperlinkEventListenerActions(HyperlinkEvent evt) {
        if (evt.getEventType() == EventType.ACTIVATED) {
            handleImmersiveHyperlinkClick(this, campaign, evt.getDescription());
        }
    }

    /**
     * Populates a button panel with a list of buttons, each defined by a label and an optional tooltip.
     * <p>
     * This method dynamically creates buttons based on the provided {@link ButtonLabelTooltipPair} objects. Each button
     * is added to the specified {@link JPanel} (`buttonPanel`) and arranged according to the selected layout style
     * (`isVerticalLayout`).
     * </p>
     *
     * @param buttons          A {@link List} of {@link ButtonLabelTooltipPair} instances, where each pair defines the
     *                         label and tooltip for a button.
     * @param isVerticalLayout A {@code boolean} value indicating the layout style: {@code true} for vertical stacking,
     *                         {@code false} for horizontal arrangement.
     */
    protected JPanel populateButtonPanel(List<ButtonLabelTooltipPair> buttons, boolean isVerticalLayout) {
        final int padding = getPadding();

        // Main container panel to hold the button panel
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BorderLayout(padding, padding));
        containerPanel.setOpaque(false);

        // Create button panel
        JPanel buttonPanel = createResponseButtonPanel(isVerticalLayout, padding);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(padding, padding, padding, padding);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = isVerticalLayout ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        gbc.weightx = isVerticalLayout ? 1 : 0;

        List<TransmissionResponseButton> buttonList = new ArrayList<>();

        // First pass: Create buttons and determine the largest size
        for (ButtonLabelTooltipPair buttonStrings : buttons) {
            TransmissionResponseButton button = null;

            if (isVerticalLayout) {
                StringBuilder buttonLabel = new StringBuilder("<html>");

                String label = buttonStrings.btnLabel();
                String tooltip = buttonStrings.btnTooltip();
                if (label != null && tooltip != null) {
                    buttonLabel.append("<b>")
                          .append(buttonStrings.btnLabel())
                          .append("</b>")
                          .append("<br>")
                          .append(tooltip);
                } else if (label == null && tooltip != null) {
                    buttonLabel.append(tooltip);
                } else if (label != null) {
                    buttonLabel.append(label);
                }

                button = new TransmissionResponseButton(buttonLabel.toString());
            } else {
                String label = buttonStrings.btnLabel();
                String tooltip = buttonStrings.btnTooltip();
                String text = resolveHorizontalButtonText(label, tooltip);
                if (text != null) {
                    button = new TransmissionResponseButton(text);

                    if (label != null && tooltip != null) {
                        button.setToolTipText(wordWrap(tooltip));
                    }
                }
            }

            if (button == null) {
                continue;
            }

            ImmersiveDialogStyle.applyResponseButtonStyle(button);

            // Left-align text, if using vertical layout, otherwise we want text centralized (default)
            if (isVerticalLayout) {
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setHorizontalTextPosition(SwingConstants.LEFT);
            }

            TransmissionResponseButton responseButton = button;
            int responseIndex = buttons.indexOf(buttonStrings);
            button.addActionListener(evt -> responseActivationController.activate(
                  responseButton,
                  buttonList,
                  () -> captureResponseState(responseIndex),
                getText("ImmersiveDialog.response.transmitting.text"),
                getText("ImmersiveDialog.response.transmitting.compact"),
                getText("ImmersiveDialog.response.transmitting.accessible")));

            buttonList.add(button);
        }

        applyUniformButtonSizes(buttonList);

        // Final pass: Add buttons to the panel
        for (TransmissionResponseButton button : buttonList) {
            if (isVerticalLayout) {
                buttonPanel.add(button, gbc);
                // If we're using a vertical layout, we just want the buttons stacked
                gbc.gridy++;
            } else {
                buttonPanel.add(button);
            }
        }

        // Add the button panel to the bottom of the container
        containerPanel.add(buttonPanel, BorderLayout.CENTER);

        return containerPanel;
    }

    static JPanel createResponseButtonPanel(boolean isVerticalLayout, int padding) {
        JPanel buttonPanel = new JPanel(isVerticalLayout
                                              ? new GridBagLayout()
                                              : new WrapLayout(FlowLayout.CENTER, padding, padding));
        buttonPanel.setOpaque(false);
        return buttonPanel;
    }

    static String resolveHorizontalButtonText(String label, String tooltip) {
        return label != null ? label : tooltip;
    }

    static void applyUniformButtonSizes(List<? extends JButton> buttons) {
        int largestWidth = 0;
        int largestHeight = 0;
        for (JButton button : buttons) {
            if (button == null) {
                throw new IllegalArgumentException("buttons cannot contain null");
            }
            Dimension requiredSize = calculateRequiredButtonSize(button);
            largestWidth = max(largestWidth, requiredSize.width);
            largestHeight = max(largestHeight, requiredSize.height);
        }

        Dimension largestSize = new Dimension(largestWidth, largestHeight);
        for (JButton button : buttons) {
            if (button == null) {
                throw new IllegalArgumentException("buttons cannot contain null");
            }
            button.setMinimumSize(new Dimension(largestSize));
            button.setPreferredSize(new Dimension(largestSize));
        }
    }

    private static Dimension calculateRequiredButtonSize(JButton button) {
        Dimension uiPreferredSize = button.getPreferredSize();
        String text = button.getText();
        if (BasicHTML.isHTMLString(text)) {
            return new Dimension(uiPreferredSize);
        }

        FontMetrics fontMetrics = button.getFontMetrics(button.getFont());
        Insets insets = button.getInsets();
        int contentWidth = text == null ? 0 : fontMetrics.stringWidth(text);
        int contentHeight = fontMetrics.getHeight();
        Icon icon = button.getIcon();
        if (icon != null) {
            contentWidth += icon.getIconWidth() + button.getIconTextGap();
            contentHeight = max(contentHeight, icon.getIconHeight());
        }

        int horizontalAllowance = scaleForGUI(RESPONSE_BUTTON_HORIZONTAL_LAYOUT_ALLOWANCE);
        int verticalAllowance = scaleForGUI(RESPONSE_BUTTON_VERTICAL_LAYOUT_ALLOWANCE);
        int requiredWidth = contentWidth + insets.left + insets.right + horizontalAllowance * 2;
        int requiredHeight = contentHeight + insets.top + insets.bottom + verticalAllowance * 2;
        return new Dimension(max(uiPreferredSize.width, requiredWidth), max(uiPreferredSize.height, requiredHeight));
    }

    private void captureResponseState(int responseIndex) {
        setDialogChoice(responseIndex);

        if (spinner != null) {
            setSpinnerValue((int) spinner.getValue());
        }

        if (comboBox != null) {
            setComboBoxChoiceIndex(comboBox.getSelectedIndex());
        }
    }

    /**
     * Retrieves the {@link JSpinner} contained within the specified {@link JPanel}.
     * <p>
     * This method iterates through all components in the given panel to find and return the first instance of
     * {@link JSpinner}. If no such spinner is found, it logs an error and returns a new, empty {@link JSpinner} as a
     * fallback.
     * </p>
     *
     * @param supplementalPanel The {@link JPanel} to search for a {@link JSpinner}. Must not be {@code null}.
     */
    private void fetchSpinnerFromPanel(JPanel supplementalPanel) {
        for (Component component : supplementalPanel.getComponents()) {
            if (component instanceof JSpinner) {
                spinner = (JSpinner) component;
            }
        }
    }

    private void fetchComboBoxFromPanel(JPanel supplementalPanel) {
        for (Component component : supplementalPanel.getComponents()) {
            if (component instanceof MMComboBox<?>) {
                comboBox = (MMComboBox<?>) component;
            }
        }
    }

    /**
     * Builds a panel containing a visual representation of the left-side speaker.
     *
     * <p>The panel includes the speaker's image (if available) and their descriptive information. The name and
     * description are determined from the given {@link Campaign} and optional {@link Person}. The layout and sizing are
     * set to align with user interface expectations.</p>
     *
     * @param speaker  the {@link Person} to be shown as the left speaker; may be {@code null}
     * @param campaign the current {@link Campaign} providing context and fallback values
     *
     * @return a {@link JPanel} displaying the left speaker's image and description
     */
    protected JPanel buildLeftSpeakerPanel(@Nullable Person speaker, Campaign campaign) {
        JPanel speakerBox = new JPanel();
        speakerBox.setLayout(new BoxLayout(speakerBox, BoxLayout.Y_AXIS));
        speakerBox.setOpaque(false);
        speakerBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        speakerBox.setMaximumSize(new Dimension(IMAGE_WIDTH, scaleForGUI(MAX_VALUE)));

        // Get speaker details
        String speakerName = campaign.getPlayerForce().getName();
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        }

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIcon(speakerIcon, IMAGE_WIDTH, true);
        }
        TransmissionImagePanel imagePanel = new TransmissionImagePanel(speakerIcon, signalQuality);
        imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(String.format(
              "<html><div style='width:%dpx; text-align:center;'>%s</div></html>",
              IMAGE_WIDTH,
              speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the speakerBox
        speakerBox.add(imagePanel);
        if (speakerIcon != null) {
            speakerBox.add(Box.createRigidArea(scaleForGUI(0, PADDING)));
        }
        speakerBox.add(leftDescription);

        return speakerBox;
    }

    /**
     * Builds a panel for the right-side speaker.
     *
     * <p><b>Usage:</b> By default, this implementation delegates to {@link #buildLeftSpeakerPanel(Person, Campaign)}.
     * However, it can be independently overridden to allow for customization of the panel. Such as when we want to have
     * the left and right speaker panels visually distinctive.</p>
     *
     * @param speaker  the {@link Person} to be shown as the right speaker; may be {@code null}
     * @param campaign the current {@link Campaign} providing context and fallback values
     *
     * @return a {@link JPanel} displaying the right speaker's image and description
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected JPanel buildRightSpeakerPanel(@Nullable Person speaker, Campaign campaign) {
        return buildLeftSpeakerPanel(speaker, campaign);
    }

    /**
     * Assembles the speaker description based on the provided speaker and campaign details.
     *
     * <p>
     * The description includes:
     * <ul>
     *   <li>The speaker's title and roles (both primary and secondary, if applicable).</li>
     *   <li>The force associated with the speaker.</li>
     *   <li>A fallback to the campaign name if the speaker is not available.</li>
     * </ul>
     *
     * @param campaign    The current campaign.
     * @param speaker     The {@link Person} representing the speaker, or {@code null} to use fallback data.
     * @param speakerName The name/title to use for the speaker if one exists.
     *
     * @return A {@link StringBuilder} containing the formatted HTML description of the speaker.
     */
    public static StringBuilder getSpeakerDescription(Campaign campaign, Person speaker, String speakerName) {
        StringBuilder speakerDescription = new StringBuilder();

        if (speaker != null) {
            speakerDescription.append("<b>").append(speakerName).append("</b>");

            boolean isClan = campaign.getPlayerForce().getFaction().isClan();

            PersonnelRole primaryRole = speaker.getPrimaryRole();
            if (!primaryRole.isNone()) {
                speakerDescription.append("<br>").append(primaryRole.getLabel(isClan));
            }

            PersonnelRole secondaryRole = speaker.getSecondaryRole();
            if (!secondaryRole.isNone()) {
                speakerDescription.append("<br>").append(secondaryRole.getLabel(isClan));
            }

            Unit assignedUnit = speaker.getUnit();
            if (assignedUnit != null) {
                int forceId = assignedUnit.getFormationId();

                if (forceId != FORMATION_NONE) {
                    Formation formation = campaign.getPlayerForce().getFormation(forceId);

                    if (formation != null) {
                        speakerDescription.append("<br>").append(formation.getName());
                    }
                }
            }
        } else {
            speakerDescription.append("<b>").append(campaign.getPlayerForce().getName()).append("</b>");
        }
        return speakerDescription;
    }

    /**
     * Retrieves the speaker's icon for dialogs. If no speaker is supplied, the faction icon for the campaign is
     * returned instead.
     *
     * @param campaign the {@link Campaign} instance containing the faction icon; can be {@code null} to use a default
     *                 image.
     * @param speaker  the {@link Person} serving as the speaker for the dialog; can be {@code null}.
     *
     * @return an {@link ImageIcon} for the speaker's portrait, or the faction icon if the speaker is {@code null}.
     */
    public static @Nullable ImageIcon getSpeakerIcon(@Nullable Campaign campaign, @Nullable Person speaker) {
        if (campaign == null) {
            return new ImageIcon("data/images/universe/factions/logo_mercenaries.png");
        }

        if (speaker == null) {
            return campaign.getCampaignFactionIcon();
        }

        Image baseImage;
        if (campaign.getPlayerForce().getHumanResources().getPersonnel().contains(speaker)) {
            Portrait portrait = speaker.getPortrait();

            if (portrait == null ||
                      portrait.getFilename().equalsIgnoreCase(DEFAULT_PORTRAIT_FILENAME) ||
                      portrait.getFilename().equalsIgnoreCase(NO_PORTRAIT_NAME)) {
                return campaign.getCampaignFactionIcon();
            }

            baseImage = portrait.getBaseImage();
        } else {
            baseImage = Factions.getFactionLogo(campaign.getGameYear(), speaker.getOriginFaction().getShortName())
                              .getImage();
        }

        // The following sorcery is due to the compressed manner in which personnel portraits are stored.
        // We need to manipulate the original base image, otherwise it looks grainy and terrible.
        ImageObserver observer = (img, infoFlags, x, y, width, height) -> true;
        int baseImageHeight = baseImage.getHeight(observer);
        int baseImageWidth = baseImage.getWidth(observer);
        int targetWidth = Math.max(1, IMAGE_WIDTH);

        int height = (int) Math.ceil((double) targetWidth * baseImageHeight / baseImageWidth);

        return new ImageIcon(baseImage.getScaledInstance(targetWidth, height, Image.SCALE_SMOOTH));
    }

    static final class ResponseActivationController {
        static final int TRANSMISSION_CONFIRMATION_DELAY_MS = 350;

        private final Runnable dialogDisposer;
        private Timer confirmationTimer;
        private TransmissionResponseButton selectedButton;
        private boolean responseActivated;
        private boolean completionPending;
        private boolean transmissionFeedbackVisible;

          ResponseActivationController(Runnable dialogDisposer) {
            this.dialogDisposer = dialogDisposer;
        }

        boolean activate(TransmissionResponseButton selectedButton,
              List<TransmissionResponseButton> responseButtons, Runnable captureResponse,
              String confirmationText, String compactConfirmationText, String accessibleFeedbackText) {
            if (responseActivated) {
                return false;
            }

            responseActivated = true;
            captureResponse.run();
            for (TransmissionResponseButton responseButton : responseButtons) {
                if (responseButton != selectedButton) {
                    responseButton.setEnabled(false);
                }
            }
            this.selectedButton = selectedButton;
            selectedButton.lockTransmissionConfirmation(
                confirmationText, compactConfirmationText, accessibleFeedbackText);
            transmissionFeedbackVisible = true;

            completionPending = true;
            confirmationTimer = new Timer(TRANSMISSION_CONFIRMATION_DELAY_MS,
                  event -> completeTransmission());
            confirmationTimer.setRepeats(false);
            confirmationTimer.start();
            return true;
        }

        void completeTransmission() {
            if (!completionPending) {
                return;
            }

            completionPending = false;
            stopTimer();
            clearTransmissionFeedback();
            dialogDisposer.run();
        }

        void cancel() {
            completionPending = false;
            stopTimer();
            clearTransmissionFeedback();
        }

        boolean isConfirmationTimerRunning() {
            return confirmationTimer != null && confirmationTimer.isRunning();
        }

        boolean isConfirmationTimerRepeating() {
            return confirmationTimer != null && confirmationTimer.isRepeats();
        }

        private void stopTimer() {
            if (confirmationTimer != null) {
                confirmationTimer.stop();
                confirmationTimer = null;
            }
        }

        private void clearTransmissionFeedback() {
            if (!transmissionFeedbackVisible) {
                return;
            }

            transmissionFeedbackVisible = false;
            selectedButton.clearTransmissionConfirmation();
            selectedButton = null;
        }
    }

    /**
     * Represents a label-tooltip pair for constructing UI buttons. Each button displays a label and optionally provides
     * a tooltip when hovered.
     */
    public record ButtonLabelTooltipPair(String btnLabel, @Nullable String btnTooltip) {
        /**
         * Constructs a ButtonLabelTooltipPair with the given label and tooltip.
         *
         * @param btnLabel   The label for the button. Must not be {@code null}.
         * @param btnTooltip The tooltip for the button. Can be {@code null} if no tooltip is given.
         *
         * @throws IllegalArgumentException if both {@code btnLabel} and {@code btnTooltip} are {@code null}.
         */
        public ButtonLabelTooltipPair {
            if (btnLabel == null && btnTooltip == null) {
                throw new IllegalArgumentException("btnLabel and btnTooltip cannot be null at the same time.");
            }
        }

        /**
         * Retrieves the button label.
         *
         * @return The button label as a {@link String}.
         */
        @Override
        public String btnLabel() {
            return btnLabel;
        }

        /**
         * Retrieves the button tooltip.
         *
         * @return The button tooltip as a {@link String}, or {@code null} if no tooltip is set.
         */
        @Override
        public @Nullable String btnTooltip() {
            return btnTooltip;
        }
    }
}
