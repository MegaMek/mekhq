/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerDescription;
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
public abstract class AbstractMHQNagDialog extends JDialog {
    /**
     * Unique key for this nag dialog, used to track if the dialog has been ignored
     * by the user in the campaign settings.
     */
    private final String nagKey;
    /**
     * The right-side description label which displays the message.
     * Dynamically updated when {@link #setRightDescriptionMessage(String)} is called.
     */
    private JLabel rightDescription;
    private String rightDescriptionMessage;

    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);

    /**
     * Indicates whether the user selected the "Advance Day" option.
     */
    private boolean advanceDaySelected = true;

    protected final transient ResourceBundle resources = ResourceBundle.getBundle(
        "mekhq.resources.GUI", MekHQ.getMHQOptions().getLocale());

    /**
     * Constructs an AbstractMHQNagDialog with the provided campaign and nag key.
     *
     * @param campaign The current campaign, used to determine speaker details and other contextual data.
     * @param nagKey   A unique key to identify this nag dialog for tracking ignore preferences.
     */
    public AbstractMHQNagDialog(final Campaign campaign, final String nagKey) {
        setTitle(resources.getString("incomingTransmission.title"));

        this.nagKey = nagKey;
        this.rightDescriptionMessage = "";

        setLayout(new BorderLayout());

        // Main Panel to hold both boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        // Left box for speaker details
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Get speaker details
        Person speaker = campaign.getSeniorAdminPerson(AdministratorSpecialization.COMMAND);
        String speakerName = (speaker != null) ? speaker.getFullTitle() : campaign.getName();

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIconToWidth(speakerIcon, 100);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add the image and description to the leftBox
        leftBox.add(imageLabel);
        leftBox.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        leftBox.add(leftDescription);

        // Add leftBox to mainPanel
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        mainPanel.add(leftBox, constraints);

        // Right box: Just a message
        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setBorder(BorderFactory.createEtchedBorder());
        rightDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH, rightDescriptionMessage));
        rightBox.add(rightDescription);

        // Add rightBox to mainPanel
        constraints.gridx = 1;
        constraints.weightx = 1; // Allow horizontal stretching
        mainPanel.add(rightBox, constraints);

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
            dispose();
        });

        JButton cancelButton = new JButton(resources.getString("button.cancel"));
        cancelButton.addActionListener(e -> {
            advanceDaySelected = false;
            MekHQ.getMHQOptions().setNagDialogIgnore(this.nagKey, ignoreFutureNags.isSelected());
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
        setModal(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Displays the dialog to the user and waits for a response.
     *
     * <p>
     * This method makes the dialog visible and halts further execution until the user
     * either dismisses or interacts with the dialog (e.g., clicks a button).
     * </p>
     */
    public void showDialog() {
        setVisible(true);
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
     * Checks if the user selected the "Advance Day" action.
     *
     * <p>
     * This result is set when the user interacts with the dialog’s buttons, either
     * advancing to the next day or cancelling the dialog. This value will be {@code true}
     * if the "Advance Day" button was selected, otherwise {@code false}.
     * </p>
     *
     * @return {@code true} if "Advance Day" was canceled, otherwise {@code false}.
     */
    public boolean wasAdvanceDayCanceled() {
        return !advanceDaySelected;
    }
}
