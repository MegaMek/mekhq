/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.resupplyAndCaches;

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerDescription;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogRoleplayEvent} class handles the creation and display of roleplay event dialogs
 * for convoy missions in MekHQ. These dialogs provide narrative elements to enhance the immersion
 * of convoy missions by using dynamic content, player convoy details, and localized text.
 */
public class DialogRoleplayEvent extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a roleplay event dialog for a player convoy. The dialog is used to present messages related
     * to narrative events that occur during convoy missions, accompanied by speaker details for immersion.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *     <li>Constructs a {@link JDialog} with a localized title from the resource bundle.</li>
     *     <li>Determines the speaker:
     *         <ul>
     *             <li>Attempts to retrieve the force commander from the convoy.</li>
     *             <li>If no commander is found, defaults to displaying the convoy name.</li>
     *         </ul>
     *     </li>
     *     <li>Fetches the speaker's faction or individual icon, scales it to 100 pixels wide for consistency,
     *         and displays it as part of the dialog.</li>
     *     <li>Constructs and displays a narrative message that includes game-specific dynamic details
     *         such as the commander address derived from the campaign.</li>
     *     <li>Organizes the GUI into panels:
     *         <ul>
     *             <li>An icon panel for the speaker's visualization.</li>
     *             <li>A description panel containing an HTML-formatted, localized message displayed in a
     *                 centered, styled layout.</li>
     *         </ul>
     *     </li>
     *     <li>Includes a confirmation button that allows the user to dismiss the dialog, concluding the roleplay event.</li>
     * </ol>
     *
     * <p>The dialog is modal to ensure the player engages with it before resuming other actions.</p>
     *
     * @param campaign      the {@link Campaign} instance, providing context for accessing relevant personnel
     *                      and dynamic game data like the player commander address.
     * @param playerConvoy  the {@link Force} instance representing the player's convoy. This is used to retrieve
     *                      the force commander and the convoy's name if no commander is available.
     * @param eventText     the narrative text describing the roleplay event. This string may include formatting
     *                      placeholders ({@code %s}) to dynamically incorporate campaign-specific details.
     */
    public DialogRoleplayEvent(Campaign campaign, Force playerConvoy, String eventText) {
        setTitle(resources.getString("incomingTransmission.title"));

        // Main Panel to hold both boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(INSERT_SIZE, INSERT_SIZE, INSERT_SIZE, INSERT_SIZE);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;

        // Left box for speaker details
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Get speaker details
        UUID speakerId = playerConvoy.getForceCommanderID();
        Person speaker = campaign.getPerson(speakerId);

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            speakerName = campaign.getName();
        }

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
        leftBox.add(Box.createRigidArea(new Dimension(0, INSERT_SIZE)));
        leftBox.add(leftDescription);

        // Add leftBox to mainPanel
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        mainPanel.add(leftBox, constraints);

        // Right box: Just a message
        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setBorder(BorderFactory.createEtchedBorder());

        String message = String.format(eventText, campaign.getCommanderAddress(false));

        JLabel rightDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH, message));
        rightBox.add(rightDescription);

        // Add rightBox to mainPanel
        constraints.gridx = 1;
        constraints.weightx = 1; // Allow horizontal stretching
        mainPanel.add(rightBox, constraints);

        add(mainPanel, BorderLayout.CENTER);

        // Create a container panel to hold both the button panel and the new panel
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS)); // Stack vertically

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton(resources.getString("convoyConfirm.text"));
        confirmButton.addActionListener(e -> dispose());
        buttonPanel.add(confirmButton);

        // Add the button panel to the container
        containerPanel.add(buttonPanel);

        // New panel (to be added below the button panel)
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel lblInfo = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH + LEFT_WIDTH,
                String.format(resources.getString("documentation.prompt"))));
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblInfo, BorderLayout.CENTER);
        infoPanel.setBorder(BorderFactory.createEtchedBorder());

        // Add the new panel to the container (below the button panel)
        containerPanel.add(infoPanel);

        // Add the container panel to the dialog (at the bottom of the layout)
        add(containerPanel, BorderLayout.SOUTH);

        // Dialog settings
        pack();
        setModal(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}
