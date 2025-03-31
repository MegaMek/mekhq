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

import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerDescription;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.personnel.Person;

/**
 * The {@code DialogPlayerConvoyOption} class provides functionality to display a dialog that prompts the player to
 * choose whether to use their own convoy to transport cargo. The dialog dynamically generates content, localized
 * messages, and speaker visuals to guide the player's decision.
 */
public class DialogPlayerConvoyOption extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Displays a dialog that allows the player to decide if they want to use their own convoy for resupply. The dialog
     * presents key information such as required cargo tonnage, available convoy capacity, and the number of operational
     * player convoys. When convoy usage is forced, the dialog reflects that with a customized message and altered
     * behavior.
     *
     * <p>This method performs the following tasks:</p>
     * <ul>
     *     <li>Builds a {@link JDialog} with a title and localized content fetched from a {@link ResourceBundle}.</li>
     *     <li>Determines and displays the speaker (e.g., logistics representative or faction representative)
     *         with an appropriate icon, scaled to 100px wide.</li>
     *     <li>Calculates relevant convoy data:
     *         <ul>
     *             <li>Required cargo tonnage for the operation</li>
     *             <li>Total player convoy capacity</li>
     *             <li>Number of operational convoys, with proper pluralization for the display text</li>
     *         </ul>
     *     </li>
     *     <li>Generates a decision-related message tailored to either optional or forced use of a convoy.</li>
     *     <li>Constructs the GUI with:
     *         <ul>
     *             <li>An icon and HTML-formatted description message panel.</li>
     *             <li>Action buttons—Accept (choose to use player convoy) and Refuse (reject)—with appropriate behavior.</li>
     *         </ul>
     *     </li>
     *     <li>Attaches a {@link WindowListener} to handle dialog closure and set default behavior
     *         (refuse convoy) if the dialog is closed without a selection.</li>
     *     <li>Applies modal blocking to ensure the dialog is interacted with before proceeding.</li>
     * </ul>
     *
     * <p>The dialog dynamically disables the "Accept" button if no operational convoys are available.</p>
     *
     * @param resupply                the {@link Resupply} instance providing campaign context, cargo details, and
     *                                available player convoys.
     * @param forcedUseOfPlayerConvoy a boolean flag indicating whether the use of the player's convoy is mandatory. If
     *                                true, the optional decision is bypassed, and the dialog displays a notice
     *                                reflecting the forced choice.
     */
    public DialogPlayerConvoyOption(Resupply resupply, boolean forcedUseOfPlayerConvoy) {
        final Campaign campaign = resupply.getCampaign();

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
        Person speaker = campaign.getSeniorAdminPerson(AdministratorSpecialization.LOGISTICS);

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            speakerName = campaign.getName();
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

        int playerConvoyCount = resupply.getPlayerConvoys().size();
        String pluralizer = playerConvoyCount != 1 ? "s" : "";
        String messageResource;

        String message;
        if (forcedUseOfPlayerConvoy) {
            messageResource = "usePlayerConvoyForced.text";

            message = getFormattedTextAt(RESOURCE_BUNDLE,
                  messageResource,
                  campaign.getCommanderAddress(false),
                  resupply.getTargetCargoTonnagePlayerConvoy(),
                  resupply.getTotalPlayerCargoCapacity(),
                  playerConvoyCount,
                  pluralizer);
        } else {
            messageResource = "usePlayerConvoyOptional.text";

            message = getFormattedTextAt(RESOURCE_BUNDLE,
                  messageResource,
                  campaign.getCommanderAddress(false),
                  resupply.getTargetCargoTonnagePlayerConvoy(),
                  resupply.getTotalPlayerCargoCapacity(),
                  playerConvoyCount,
                  pluralizer,
                  resupply.getTargetCargoTonnage());
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

        // Create the buttons and add their action listener.
        JButton acceptButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "confirmAccept.text"));
        acceptButton.addActionListener(e -> {
            dispose();
            resupply.setUsePlayerConvoy(true);
        });
        acceptButton.setEnabled(playerConvoyCount > 0);
        buttonPanel.add(acceptButton);

        JButton refuseButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "confirmRefuse.text"));
        refuseButton.addActionListener(e -> {
            dispose();
            resupply.setUsePlayerConvoy(false);
        });
        buttonPanel.add(refuseButton);

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
