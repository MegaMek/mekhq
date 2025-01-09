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

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getEnemyFactionReference;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogSwindled} class provides functionality to display a dialog related to swindling events
 * during guerrilla contract missions. This dialog presents localized narrative content with
 * dynamic elements such as faction logos and contextual details about enemy factions.
 */
public class DialogSwindled extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog notifying the player that they have been swindled during a resupply.
     * The dialog includes details about the incident, a visual representation via a faction logo, and a confirmation
     * button to allow the player to dismiss the dialog after reading it.
     *
     * <p>This method performs the following tasks:</p>
     * <ol>
     *     <li>Sets up a {@link JDialog} with a localized title and manages dialog layout.</li>
     *     <li>Retrieves and displays a faction representative's logo:
     *         <ul>
     *             <li>Uses a default faction logo (PIR for generic pirate factions).</li>
     *             <li>Scales the logo to 100 pixels for consistent sizing and presentation.</li>
     *         </ul>
     *     </li>
     *     <li>Generates a randomized, localized narrative message describing the swindling event:
     *         <ul>
     *             <li>Fetches the player's commander address from the campaign.</li>
     *             <li>Includes a reference to the enemy faction involved in the event, dynamically resolved from the resupply data.</li>
     *             <li>Applies randomization to select from predefined narrative templates for replay variety.</li>
     *         </ul>
     *     </li>
     *     <li>Organizes the dialog layout into:
     *         <ul>
     *             <li>An icon panel displaying the faction logo.</li>
     *             <li>A description panel containing the dynamically generated narrative message.</li>
     *         </ul>
     *     </li>
     *     <li>Adds a confirmation button that dismisses the dialog when pressed.</li>
     *     <li>Enforces modal behavior to ensure the player acknowledges the dialog before continuing.</li>
     * </ol>
     *
     * <p>This dialog enhances the narrative immersion of being swindled in missions by presenting
     * visually and contextually rich content relevant to the player's situation.</p>
     *
     * @param resupply the {@link Resupply} instance containing details about the current campaign, contract,
     *                 and mission context. This object is used to retrieve dynamic elements such as the enemy
     *                 faction and the player's information.
     */
    public DialogSwindled(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();

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
        final RandomCallsignGenerator callsignGenerator = RandomCallsignGenerator.getInstance();
        String smugglerCallSign = callsignGenerator.generate();
        String smugglerTitle = resources.getString("guerrillaSpeaker.text");
        String speakerName = String.format("<b>'%s'</b><br>%s", smugglerCallSign, smugglerTitle);

        ImageIcon speakerIcon = getFactionLogo(campaign, "PIR", true);
        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH, speakerName));
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

        String enemyFactionReference = getEnemyFactionReference(resupply);
        String message = String.format(
            resources.getString("guerrillaSwindled" + Compute.randomInt(25) + ".text"),
                campaign.getCommanderAddress(true),
            enemyFactionReference);

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
        JButton confirmButton = new JButton(resources.getString("logisticsDestroyed.text"));
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
