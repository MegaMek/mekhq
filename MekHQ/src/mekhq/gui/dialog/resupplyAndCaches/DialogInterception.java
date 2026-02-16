/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.resupplyAndCaches;

import static megamek.common.compute.Compute.randomInt;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerDescription;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.personnel.Person;

/**
 * @deprecated unused
 */
@Deprecated(since = "0.05.06", forRemoval = true)
public class DialogInterception extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Displays a dialog for an interception event that occurs during a resupply operation.
     *
     * <p>This method performs the following steps:</p>
     * 1. Constructs a {@link JDialog} with a title from the localized {@link ResourceBundle}. 2. Determines the
     * speaker's name and icon. This involves: - Retrieving the force commander from the convoy (if available). - Using
     * fallback values for the speaker name and icon if no target convoy was provided. 3. Creates a message describing
     * the interception event, using dynamic text generated from the resources. 4. Builds a GUI with Swing components: -
     * A description panel containing a localized, HTML-styled message. - An image panel displaying the speaker's icon
     * (sized to a width of 100 px). 5. Adds confirmation buttons to the dialog, allowing users to close it. 6. Displays
     * the dialog as modal to block further user interaction until dismissed.
     *
     * @param resupply     the {@link Resupply} instance containing the current campaign and contract details. Used to
     *                     access mission context, player commander information, and employer details.
     * @param targetConvoy the optional {@link Formation} representing the convoy involved in the interception. If
     *                     {@code null}, the dialog will use default values for the speaker and faction visuals.
     */
    public DialogInterception(Resupply resupply, @Nullable Formation targetConvoy) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

        setTitle(getFormattedTextAt(RESOURCE_BUNDLE, "incomingTransmission.title"));

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
            UUID speakerId = targetConvoy.getFormationCommanderID();
            speaker = campaign.getPerson(speakerId);
        }

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            if (targetConvoy == null) {
                speakerName = getFormattedTextAt(RESOURCE_BUNDLE,
                      "dialogBorderConvoySpeakerDefault.text",
                      contract.getEmployerName(campaign.getGameYear()));
            } else {
                speakerName = campaign.getName();
            }
        }

        // Add speaker image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIcon(speakerIcon, 100, true);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s</div></html>",
              LEFT_WIDTH,
              speakerDescription));
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
            if (targetConvoy.formationContainsOnlyVTOLForces(campaign.getHangar(), false) ||
                      targetConvoy.formationContainsOnlyAerialForces(campaign.getHangar(), false, false)) {
                message = getFormattedTextAt(RESOURCE_BUNDLE,
                      "statusUpdateIntercepted.boilerplate",
                      campaign.getCommanderAddress(),
                      getFormattedTextAt(RESOURCE_BUNDLE, "interceptionInstructions.text"));
            }
        }

        if (message.isBlank()) {
            message = getFormattedTextAt(RESOURCE_BUNDLE,
                  "statusUpdateIntercepted" + randomInt(20) + ".text",
                  campaign.getCommanderAddress(),
                  getFormattedTextAt(RESOURCE_BUNDLE, "interceptionInstructions.text"));
        }

        JLabel rightDescription = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s</div></html>",
              RIGHT_WIDTH,
              message));
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
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "logisticsReceived.text"));
        confirmButton.addActionListener(e -> dispose());
        buttonPanel.add(confirmButton);

        // Add the button panel to the container
        containerPanel.add(buttonPanel);

        // New panel (to be added below the button panel)
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel lblInfo = new JLabel(String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
              RIGHT_WIDTH + LEFT_WIDTH,
              getFormattedTextAt(RESOURCE_BUNDLE, "documentation.prompt")));
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
