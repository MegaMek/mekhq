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
import java.util.ResourceBundle;

import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * The {@code DialogResupplyFocus} class is responsible for displaying a dialog that allows
 * the player to select their focus preference during a resupply operation. The player can
 * choose between a balanced approach, prioritizing armor, or prioritizing ammunition.
 * The dialog includes a speaker icon, a dynamically generated message, and actionable options.
 */
public class DialogResupplyFocus {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog to let the player select a resupply focus. The available options include:
     * <ul>
     *     <li><b>Balanced:</b> A default option with no specific prioritization.</li>
     *     <li><b>Armor Focus:</b> Prioritize replenishing armor.</li>
     *     <li><b>Ammo Focus:</b> Prioritize ammunition replenishment.</li>
     * </ul>
     * The player’s choice will dynamically adjust the resupply focus allocation in the {@link Resupply} object.
     *
     * <p>This method performs the following tasks:</p>
     * <ol>
     *     <li>Builds a {@link JDialog} with a localized title, speaker representation, and decision buttons.</li>
     *     <li>Generates a message describing the resupply decision options.</li>
     *     <li>Presents a dynamically chosen speaker from campaign logistics personnel, or a suitable fallback.</li>
     *     <li>Displays an icon representing the speaker, scaled to 100px wide for consistent presentation.</li>
     *     <li>Defines and attaches behaviors associated with each selection:
     *         <ul>
     *             <li><b>Balanced:</b> Applies the default allocation (i.e., no adjustable focus).</li>
     *             <li><b>Armor:</b> Sets 75% prioritization to armor resources, disabling focus on ammo and parts.</li>
     *             <li><b>Ammo:</b> Sets 75% prioritization to ammunition, disabling focus on armor and parts.</li>
     *         </ul>
     *     </li>
     *     <li>Fixes restrictions for game balance purposes by disallowing direct prioritization of parts.</li>
     *     <li>Enforces modal behavior to ensure that the player interacts with the dialog before proceeding.</li>
     * </ol>
     *
     * <p>The resupply focus is stored in the {@link Resupply} object after the dialog is completed.</p>
     *
     * @param resupply the {@link Resupply} instance containing campaign and logistical context.
     *                 The resupply focus preferences will be set within this object based on
     *                 the player's selection.
     */
    public static void createResupplyFocusDialog(Resupply resupply) {
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
        ImageIcon speakerIcon = getSpeakerIcon(campaign, speaker);
        speakerIcon = scaleImageIconToWidth(speakerIcon, 100);

        // Create and display the message
        String messageResource = resources.getString("focusDescription.text");
        String message = String.format(messageResource, campaign.getCommanderAddress(false));

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
        JButton optionBalanced = new JButton(resources.getString("optionBalanced.text"));
        optionBalanced.addActionListener(e -> {
            dialog.dispose();
            // The class initialization assumes a balanced approach
        });

        // The player should not be able to focus on parts for game balance reasons.
        // If the player could pick parts, the optimum choice would be to always pick parts.

        JButton optionArmor = new JButton(resources.getString("optionArmor.text"));
        optionArmor.addActionListener(e -> {
            dialog.dispose();
            resupply.setFocusAmmo(0);
            resupply.setFocusArmor(0.75);
            resupply.setFocusParts(0);
        });

        JButton optionAmmo = new JButton(resources.getString("optionAmmo.text"));
        optionAmmo.addActionListener(e -> {
            dialog.dispose();
            resupply.setFocusAmmo(0.75);
            resupply.setFocusArmor(0);
            resupply.setFocusParts(0);
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(optionBalanced);
        buttonPanel.add(optionArmor);
        buttonPanel.add(optionAmmo);

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
