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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

public class MHQDialogImmersive extends JDialog {
    private final Campaign campaign;

    private int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    private int CENTER_WIDTH = UIUtil.scaleForGUI(400);
    private int RIGHT_WIDTH = UIUtil.scaleForGUI(200);

    private final int INSERT_SIZE = UIUtil.scaleForGUI(10);
    private final int IMAGE_WIDTH = 100; // This is scaled to GUI by 'scaleImageIconToWidth'

    private JPanel southPanel;
    private JPanel buttonPanel;
    private final Person leftSpeaker;
    private final Person rightSpeaker;

    private int dialogChoice;

    private final transient ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.GUI", MekHQ.getMHQOptions().getLocale());

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
     * Constructs an immersive dialog for displaying speakers, a message, and action buttons.
     *
     * @param campaign              The {@link Campaign} tied to the dialog.
     * @param leftSpeaker           The optional {@link Person} to display as the left speaker in the dialog.
     *                              Pass {@code null} for no left speaker.
     * @param rightSpeaker          The optional {@link Person} to display as the right speaker in the dialog.
     *                              Pass {@code null} for no right speaker.
     * @param centerMessage         The main message displayed in the center of the dialog.
     * @param buttons               A list of {@link ButtonLabelTooltipPair} controls to add below the message. Button
     *                              labels and tooltips are defined in these pairs.
     * @param outOfCharacterMessage An optional message displayed below the buttons (normally for
     *                             OOC notes). Pass {@code null} for no message.
     * @param leftWidth             Optional custom width for the left speaker panel. Pass {@code null}
     *                             to use default width.
     * @param defaultChoiceIndex   The index of the button included in {@code buttons} that is
     *                            presumed to have been selected if the user cancels the dialog.
     *                            Essentially, the default user choice if no other choice is made.
     * @param centerWidth           Optional custom width for the center panel. Pass {@code null} to
     *                             use default width.
     * @param rightWidth            Optional custom width for the right speaker panel. Pass {@code null}
     *                             to use default width.
     */
    public MHQDialogImmersive(Campaign campaign, @Nullable Person leftSpeaker,
                              @Nullable Person rightSpeaker, String centerMessage,
                              List<ButtonLabelTooltipPair> buttons,
                              @Nullable String outOfCharacterMessage, int defaultChoiceIndex,
                              @Nullable Integer leftWidth, @Nullable Integer centerWidth,
                              @Nullable Integer rightWidth) {
        this.campaign = campaign;
        this.leftSpeaker = leftSpeaker;
        this.rightSpeaker = rightSpeaker;

        dialogChoice = defaultChoiceIndex;

        LEFT_WIDTH = (leftWidth != null) ? leftWidth : LEFT_WIDTH;
        CENTER_WIDTH = (centerWidth != null) ? centerWidth : CENTER_WIDTH;
        RIGHT_WIDTH = (rightWidth != null) ? rightWidth : RIGHT_WIDTH;

        int gridx = 0;

        setTitle(resources.getString("incomingTransmission.title"));

        // Main Panel to hold all boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        // Left box for speaker details
        if (leftSpeaker != null) {
            JPanel pnlLeftSpeaker = buildSpeakerPanel(true);

            // Add pnlLeftSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 0;
            mainPanel.add(pnlLeftSpeaker, constraints);

            gridx++;
        }

        // Center box: for message
        JPanel pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setBorder(BorderFactory.createEtchedBorder());
        JLabel lblCenterMessage = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                CENTER_WIDTH, centerMessage)
        );
        pnlCenter.add(lblCenterMessage);

        // Add mainPanel to dialog
        constraints.gridx = gridx;
        constraints.gridy = 0;
        constraints.weightx = 0;
        mainPanel.add(pnlCenter, constraints);

        gridx++;

        // Right box for speaker details
        if (rightSpeaker != null) {
            JPanel pnlRightSpeaker = buildSpeakerPanel(false);

            // Add pnlRightSpeaker to mainPanel
            constraints.gridx = gridx;
            constraints.gridy = 0;
            constraints.weightx = 0;
            mainPanel.add(pnlRightSpeaker, constraints);
        }

        // Add mainPanel to dialog
        add(mainPanel, BorderLayout.CENTER);

        // Buttons panel
        buttonPanel = new JPanel();
        populateButtonPanel(buttons);

        // Bottom panel, for OOC information
        southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);

        if (outOfCharacterMessage != null) {
            populateOutOfCharacterPanel(outOfCharacterMessage);
        }

        // Add southPanel to the dialog
        add(southPanel, BorderLayout.SOUTH);

        // Dialog settings
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
    }

    /**
     * Creates and displays an instance of the {@link MHQDialogImmersive}.
     * <p>
     * This method acts as a factory for generating and displaying the dialog,
     * allowing easy instantiation without requiring custom panel widths.
     *
     * @param campaign              The {@link Campaign} tied to the dialog.
     * @param leftSpeaker           The optional {@link Person} to display as the left speaker in
     *                              the dialog. Can be {@code null}.
     * @param rightSpeaker          The optional {@link Person} to display as the right speaker in
     *                             the dialog. Can be {@code null}.
     * @param centerMessage         The main message to display in the dialog.
     * @param buttons               A list of {@link ButtonLabelTooltipPair} buttons with labels
     *                             and optional tooltips.
     * @param outOfCharacterMessage An optional custom OOC message displayed beneath the buttons.
     *                             Can be {@code null}.
     * @param defaultChoiceIndex    The default index selected if no user action is taken.
     * @return A newly created and displayed instance of {@link MHQDialogImmersive}.
     *
     * @see #MHQDialogImmersive(Campaign, Person, Person, String, List, String, int, Integer,
     * Integer, Integer)
     */
    public static MHQDialogImmersive createMHQDialogImmersive(Campaign campaign, @Nullable Person leftSpeaker,
                                                @Nullable Person rightSpeaker, String centerMessage,
                                                List<ButtonLabelTooltipPair> buttons,
                                                @Nullable String outOfCharacterMessage,
                                                int defaultChoiceIndex) {
        return new MHQDialogImmersive(campaign, leftSpeaker, rightSpeaker, centerMessage, buttons,
            outOfCharacterMessage, defaultChoiceIndex, null, null, null);
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
        JPanel pnlOutOfCharacter = new JPanel(new BorderLayout());
        pnlOutOfCharacter.setBorder(BorderFactory.createEtchedBorder());

        int bottomPanelWidth = CENTER_WIDTH;
        bottomPanelWidth += leftSpeaker == null ? 0 : LEFT_WIDTH;
        bottomPanelWidth += rightSpeaker == null ? 0 : RIGHT_WIDTH;

        JLabel lblOutOfCharacter = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                bottomPanelWidth, outOfCharacterMessage));

        lblOutOfCharacter.setHorizontalAlignment(SwingConstants.CENTER);
        pnlOutOfCharacter.add(lblOutOfCharacter, BorderLayout.CENTER);

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
        for (ButtonLabelTooltipPair buttonStrings : buttons) {
            JButton button = new JButton(buttonStrings.btnLabel());

            String tooltip = buttonStrings.btnTooltip();
            if (tooltip != null) {
                button.setToolTipText(tooltip);
            }

            button.addActionListener(evt -> {
                dialogChoice = buttons.indexOf(buttonStrings);
                dispose();
            });

            buttonPanel.add(button);
        }
    }

    /**
     * Builds a panel for displaying a speaker's image, name, and role.
     * <p>
     * This method creates a vertically stacked panel that includes the person's icon, title,
     * and any additional descriptive information (e.g., roles, forces, or campaign affiliations).
     *
     * @param isLeftSpeaker Indicates if the individual is displayed on the left side of the dialog.
     *                      This affects alignment and panel width.
     * @return A {@link JPanel} forming the speaker's dialog box.
     */
    private JPanel buildSpeakerPanel(boolean isLeftSpeaker) {
        final Person speaker = isLeftSpeaker ? leftSpeaker : rightSpeaker;
        final int width = isLeftSpeaker ? LEFT_WIDTH : RIGHT_WIDTH;
        final float alignment = isLeftSpeaker ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT;

        JPanel speakerBox = new JPanel();
        speakerBox.setLayout(new BoxLayout(speakerBox, BoxLayout.Y_AXIS));
        speakerBox.setAlignmentX(alignment);

        // Get speaker details
        String speakerName = speaker.getFullTitle();

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
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                width, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the speakerBox
        speakerBox.add(imageLabel);
        speakerBox.add(Box.createRigidArea(new Dimension(0, INSERT_SIZE)));
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
     * A class to store and represent a button label and its associated tooltip.
     * <p>
     * This class is useful for scenarios where you want to pair a button's display label
     * with an optional tooltip message.
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
