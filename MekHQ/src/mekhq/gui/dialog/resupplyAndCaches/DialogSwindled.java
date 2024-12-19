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
public class DialogSwindled {
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
    public static void swindledDialog(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();

        // Dialog dimensions and representative
        final int DIALOG_WIDTH = UIUtil.scaleForGUI(400);

        // Creates and sets up the dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());

        // Prepares and adds the icon of the representative as a label
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(JLabel.CENTER);

        ImageIcon factionLogo = getFactionLogo(campaign, "PIR", true);
        factionLogo = scaleImageIconToWidth(factionLogo, 100);
        iconLabel.setIcon(factionLogo);
        dialog.add(iconLabel, BorderLayout.NORTH);

        // Prepares and adds the description
        String enemyFactionReference = getEnemyFactionReference(resupply);

        String message = String.format(
            resources.getString("guerrillaSwindled" + Compute.randomInt(25) + ".text"),
            campaign.getCommanderAddress(true), enemyFactionReference);

        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(DIALOG_WIDTH), message));
        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
            String.format(resources.getString("dialogBorderTitle.text"), "")));
        descriptionPanel.add(description);
        dialog.add(descriptionPanel, BorderLayout.CENTER);

        // Prepares and adds the confirm button
        JButton confirmButton = new JButton(resources.getString("logisticsDestroyed.text"));
        confirmButton.addActionListener(e -> dialog.dispose());
        dialog.add(confirmButton,  BorderLayout.SOUTH);

        // Pack, position and display the dialog
        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
