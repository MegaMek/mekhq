/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.forceContainsOnlyAerialForces;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.forceContainsOnlyVTOLForces;
import static mekhq.gui.baseComponents.AbstractMHQNagDialog.getSpeakerDescription;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogInterception} class is responsible for displaying a UI dialog when an
 * interception scenario occurs during a supply or convoy mission in MekHQ. The dialog uses
 * localized resources to generate the content dynamically and includes relevant visual elements
 * like the speaker's icon, title, and mission details.
 */
public class DialogInterception extends JDialog{
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog for an interception event that occurs during a resupply operation.
     *
     * <p>This method performs the following steps:</p>
     * 1. Constructs a {@link JDialog} with a title from the localized {@link ResourceBundle}.
     * 2. Determines the speaker's name and icon. This involves:
     *    - Retrieving the force commander from the convoy (if available).
     *    - Using fallback values for the speaker name and icon if no target convoy was provided.
     * 3. Creates a message describing the interception event, using dynamic text
     *    generated from the resources.
     * 4. Builds a GUI with Swing components:
     *    - A description panel containing a localized, HTML-styled message.
     *    - An image panel displaying the speaker's icon (sized to a width of 100px).
     * 5. Adds confirmation buttons to the dialog, allowing users to close it.
     * 6. Displays the dialog as modal to block further user interaction until dismissed.
     *
     * @param resupply    the {@link Resupply} instance containing the current campaign
     *                    and contract details. Used to access mission context, player commander
     *                    information, and employer details.
     * @param targetConvoy the optional {@link Force} representing the convoy involved
     *                     in the interception. If {@code null}, the dialog will use default values
     *                     for the speaker and faction visuals.
     */
    public DialogInterception(Resupply resupply, @Nullable Force targetConvoy) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

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
        Person speaker = null;
        if (targetConvoy != null) {
            UUID speakerId = targetConvoy.getForceCommanderID();
            speaker = campaign.getPerson(speakerId);
        }

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            if (targetConvoy == null) {
                speakerName = String.format(resources.getString("dialogBorderConvoySpeakerDefault.text"),
                    contract.getEmployerName(campaign.getGameYear()));
            } else {
                speakerName = campaign.getName();
            }
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

        String message = "";

        if (targetConvoy != null) {
            if (forceContainsOnlyVTOLForces(campaign, targetConvoy)
                || forceContainsOnlyAerialForces(campaign, targetConvoy)) {
                message = String.format(
                    resources.getString("statusUpdateIntercepted.boilerplate"),
                        campaign.getCommanderAddress(false),
                        resources.getString("interceptionInstructions.text"));
            }
        }

        if (message.isBlank()) {
            message = String.format(
                resources.getString("statusUpdateIntercepted" + randomInt(20) + ".text"),
                    campaign.getCommanderAddress(false),
                    resources.getString("interceptionInstructions.text"));
        }

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
        JButton confirmButton = new JButton(resources.getString("logisticsReceived.text"));
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
