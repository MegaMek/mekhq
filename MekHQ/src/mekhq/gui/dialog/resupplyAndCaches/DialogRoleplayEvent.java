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
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;

/**
 * @deprecated unused
 */
@Deprecated(since = "0.05.06", forRemoval = true)
public class DialogRoleplayEvent extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Displays a roleplay event dialog for a player convoy. The dialog is used to present messages related to narrative
     * events that occur during convoy missions, accompanied by speaker details for immersion.
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
     * @param campaign     the {@link Campaign} instance, providing context for accessing relevant personnel and dynamic
     *                     game data like the player commander address.
     * @param playerConvoy the {@link Formation} instance representing the player's convoy. This is used to retrieve the
     *                     force commander and the convoy's name if no commander is available.
     * @param eventText    the narrative text describing the roleplay event. This string may include formatting
     *                     placeholders ({@code %s}) to dynamically incorporate campaign-specific details.
     */
    public DialogRoleplayEvent(Campaign campaign, Formation playerConvoy, String eventText) {
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
        UUID speakerId = playerConvoy.getFormationCommanderID();
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

        JLabel rightDescription = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s</div></html>",
              RIGHT_WIDTH,
              eventText));
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
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "convoyConfirm.text"));
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
