/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog.resupplyAndCaches;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

import static megamek.common.Compute.randomInt;
import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerDescription;
import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * This class provides a utility method to display a custom dialog related to abandoned convoys
 * in the MekHQ game. The dialog includes detailed information and visuals, like the convoy
 * commander or speaker, a status update message, and employer details.
 */
public class DialogAbandonedConvoy extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    public DialogAbandonedConvoy(Campaign campaign, AtBContract contract, @Nullable Force targetConvoy) {
        setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "incomingTransmission.title"));

        final int INSERT_SIZE = UIUtil.scaleForGUI(10);

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
                speakerName = getFormattedTextAt(RESOURCE_BUNDLE, "dialogBorderConvoySpeakerDefault.text",
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

        String message = getFormattedTextAt(RESOURCE_BUNDLE,
            "statusUpdateAbandoned" + randomInt(20) + ".text",
            campaign.getCommanderAddress(false));

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
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "logisticsDestroyed.text"));
        confirmButton.addActionListener(e -> dispose());
        buttonPanel.add(confirmButton);

        // Add the button panel to the container
        containerPanel.add(buttonPanel);

        // New panel (to be added below the button panel)
        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createEtchedBorder());
        infoPanel.setLayout(new BorderLayout());

        JLabel newPanelLabel = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH + RIGHT_WIDTH,
                getFormattedTextAt(RESOURCE_BUNDLE, "documentation.prompt")));
        infoPanel.add(newPanelLabel, BorderLayout.CENTER);

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
