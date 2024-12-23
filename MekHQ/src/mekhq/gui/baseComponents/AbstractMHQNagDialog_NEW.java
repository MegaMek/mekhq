/*
 * Copyright (c) 2024 The MegaMek Team. All Rights Reserved.
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
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * An abstract base class for displaying a nag dialog within MekHQ.
 * <p>
 * This dialog is used to show certain informational or warning messages during a campaign,
 * with the option for users to ignore future messages of the same type.
 * It includes visual elements for presenting a speaker image, description, and some configurable
 * details, along with buttons and a checkbox for user input.
 * </p>
 *
 * <p>
 * Extending this class allows customization of the dialog’s behavior and content,
 * while maintaining a consistent design across the application.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Displays a two-panel layout with speaker details and a message.</li>
 *   <li>Allows users to advance to the next day or cancel, optionally ignoring future warnings.</li>
 *   <li>Localized using resource bundles.</li>
 * </ul>
 */
public abstract class AbstractMHQNagDialog_NEW extends JDialog {
    private final Campaign campaign;
    /**
     * Unique key for this nag dialog, used to track if the dialog has been ignored
     * by the user in the campaign settings.
     */
    private final String nagKey;
    /**
     * The right-side description label which displays the message.
     * Dynamically updated when {@link #setRightDescriptionMessage(String)} is called.
     */
    private JLabel rightDescription = new JLabel();
    private String rightDescriptionMessage;

    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);

    /**
     * Indicates whether the user selected the "Advance Day" option.
     */
    private boolean advanceDaySelected = true;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.GUI", MekHQ.getMHQOptions().getLocale());

    private static final MMLogger logger = MMLogger.create(AbstractMHQNagDialog_NEW.class);

    /**
     * Constructs an AbstractMHQNagDialog_NEW with the provided campaign, owner, and nag key.
     *
     * @param campaign The current campaign, used to determine speaker details and other contextual data.
     * @param owner    The parent frame of the dialog (often the main application window).
     * @param nagKey   A unique key to identify this nag dialog for tracking ignore preferences.
     */
    public AbstractMHQNagDialog_NEW(final Campaign campaign, final Frame owner, final String nagKey) {
        super(owner, "", true);
        setTitle(resources.getString("incomingTransmission.title"));

        this.campaign = campaign;
        this.nagKey = nagKey;
        this.rightDescriptionMessage = "";

        setLayout(new BorderLayout());

        // Main Panel to hold both boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        // Left box for speaker details
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Get speaker details
        Person speaker = campaign.getSeniorAdminCommandPerson();
        String speakerName = (speaker != null) ? speaker.getFullTitle() : campaign.getName();

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(speaker, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the leftBox
        leftBox.add(imageLabel);
        leftBox.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        leftBox.add(leftDescription);

        // Add leftBox to mainPanel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        mainPanel.add(leftBox, gbc);

        // Right box: Just a message
        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setBorder(BorderFactory.createEtchedBorder());
        rightDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH, rightDescriptionMessage));
        rightBox.add(rightDescription);

        // Add rightBox to mainPanel
        gbc.gridx = 1;
        gbc.weightx = 1; // Allow horizontal stretching
        mainPanel.add(rightBox, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Checkbox Panel
        JPanel checkboxPanel = new JPanel();
        JCheckBox ignoreFutureNags = new JCheckBox(resources.getString("ignoreFutureNags.checkbox"));
        checkboxPanel.add(ignoreFutureNags);

        // Buttons panel
        JPanel buttonPanel = new JPanel();

        JButton advanceDayButton = new JButton(resources.getString("button.advanceDay"));
        advanceDayButton.addActionListener(e -> {
            advanceDaySelected = true;
            MekHQ.getMHQOptions().setNagDialogIgnore(this.nagKey, ignoreFutureNags.isSelected());
            logger.info("Nag dialog cancelled: (" + nagKey + ':' + ignoreFutureNags.isSelected() + ')');
            dispose();
        });

        JButton cancelButton = new JButton(resources.getString("button.cancel"));
        cancelButton.addActionListener(e -> {
            advanceDaySelected = false;
            MekHQ.getMHQOptions().setNagDialogIgnore(this.nagKey, ignoreFutureNags.isSelected());
            logger.info("Nag dialog cancelled: (" + nagKey + ':' + ignoreFutureNags.isSelected() + ')');
            dispose();
        });

        buttonPanel.add(advanceDayButton);
        buttonPanel.add(cancelButton);

        // Combine Checkbox and Buttons into a single panel
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(checkboxPanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        // Dialog settings
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Displays the dialog to the user and waits for a response.
     *
     * <p>
     * This method makes the dialog visible and halts further execution until the user
     * either dismisses or interacts with the dialog (e.g., clicks a button).
     * </p>
     *
     * @return {@code true} if the user selected the "Advance Day" option, otherwise {@code false}.
     */
    public boolean showDialog() {
        setVisible(true);
        return advanceDaySelected;
    }

    /**
     * Updates the message displayed in the right-hand panel of the dialog.
     *
     * <p>
     * This method updates the content dynamically and repaints the dialog to reflect changes immediately.
     * </p>
     *
     * @param rightDescriptionMessage The message to display, expected to be a valid HTML string.
     */
    public void setRightDescriptionMessage(String rightDescriptionMessage) {
        this.rightDescriptionMessage = rightDescriptionMessage;

        // Update the right description JLabel
        rightDescription.setText(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH, rightDescriptionMessage));

        repaint();
        pack();
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
     * @param speaker     The {@link Person} representing the speaker, or {@code null} to use fallback data.
     * @param speakerName The name/title to use for the speaker if one exists.
     * @return A {@link StringBuilder} containing the formatted HTML description of the speaker.
     */
    private StringBuilder getSpeakerDescription(Person speaker, String speakerName) {
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
     * Checks if the user selected the "Advance Day" action.
     *
     * <p>
     * This result is set when the user interacts with the dialog’s buttons, either
     * advancing to the next day or cancelling the dialog. This value will be {@code true}
     * if the "Advance Day" button was selected, otherwise {@code false}.
     * </p>
     *
     * @return {@code true} if "Advance Day" was chosen, otherwise {@code false}.
     */
    public boolean isAdvanceDaySelected() {
        return advanceDaySelected;
    }
}
