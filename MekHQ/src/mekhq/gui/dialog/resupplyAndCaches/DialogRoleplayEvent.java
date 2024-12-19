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
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogRoleplayEvent} class handles the creation and display of roleplay event dialogs
 * for convoy missions in MekHQ. These dialogs provide narrative elements to enhance the immersion
 * of convoy missions by using dynamic content, player convoy details, and localized text.
 */
public class DialogRoleplayEvent {
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
    public static void dialogConvoyRoleplayEvent(Campaign campaign, Force playerConvoy, String eventText) {
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Establish the speaker
        UUID speakerID = playerConvoy.getForceCommanderID();
        Person speaker = campaign.getPerson(speakerID);

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            speakerName = playerConvoy.getName();
        }

        // An ImageIcon to hold the faction icon
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);

        // Create and display the message
        String message = String.format(eventText, campaign.getCommanderAddress(false));

        // Create a panel to display the icon and the message
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), speakerName)));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Create a panel to display the icon and the message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(speakerIcon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(descriptionPanel, BorderLayout.SOUTH);

        // Create the button and add their action listeners.
        JButton optionConfirm = new JButton(resources.getString("convoyConfirm.text"));
        optionConfirm.addActionListener(evt -> dialog.dispose());

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(optionConfirm);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
