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
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.UUID;

import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;

/**
 * This class provides utility methods to display dialogs related to the beginning of a contract.
 * It generates user-friendly messages summarizing cargo requirements, player convoy capabilities,
 * and mission details.
 */
public class DialogContractStart {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    /**
     * Displays a dialog at the start of a contract, providing summarized details about the mission
     * and player convoy capabilities. The content is dynamically generated based on the given
     * {@link Campaign} and {@link AtBContract}.
     * <p>
     * This method:
     * - Generates a message summarizing the player's convoy capabilities and cargo capacity.
     * - Fetches localized text from the resource bundle based on the contract type and command rights.
     * - Displays a dialog with visuals (e.g., faction icon) and a confirmation button to proceed.
     *
     * @param campaign the current {@link Campaign}.
     * @param contract the active contract.
     */
    public static void contractStartDialog(Campaign campaign, AtBContract contract) {
        // Retrieves the title from the resources
        String title = resources.getString("dialog.title");

        // An ImageIcon to hold the faction icon
        ImageIcon icon = campaign.getCampaignFactionIcon();

        // Create a text pane to display the message
        String message = generateContractStartMessage(campaign, contract);
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText(message);
        textPane.setEditable(false);

        // Create a panel to display the icon and the message
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(icon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(textPane, BorderLayout.SOUTH);

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());

        // Create an accept button and add its action listener.
        JButton acceptButton = new JButton(resources.getString("convoyConfirm.text"));
        acceptButton.addActionListener(e -> dialog.dispose());

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(acceptButton);

        // Add the original panel and button panel to the dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Generates an HTML-formatted message to display in the start-of-contract dialog.
     * The message includes details such as
     * - Total player convoy cargo capacity.
     * - Number of operational player convoys.
     * - Cargo requirements for the contract.
     * <p>
     * The message format adapts based on the contract type (e.g., guerrilla warfare vs. general contract)
     * and the player's command rights (e.g., independent command).
     * <p>
     * This method:
     * - Iterates through all player forces to calculate total convoy cargo capacity.
     * - Checks for convoy readiness, excluding units that are damaged, uncrewed, or prohibited.
     * - Formats the message using localized templates from the resource bundle.
     *
     * @param campaign the current {@link Campaign}.
     * @param contract the current {@link AtBContract}.
     * @return an HTML-formatted string message summarizing the player's readiness and convoy details
     *         in the context of the contract.
     */
    private static String generateContractStartMessage(Campaign campaign, AtBContract contract) {
        int playerConvoys = 0;
        double totalPlayerCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.isConvoyForce()) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            if (force.isConvoyForce()) {
                boolean hasCargo = false;
                for (UUID unitId : force.getAllUnits(false)) {
                    try {
                        Unit unit = campaign.getUnit(unitId);
                        Entity entity = unit.getEntity();

                        if (unit.isDamaged()
                            || !unit.isFullyCrewed()
                            || isProhibitedUnitType(entity, true)) {
                            continue;
                        }

                        double individualCargo = unit.getCargoCapacity();

                        if (individualCargo > 0) {
                            hasCargo = true;
                        }

                        cargoCapacitySubTotal += individualCargo;
                    } catch (Exception ignored) {
                        // If we run into an exception, it's because we failed to get Unit or Entity.
                        // In either case, we just ignore that unit.
                    }
                }

                if (hasCargo) {
                    if (cargoCapacitySubTotal > 0) {
                        totalPlayerCargoCapacity += cargoCapacitySubTotal;
                        playerConvoys++;
                    }
                }
            }
        }

        String convoyMessage;
        String commanderTitle = campaign.getCommanderAddress(false);

        if (contract.getContractType().isGuerrillaWarfare()) {
            String convoyMessageTemplate = resources.getString("contractStartMessageGuerrilla.text");
            convoyMessage = String.format(convoyMessageTemplate, commanderTitle);
        } else {
            String convoyMessageTemplate = resources.getString("contractStartMessageGeneric.text");
            if (contract.getCommandRights().isIndependent()) {
                convoyMessageTemplate = resources.getString("contractStartMessageIndependent.text");
            }

            convoyMessage = String.format(convoyMessageTemplate, commanderTitle,
                estimateCargoRequirements(campaign, contract), totalPlayerCargoCapacity,
                playerConvoys, playerConvoys != 1 ? "s" : "");
        }

        int width = UIUtil.scaleForGUI(500);
        return String.format("<html><i><div style='width: %s; text-align:center;'>%s</div></i></html>",
            width, convoyMessage);
    }
}
