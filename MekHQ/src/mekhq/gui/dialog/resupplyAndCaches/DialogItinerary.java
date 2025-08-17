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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.resupplyAndCaches;

import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static megamek.common.compute.Compute.randomInt;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.finances.enums.TransactionType.EQUIPMENT_PURCHASE;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.loadPlayerConvoys;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.makeDelivery;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.makeSmugglerDelivery;
import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.processConvoy;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_CONTRACT_END;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_LOOT;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_SMUGGLER;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.getSpeakerIcon;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.createPartsReport;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.formatColumnData;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getEnemyFactionReference;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * The {@code DialogItinerary} class generates and displays dialogs related to resupply operations. These include normal
 * resupply, looting, contract-ending resupply, and smuggler-related resupplies.
 */
public class DialogItinerary {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Displays a detailed itinerary dialog based on the type of resupply operation. The dialog provides information
     * such as convoy contents, supply values, roleplay items, speaker details, and visual assets. It also includes
     * appropriate action buttons to handle confirmation, refusal, or delivery of supplies, depending on the operation
     * type.
     *
     * <p>This method performs the following tasks:</p>
     * <ol>
     *     <li>Retrieves localized text and speaker information based on the resupply type.</li>
     *     <li>Generates a dynamic description message, including a formatted table of convoy contents.</li>
     *     <li>Builds a GUI using Swing, including visual assets like speaker icons and HTML-formatted text for details.</li>
     *     <li>Provides buttons with action listeners for confirmation, refusal, or receipt acknowledgment.</li>
     *     <li>Executes specific follow-up logic, such as resupply delivery or updating campaign finances, based on the
     *         user's choice and the resupply type.</li>
     * </ol>
     *
     * @param resupply the {@link Resupply} instance, which contains details about the resupply operation, including the
     *                 campaign context, contract, convoy details, and resupply type.
     */
    public static void itineraryDialog(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();
        final ResupplyType resupplyType = resupply.getResupplyType();

        final int DIALOG_WIDTH = UIUtil.scaleForGUI(700);

        // Retrieves the title from the resources
        String title = getFormattedTextAt(RESOURCE_BUNDLE, "dialog.title");

        // Create a custom dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Establish the speaker
        Person speaker;
        String speakerName;
        ImageIcon speakerIcon;

        if (resupplyType.equals(RESUPPLY_LOOT) || resupplyType.equals(RESUPPLY_CONTRACT_END)) {
            speaker = campaign.getSeniorAdminPerson(AdministratorSpecialization.LOGISTICS);

            if (speaker != null) {
                speakerName = speaker.getFullTitle();
            } else {
                speakerName = campaign.getName();
            }

            speakerIcon = getSpeakerIcon(campaign, speaker);
            speakerIcon = scaleImageIcon(speakerIcon, 100, true);
        } else if (resupplyType.equals(RESUPPLY_SMUGGLER)) {
            speakerName = getFormattedTextAt(RESOURCE_BUNDLE, "guerrillaSpeaker.text");

            speakerIcon = getFactionLogo(campaign.getGameYear(), "PIR");
            speakerIcon = scaleImageIcon(speakerIcon, 200, true);
        } else {
            speakerName = contract.getEmployerName(campaign.getGameYear());

            speakerIcon = getFactionLogo(campaign.getGameYear(), contract.getEmployerCode());
            speakerIcon = scaleImageIcon(speakerIcon, 200, true);
        }

        StringBuilder message = new StringBuilder(getInitialDescription(resupply));

        List<String> partsReport = createPartsReport(resupply);
        if (!partsReport.isEmpty()) {
            if (!resupplyType.equals(RESUPPLY_LOOT) && !resupplyType.equals(RESUPPLY_CONTRACT_END)) {
                generateRoleplayItems(campaign, partsReport);
            }
        }

        String[] columns = formatColumnData(partsReport);

        message.append("<table><tr valign='top'>")
              .append("<td>")
              .append(columns[0])
              .append("</td>")
              .append("<td>")
              .append(columns[1])
              .append("</td>")
              .append("<td>")
              .append(columns[2])
              .append("</td>")
              .append("</tr></table>");

        // Create a panel to display the icon and the message
        JLabel description = new JLabel(String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
              UIUtil.scaleForGUI(DIALOG_WIDTH),
              message));
        description.setHorizontalAlignment(JLabel.CENTER);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(getFormattedTextAt(RESOURCE_BUNDLE,
              "dialogBorderTitle.text",
              speakerName)));
        descriptionPanel.add(description);

        // Create the main panel to hold the description and image
        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(speakerIcon);
        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(descriptionPanel, BorderLayout.SOUTH);

        // Wrap the main content panel (panel) in a JScrollPane for scrolling
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Create the buttons and add their action listeners
        JButton confirmButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "confirmAccept.text"));
        confirmButton.addActionListener(e -> {
            dialog.dispose();
            campaign.getFinances()
                  .debit(EQUIPMENT_PURCHASE,
                        campaign.getLocalDate(),
                        resupply.getConvoyContentsValueCalculated(),
                        getFormattedTextAt(RESOURCE_BUNDLE, "smugglerFee.text"));

            if (resupplyType.equals(RESUPPLY_SMUGGLER)) {
                makeSmugglerDelivery(resupply);
            } else {
                if (resupply.getUsePlayerConvoy()) {
                    loadPlayerConvoys(resupply);

                    final List<Part> convoyContents = resupply.getConvoyContents();
                    if (!convoyContents.isEmpty()) {
                        campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "convoyInsufficientSize.text"));

                        for (Part part : convoyContents) {
                            campaign.addReport("- " + part.getName());
                        }
                    }
                } else {
                    processConvoy(resupply, resupply.getConvoyContents(), null);
                }
            }
        });

        JButton refuseButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "confirmRefuse.text"));
        refuseButton.addActionListener(evt -> dialog.dispose());

        JButton okButton = new JButton(getFormattedTextAt(RESOURCE_BUNDLE, "confirmReceipt.text"));
        okButton.addActionListener(evt -> {
            dialog.dispose();
            makeDelivery(resupply, null);
        });

        // Create a panel for buttons and add buttons to it
        JPanel buttonPanel = new JPanel();

        switch (resupplyType) {
            case RESUPPLY_NORMAL, RESUPPLY_SMUGGLER, RESUPPLY_CONTRACT_END -> {
                buttonPanel.add(confirmButton);
                buttonPanel.add(refuseButton);
            }
            case RESUPPLY_LOOT -> buttonPanel.add(okButton);
        }

        // Create a new panel to show additional information below the button panel
        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel lblInfo = new JLabel(String.format(
              "<html><div style='width: %s; text-align:center;'>%s<br>%s</div></html>",
              DIALOG_WIDTH,
              getFormattedTextAt(RESOURCE_BUNDLE, "roleplayItems.prompt"),
              getFormattedTextAt(RESOURCE_BUNDLE, "outOfCharacter.itinerary")));
        infoPanel.add(lblInfo);

        // Create a container panel to hold both buttonPanel and infoPanel
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS)); // Stack them vertically
        southPanel.add(buttonPanel);
        southPanel.add(infoPanel);

        // Add the scroll pane for content and south panel to the dialog
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(southPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Generates roleplay item descriptions based on active personnel and their roles in the campaign. The roleplay
     * items include items such as ration packs and medical supplies tailored to the personnel's roles. Additionally,
     * randomized content is added to increase immersion and variety.
     *
     * <p>This method processes:</p>
     * <ul>
     *     <li>Combat personnel to determine the number of ration packs required.</li>
     *     <li>Medical personnel to determine the need for medical supplies.</li>
     *     <li>Randomized flavor text for additional roleplay items with no tangible in-game effect.</li>
     * </ul>
     *
     * @param campaign    the {@link Campaign} to retrieve active personnel and their roles.
     * @param partsReport the list of strings to which new descriptive content (roleplay items) is appended.
     */
    private static void generateRoleplayItems(Campaign campaign, List<String> partsReport) {
        int rationPacks = 0;
        int medicalSupplies = 0;

        for (Person person : campaign.getActivePersonnel(true)) {
            PersonnelRole primaryRole = person.getPrimaryRole();
            PersonnelRole secondaryRole = person.getSecondaryRole();

            if (primaryRole.isCombat() || secondaryRole.isCombat()) {
                rationPacks++;
            }

            if (primaryRole.isDoctor() || secondaryRole.isDoctor()) {
                medicalSupplies++;
            }
        }

        rationPacks *= (int) Math.ceil((double) campaign.getLocalDate().lengthOfMonth() / 4);

        // These are all roleplay items that have no tangible benefit
        if (rationPacks > 0) {
            partsReport.add("<i>" +
                                  getFormattedTextAt(RESOURCE_BUNDLE, "resourcesRations.text") +
                                  " x" +
                                  rationPacks +
                                  "</i>");
        }

        if (medicalSupplies > 0) {
            partsReport.add("<i>" +
                                  getFormattedTextAt(RESOURCE_BUNDLE, "resourcesMedical.text") +
                                  " x" +
                                  medicalSupplies +
                                  "</i>");
        }

        partsReport.add("<i>" +
                              getFormattedTextAt(RESOURCE_BUNDLE, "resourcesRoleplay" + randomInt(50) + ".text") +
                              " x" +
                              (randomInt((int) Math.ceil((double) rationPacks / 5)) + 1) +
                              "</i>");
    }

    /**
     * Constructs the initial description of the resupply, tailored to the type of resupply event.
     *
     * <p>This method:</p>
     * <ul>
     *     <li>Uses switch expressions to customize the output for different resupply types:
     *         <ul>
     *             <li><b>RESUPPLY_NORMAL</b>: Includes morale-based flavor text and full supply cost details.</li>
     *             <li><b>RESUPPLY_LOOT</b>: Generates text regarding salvaged supplies and their value.</li>
     *             <li><b>RESUPPLY_CONTRACT_END</b>: Details the loot acquired at the end of the contract.</li>
     *             <li><b>RESUPPLY_SMUGGLER</b>: Includes guerrilla flavor text, enemy faction info,
     *                 and adjusted supply costs.</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param resupply the {@link Resupply} object containing context such as resupply type, convoy contents, and
     *                 related mission details.
     *
     * @return a string containing an HTML-formatted description that includes supply costs, salvage details, or
     *       guerrilla interactions, depending on the {@link ResupplyType}.
     */
    private static String getInitialDescription(Resupply resupply) {
        final Campaign campaign = resupply.getCampaign();
        final ResupplyType resupplyType = resupply.getResupplyType();

        return switch (resupplyType) {
            case RESUPPLY_NORMAL -> {
                AtBContract contract = resupply.getContract();
                AtBMoraleLevel morale = contract.getMoraleLevel();

                yield getFormattedTextAt(RESOURCE_BUNDLE,
                      morale.toString().toLowerCase() + "Supplies" + randomInt(20) + ".text",
                      getFormattedTextAt(RESOURCE_BUNDLE,
                            "supplyCostFull.text",
                            resupply.getConvoyContentsValueCalculated().toAmountAndSymbolString(),
                            resupply.getConvoyContentsValueBase().toAmountAndSymbolString()));
            }
            case RESUPPLY_LOOT -> getFormattedTextAt(RESOURCE_BUNDLE,
                  "salvaged" + randomInt(10) + ".text",
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "supplyCostAbridged.text",
                        resupply.getConvoyContentsValueBase().toAmountAndSymbolString()));
            case RESUPPLY_CONTRACT_END -> getFormattedTextAt(RESOURCE_BUNDLE,
                  "looted" + randomInt(10) + ".text",
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "supplyCostAbridged.text",
                        resupply.getConvoyContentsValueBase().toAmountAndSymbolString()));
            case RESUPPLY_SMUGGLER -> {
                String value = getFormattedTextAt(RESOURCE_BUNDLE,
                      "supplyCostFull.text",
                      resupply.getConvoyContentsValueCalculated().toAmountAndSymbolString(),
                      resupply.getConvoyContentsValueBase().toAmountAndSymbolString());

                yield getFormattedTextAt(RESOURCE_BUNDLE,
                      "guerrillaSupplies" + randomInt(25) + ".text",
                      campaign.getCommanderAddress(true),
                      getEnemyFactionReference(resupply),
                      resupply.getConvoyContentsValueCalculated().toAmountAndSymbolString(),
                      value);
            }
        };
    }
}
