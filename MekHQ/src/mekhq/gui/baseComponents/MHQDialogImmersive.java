/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.baseComponents;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.GlossaryDialog;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
import java.util.List;

import static java.lang.Math.max;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.utilities.glossary.GlossaryLibrary.GLOSSARY_COMMAND_STRING;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * An immersive dialog used in MekHQ to display interactions between speakers,
 * messages, and actions. The dialog supports entities such as speakers, campaign,
 * buttons, and optional details for enhanced storytelling.
 *
 * <p>It allows displaying one or more speakers in a dialog alongside a central message,
 * optional out-of-character notes, and UI buttons for user interaction.</p>
 *
 * <p>The dialog is flexible in terms of panel layout and width adjustments,
 * allowing for dynamic configurations based on the input parameters.</p>
 */
public class MHQDialogImmersive extends JDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    private Campaign campaign;

    private int CENTER_WIDTH = UIUtil.scaleForGUI(400);

    private final int PADDING = UIUtil.scaleForGUI(5);
    protected final int IMAGE_WIDTH = 125; // This is scaled to GUI by 'scaleImageIconToWidth'

    private JPanel northPanel;
    private JPanel southPanel;
    private JPanel buttonPanel;
    private Person leftSpeaker;
    private Person rightSpeaker;

    private int dialogChoice;

    private static final MMLogger logger = MMLogger.create(MHQDialogImmersive.class);

    /**
     * Retrieves the user's selected dialog choice.
     * <p>
     *     <strong>Usage:</strong> This allows us to keep function code out of the GUI element,
     *     making it far easier to test what's happening for any given option selection. Create
     *     the dialog, as normal, then fetch whatever decision the user made and perform any code
     *     actions required.
     * </p>
     *
     * @return An integer representing the index of the button selected by the user.
     *         If the dialog is closed without selection, this will return the {@code defaultChoiceIndex}
     *         defined during construction.
     */
    public int getDialogChoice() {
        return dialogChoice;
    }

    /**
     * Full constructor for initializing the immersive dialog with detailed layouts.
     *
     * @param campaign The {@link Campaign} tied to the dialog.
     * @param leftSpeaker Optional left-side {@link Person}; {@code null} if none.
     * @param rightSpeaker Optional right-side {@link Person}; {@code null} if none.
     * @param centerMessage The main message displayed in the dialog's center.
     * @param buttons The list of {@link ButtonLabelTooltipPair} actions for the dialog.
     * @param outOfCharacterMessage Optional out-of-character message below the buttons.
     * @param centerWidth Optional width for the center panel; defaults if null.
     */
    public MHQDialogImmersive(Campaign campaign, @Nullable Person leftSpeaker,
                              @Nullable Person rightSpeaker, String centerMessage,
                              List<ButtonLabelTooltipPair> buttons, @Nullable String outOfCharacterMessage,
                              @Nullable Integer centerWidth) {
        // Initialize
        this.campaign = campaign;
        this.leftSpeaker = leftSpeaker;
        this.rightSpeaker = rightSpeaker;

        CENTER_WIDTH = (centerWidth != null) ? centerWidth : CENTER_WIDTH;

        // Title
        setTitle();

        // Main Panel to hold all boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        int gridx = 0;

        // Left box for speaker details
        if (leftSpeaker != null) {
            JPanel pnlLeftSpeaker = buildSpeakerPanel(leftSpeaker, campaign);

            // Add pnlLeftSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            mainPanel.add(pnlLeftSpeaker, constraints);
            gridx++;
        }

        // Center box for the message
        JPanel pnlCenter = createCenterBox(centerMessage, buttons);
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 2;
        constraints.weighty = 2;
        mainPanel.add(pnlCenter, constraints);
        gridx++;

        // Right box for speaker details
        if (rightSpeaker != null) {
            JPanel pnlRightSpeaker = buildSpeakerPanel(rightSpeaker, campaign);

            // Add pnlRightSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            mainPanel.add(pnlRightSpeaker, constraints);
        }

        // Add mainPanel to dialog
        add(mainPanel, BorderLayout.CENTER);

        // Bottom panel, for OOC information
        southPanel = new JPanel(new BorderLayout());
        if (outOfCharacterMessage != null) {
            populateOutOfCharacterPanel(outOfCharacterMessage);
        }

        // Add southPanel to the dialog
        add(southPanel, BorderLayout.SOUTH);

        // Dialog settings
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setModal(true);
        pack();
        setLocationRelativeTo(null); // Needs to be after pack
        setVisible(true);
    }

    /**
     * Sets the title of the dialog window using localized text.
     */
    protected void setTitle() {
        setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "incomingTransmission.title"));
    }

    /**
     * Creates and returns a central panel containing the main dialog message and a button panel.
     * This panel is designed to display a central message, typically in HTML format,
     * using a {@link JEditorPane}, along with an optional list of buttons displayed below the message.
     * <ul>
     *   <li>The message is placed in the {@link JEditorPane}, styled for a consistent width.</li>
     *   <li>The panel includes a scrollable viewport if the message content overflows.</li>
     *   <li>An additional button panel is added at the bottom of the central panel.</li>
     * </ul>
     *
     * @param centerMessage The main dialog message as a string, typically in HTML format.
     *                      This can include basic HTML for formatting purposes.
     * @param buttons       A list of {@link ButtonLabelTooltipPair} objects defining the buttons to
     *                      be displayed at the bottom of the panel. These buttons can have labels,
     *                      tooltips, and custom actions.
     * @return A {@link JPanel} with the message displayed in the center and buttons at the bottom.
     */

    private JPanel createCenterBox(String centerMessage, List<ButtonLabelTooltipPair> buttons) {
        northPanel = new JPanel(new BorderLayout());

        // Buttons panel
        buttonPanel = new JPanel();
        populateButtonPanel(buttons);

        // Create a JEditorPane for the center message
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder());

        // Use inline CSS to set font family, size, and other style properties
        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s'>%s</div>",
            max(buttonPanel.getPreferredSize().width, CENTER_WIDTH), fontStyle, centerMessage));
        setFontScaling(editorPane, false, 1.1);
        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(evt -> {
            if (evt.getEventType() == EventType.ACTIVATED) {
                handleHyperlinkClick(campaign, evt.getDescription());
            }
        });

        // Wrap the JEditorPane in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setMinimumSize(new Dimension(CENTER_WIDTH, scrollPane.getHeight()));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Create a container with a border for the padding
        JPanel scrollPaneContainer = new JPanel(new BorderLayout());
        scrollPaneContainer.setBorder(BorderFactory.createEmptyBorder(PADDING, 0, PADDING, 0));
        scrollPaneContainer.add(scrollPane, BorderLayout.CENTER);

        // Add the scrollPane with padding to the northPanel
        northPanel.add(scrollPaneContainer, BorderLayout.CENTER);

        // Ensure the scrollbars default to the top-left position
        SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));

        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        return northPanel;
    }

    /**
     * Handles hyperlink clicks from HTML content.
     *
     * @param campaign The {@link Campaign} instance that contains relevant data.
     * @param reference The hyperlink reference (e.g., a specific identifier).
     */
    protected void handleHyperlinkClick(Campaign campaign, String reference) {
        String[] splitReference = reference.split(":");

        String commandKey = splitReference[0];
        String entryKey = splitReference[1];

        if (commandKey.equals(GLOSSARY_COMMAND_STRING)) {
            new GlossaryDialog(this, campaign, entryKey);
        }
    }

    /**
     * Populates the Out-of-Character (OOC) panel with a specific message, resizing as needed.
     * <p>
     * This method appends a formatted OOC message to the bottom of the dialog, ensuring proper width
     * alignment with any visible speaker panels.
     *
     * @param outOfCharacterMessage The OOC message to display.
     */
    private void populateOutOfCharacterPanel(String outOfCharacterMessage) {
        JPanel pnlOutOfCharacter = new JPanel(new GridBagLayout());

        // Create a compound border with an etched border and padding (empty border)
        pnlOutOfCharacter.setBorder(
            BorderFactory.createEtchedBorder()
        );

        // Create a JEditorPane for the message
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);

        int width = CENTER_WIDTH;
        width += leftSpeaker != null ? IMAGE_WIDTH + PADDING : 0;
        width += rightSpeaker != null ? IMAGE_WIDTH + PADDING : 0;

        // Use inline CSS to set font family, size, and other style properties
        editorPane.setText(String.format("<div style='width: %s'>%s</div>", width, outOfCharacterMessage));
        setFontScaling(editorPane, false, 1);

        // Add a HyperlinkListener to capture hyperlink clicks
        editorPane.addHyperlinkListener(evt -> {
            if (evt.getEventType() == EventType.ACTIVATED) {
                handleHyperlinkClick(campaign, evt.getDescription());
            }
        });

        // Add the editor pane to the panel
        pnlOutOfCharacter.add(editorPane);

        // Add the panel to the southPanel
        southPanel.add(pnlOutOfCharacter, BorderLayout.SOUTH);
    }

    /**
     * Populates the button panel with the provided buttons.
     * <p>
     * Each button in the panel represents a {@link ButtonLabelTooltipPair}, defining its displayed label
     * and optional tooltip. Clicking a button will set the {@code dialogChoice} to the corresponding index
     * of the button in the list and close the dialog window.
     *
     * @param buttons A list of button label-tooltip pairs defining the content of the buttons.
     */
    private void populateButtonPanel(List<ButtonLabelTooltipPair> buttons) {
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        for (ButtonLabelTooltipPair buttonStrings : buttons) {
            JButton button = new JButton(buttonStrings.btnLabel());

            String tooltip = buttonStrings.btnTooltip();
            if (tooltip != null) {
                button.setToolTipText(wordWrap(tooltip));
            }

            button.setMinimumSize(button.getPreferredSize());

            button.addActionListener(evt -> {
                dialogChoice = buttons.indexOf(buttonStrings);
                dispose();
            });

            buttonPanel.add(button, gbc);

            gbc.gridx++;
            if (gbc.gridx % 3 == 0) { // Move to a new row after every third button
                gbc.gridx = 0;
                gbc.gridy++;
            }
        }
    }

    /**
     * Builds a panel for displaying a speaker's image, name, and role.
     * <p>
     * This method creates a vertically stacked panel that includes the person's icon, title,
     * and any additional descriptive information (e.g., roles, forces, or campaign affiliations).
     *
     * @param speaker The character shown in the dialog, can be {@code null} for no speaker
     * @param campaign      The current campaign.
     * @return A {@link JPanel} forming the speaker's dialog box.
     */
    protected JPanel buildSpeakerPanel(@Nullable Person speaker, Campaign campaign) {
        JPanel speakerBox = new JPanel();
        speakerBox.setLayout(new BoxLayout(speakerBox, BoxLayout.Y_AXIS));
        speakerBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        speakerBox.setMaximumSize(new Dimension(IMAGE_WIDTH, Integer.MAX_VALUE));

        // Get speaker details
        String speakerName = campaign.getName();
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        }

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIconToWidth(speakerIcon, IMAGE_WIDTH);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width:%dpx; text-align:center;'>%s</div></html>",
                IMAGE_WIDTH, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the speakerBox
        speakerBox.add(imageLabel);
        speakerBox.add(leftDescription);

        return speakerBox;
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
     * @return A {@link StringBuilder} containing the formatted HTML description of the speaker.
     */
    public static StringBuilder getSpeakerDescription(Campaign campaign, Person speaker, String speakerName) {
        StringBuilder speakerDescription = new StringBuilder();

        if (speaker != null) {
            speakerDescription.append("<b>").append(speakerName).append("</b>");

            boolean isClan = campaign.getFaction().isClan();

            PersonnelRole primaryRole = speaker.getPrimaryRole();
            if (!primaryRole.isNone()) {
                speakerDescription.append("<br>").append(primaryRole.getName(isClan));
            }

            PersonnelRole secondaryRole = speaker.getSecondaryRole();
            if (!secondaryRole.isNone()) {
                speakerDescription.append("<br>").append(secondaryRole.getName(isClan));
            }

            Unit assignedUnit = speaker.getUnit();
            if (assignedUnit != null) {
                int forceId = assignedUnit.getForceId();

                if (forceId != FORCE_NONE) {
                    Force force = campaign.getForce(forceId);

                    if (force != null) {
                        speakerDescription.append("<br>").append(force.getName());
                    }
                }
            }
        } else {
            speakerDescription.append("<b>").append(campaign.getName()).append("</b>");
        }
        return speakerDescription;
    }

    /**
     * Retrieves the speaker's icon for dialogs. If no speaker is supplied, the faction icon
     * for the campaign is returned instead.
     *
     * @param campaign the {@link Campaign} instance containing the faction icon; can be
     *                 {@code null} to use a default image.
     * @param speaker  the {@link Person} serving as the speaker for the dialog; can be {@code null}.
     * @return an {@link ImageIcon} for the speaker's portrait, or the faction icon if the speaker is {@code null}.
     */
    public static @Nullable ImageIcon getSpeakerIcon(@Nullable Campaign campaign, @Nullable Person speaker) {
        if (campaign == null) {
            return new ImageIcon("data/images/universe/factions/logo_mercenaries.png");
        }

        if (speaker == null) {
            return campaign.getCampaignFactionIcon();
        }

        return speaker.getPortrait().getImageIcon();
    }

    /**
     * Represents a label-tooltip pair for constructing UI buttons.
     * Each button displays a label and optionally provides a tooltip when hovered.
     */
    public record ButtonLabelTooltipPair(String btnLabel, String btnTooltip) {
        /**
         * Constructs a ButtonLabelTooltipPair with the given label and tooltip.
         *
         * @param btnLabel   The label for the button. Must not be {@code null}.
         * @param btnTooltip The tooltip for the button. Can be {@code null} if no tooltip is given.
         * @throws IllegalArgumentException if {@code btnLabel} is {@code null}.
         */
        public ButtonLabelTooltipPair(String btnLabel, @Nullable String btnTooltip) {
            if (btnLabel == null) {
                throw new IllegalArgumentException("btnLabel cannot be null.");
            }
            this.btnLabel = btnLabel;
            this.btnTooltip = btnTooltip;
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
