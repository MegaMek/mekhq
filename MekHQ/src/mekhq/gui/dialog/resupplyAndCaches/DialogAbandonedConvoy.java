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
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * This class provides a utility method to display a custom dialog related to abandoned convoys
 * in the MekHQ game. The dialog includes detailed information and visuals, like the convoy
 * commander or speaker, a status update message, and employer details.
 */
public class DialogAbandonedConvoy {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog to inform the player about an abandoned convoy.
     * <p>
     * This method constructs and shows a dialog that:
     * - Fetches title and content from a resource bundle.
     * - Retrieves speaker information based on the convoy's force commander or default context.
     * - Displays a status message formatted with current game details.
     * - Includes buttons to handle user confirmation.
     *
     * @param campaign    the current {@link Campaign} object representing the player's campaign.
     * @param contract    the {@link AtBContract} object representing the active contract containing
     *                    details about the employer and mission.
     * @param targetConvoy an optional {@link Force} object representing the convoy's details.
     *                     If null, the dialog uses default values for the speaker and visuals.
     */
    public static void abandonedConvoyDialog(Campaign campaign, AtBContract contract,
                                             @Nullable Force targetConvoy) {
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Establish the speaker
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

        // An ImageIcon to hold the faction icon
        ImageIcon speakerIcon;
        if (targetConvoy == null) {
            speakerIcon = getFactionLogo(campaign, contract.getEmployerCode(),
                true);
        } else {
            speakerIcon = getSpeakerIcon(campaign, speaker);
        }

        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);

        // Create and display the message
        String message = String.format(
            resources.getString("statusUpdateAbandoned" + randomInt(20) + ".text"),
            campaign.getCommanderAddress(false));

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

        // Create the buttons and add their action listeners.
        JButton optionConfirm = new JButton(resources.getString("logisticsDestroyed.text"));
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
