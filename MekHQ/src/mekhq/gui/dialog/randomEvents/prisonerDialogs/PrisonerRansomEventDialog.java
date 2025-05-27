/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * @deprecated Unused
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class PrisonerRansomEventDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to present a ransom offer for prisoners.
     *
     * @param campaign       The current campaign context, which provides relevant information for the dialog.
     * @param prisoners      The list of prisoners involved in the ransom offer.
     * @param payment        The ransom payment amount offered in exchange for the prisoners.
     * @param isFriendlyPOWs {@code true} if the ransom offer is for friendly personnel captured by the enemy,
     *                       {@code false} if the offer involves enemy prisoners held by the player.
     */
    public PrisonerRansomEventDialog(Campaign campaign, List<Person> prisoners, Money payment, boolean isFriendlyPOWs) {
        super(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              createInCharacterMessage(campaign, payment, prisoners, isFriendlyPOWs),
              createButtons(),
              createOutOfCharacterMessage(isFriendlyPOWs),
              null,
              false,
              null,
              null,
              true);
    }

    /**
     * Generates the list of action buttons for the dialog.
     *
     * <p>The dialog offers two options for the player:
     * <ul>
     *     <li><b>Decline:</b> Rejects the ransom offer, keeping the current situation unchanged.</li>
     *     <li><b>Accept:</b> Agrees to the ransom terms, paying or receiving the ransom amount and
     *     resolving the situation accordingly.</li>
     * </ul>
     * </p>
     *
     * @return A list of buttons with appropriate labels for accepting or declining the ransom offer.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "decline.button"), null);
        ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "accept.button"), null);

        return List.of(btnDecline, btnAccept);
    }

    /**
     * Creates the immersive in-character message for the dialog.
     *
     * <p>The generated message is tailored to whether the offer involves friendly or enemy prisoners.
     * It includes details about the ransom amount, the list of prisoners, and addresses the player in their in-universe
     * title. The prisoners are listed in a structured table format for clarity and immersion.</p>
     *
     * @param campaign       The current campaign instance, from which player context and relevant information are
     *                       derived.
     * @param payment        The ransom amount offered for the prisoners.
     * @param prisoners      The list of prisoners involved in the transaction.
     * @param isFriendlyPOWs {@code true} if the ransom is for friendly prisoners captured by the enemy, {@code false}
     *                       if it involves enemy prisoners held by the player.
     *
     * @return A formatted HTML string containing the narrative in-character message for the dialog.
     */
    private static String createInCharacterMessage(Campaign campaign, Money payment, List<Person> prisoners,
          boolean isFriendlyPOWs) {
        String commanderAddress = campaign.getCommanderAddress(false);
        StringBuilder message = new StringBuilder();
        String key = isFriendlyPOWs ? "pows" : "prisoners";
        message.append(getFormattedTextAt(RESOURCE_BUNDLE, key + ".message", commanderAddress, payment));

        // Create a table to hold the personnel
        message.append("<br><table style='width:100%; text-align:left;'>");

        for (int i = 0; i < prisoners.size(); i++) {
            if (i % 2 == 0) {
                message.append("<tr>");
            }

            // Add the person in a column
            Person person = prisoners.get(i);
            message.append("<td>- ").append(person.getHyperlinkedFullTitle()).append("</td>");

            if ((i + 1) % 2 == 0 || i == prisoners.size() - 1) {
                message.append("</tr>");
            }
        }

        message.append("</table>");
        return message.toString();
    }

    /**
     * Creates the optional out-of-character (OOC) message for the dialog.
     *
     * <p>The OOC message provides additional context, such as explaining the mechanics
     * of the ransom event or the consequences of accepting or declining the offer.</p>
     *
     * @param isFriendlyPOWs {@code true} if the ransom is for friendly prisoners, {@code false} if it is for enemy
     *                       prisoners.
     *
     * @return A formatted string containing the OOC message for further clarification.
     */
    private static String createOutOfCharacterMessage(boolean isFriendlyPOWs) {
        String key = isFriendlyPOWs ? "pows" : "prisoners";
        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ooc");
    }
}
