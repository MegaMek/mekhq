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

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerDescription;
import static mekhq.gui.baseComponents.MHQDialogImmersive.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * The {@code DialogResupplyFocus} class is responsible for displaying a dialog that allows
 * the player to select their focus preference during a resupply operation. The player can
 * choose between a balanced approach, prioritizing armor, or prioritizing ammunition.
 * The dialog includes a speaker icon, a dynamically generated message, and actionable options.
 */
public class DialogResupplyFocus extends JDialog {
    final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
    final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);
    final int INSERT_SIZE = UIUtil.scaleForGUI(10);

    private static final String  RESOURCE_BUNDLE = "mekhq.resources.Resupply";

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
    public DialogResupplyFocus(Resupply resupply) {
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
            speakerIcon = scaleImageIconToWidth(speakerIcon, 100);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, speaker, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH, speakerDescription));
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

        String message = getFormattedTextAt(RESOURCE_BUNDLE, "focusDescription.text",
            campaign.getCommanderAddress(false));

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
        JButton optionBalanced = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "optionBalanced.text"));
        optionBalanced.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "optionBalanced.tooltip"));
        optionBalanced.addActionListener(e -> {
            dispose();
            // The Resupply class initialization assumes a balanced approach
        });
        buttonPanel.add(optionBalanced);

        // The player should not be able to focus on parts for game balance reasons.
        // If the player could pick parts, the optimum choice would be to always pick parts.

        JButton optionArmor = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "optionArmor.text"));
        optionArmor.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "optionArmor.tooltip"));
        optionArmor.addActionListener(e -> {
            dispose();
            resupply.setFocusAmmo(0);
            resupply.setFocusArmor(0.75);
            resupply.setFocusParts(0);
        });
        buttonPanel.add(optionArmor);

        JButton optionAmmo = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "optionAmmo.text"));
        optionAmmo.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE, "optionAmmo.tooltip"));
        optionAmmo.addActionListener(e -> {
            dispose();
            resupply.setFocusAmmo(0.75);
            resupply.setFocusArmor(0);
            resupply.setFocusParts(0);
        });
        buttonPanel.add(optionAmmo);

        // Add the button panel to the container
        containerPanel.add(buttonPanel);

        // New panel (to be added below the button panel)
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel lblInfo = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
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
