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
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogInterception} class is responsible for displaying a UI dialog when an
 * interception scenario occurs during a supply or convoy mission in MekHQ. The dialog uses
 * localized resources to generate the content dynamically and includes relevant visual elements
 * like the speaker's icon, title, and mission details.
 */
public class DialogInterception {
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
    public static void dialogInterception(Resupply resupply, @Nullable Force targetConvoy) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

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
            resources.getString("statusUpdateIntercepted" + randomInt(20) + ".text"),
            campaign.getCommanderAddress(false),
            resources.getString("interceptionInstructions.text"));

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
        JButton optionConfirm = new JButton(resources.getString("logisticsReceived.text"));
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
