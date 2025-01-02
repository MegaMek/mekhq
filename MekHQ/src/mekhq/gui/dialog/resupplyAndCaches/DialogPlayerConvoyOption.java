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
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogPlayerConvoyOption} class provides functionality to display a dialog
 * that prompts the player to choose whether to use their own convoy to transport cargo.
 * The dialog dynamically generates content, localized messages, and speaker visuals to
 * guide the player's decision.
 */
public class DialogPlayerConvoyOption {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog that allows the player to decide if they want to use their own convoy for resupply.
     * The dialog presents key information such as required cargo tonnage, available convoy capacity, and
     * the number of operational player convoys. When convoy usage is forced, the dialog reflects that with
     * a customized message and altered behavior.
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
     * @param resupply              the {@link Resupply} instance providing campaign context, cargo details,
     *                              and available player convoys.
     * @param forcedUseOfPlayerConvoy a boolean flag indicating whether the use of the player's convoy
     *                                is mandatory. If true, the optional decision is bypassed, and the
     *                                dialog displays a notice reflecting the forced choice.
     */
    public static void createPlayerConvoyOptionalDialog(Resupply resupply, boolean forcedUseOfPlayerConvoy) {
        final Campaign campaign = resupply.getCampaign();

        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Establish the speaker
        Person speaker = campaign.getSeniorAdminPerson(1);

        String speakerName;
        if (speaker != null) {
            speakerName = speaker.getFullTitle();
        } else {
            speakerName = campaign.getName();
        }

        // An ImageIcon to hold the faction icon
        ImageIcon speakerIcon = ResupplyDialogUtilities.getSpeakerIcon(campaign, speaker);
        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);

        // Create and display the message
        int playerConvoyCount = resupply.getPlayerConvoys().size();
        String pluralizer = playerConvoyCount != 1 ? "s" : "";
        String messageResource;

        if (forcedUseOfPlayerConvoy) {
            messageResource = resources.getString("usePlayerConvoyForced.text");
        } else {
            messageResource = resources.getString("usePlayerConvoyOptional.text");
        }

        String message = String.format(messageResource, campaign.getCommanderAddress(false),
            resupply.getTargetCargoTonnagePlayerConvoy(), resupply.getTotalPlayerCargoCapacity(),
            playerConvoyCount, pluralizer, pluralizer);

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

        // Create the buttons and add their action listener.
        JButton acceptButton = new JButton(resources.getString("confirmAccept.text"));
        acceptButton.addActionListener(e -> {
            dialog.dispose();
            resupply.setUsePlayerConvoy(true);
        });
        acceptButton.setEnabled(playerConvoyCount > 0);

        JButton refuseButton = new JButton(resources.getString("confirmRefuse.text"));
        refuseButton.addActionListener(e -> {
            dialog.dispose();
            resupply.setUsePlayerConvoy(false);
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);
        buttonPanel.add(refuseButton);

        // Add a WindowListener to handle the close operation
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                resupply.setUsePlayerConvoy(false);
            }
        });

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
