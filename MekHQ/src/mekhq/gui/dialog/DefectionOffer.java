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
package mekhq.gui.dialog;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Represents a dialog created when a prisoner wants to defect to the player's force.
 *
 * <p>This dialog is displayed to inform the player about the defection offer and provide
 * immersive narrative description about the defection. Depending on whether the prisoner is a
 * standard defector or a bondsman (specific to Clan campaigns and personnel), the dialog
 * customizes the in-character and out-of-character messages for the player.</p>
 */
public class DefectionOffer extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to handle a defection offer from a prisoner.
     *
     * @param campaign    The current campaign instance, which provides the context for the dialog.
     * @param defector    The prisoner making the defection offer.
     * @param isBondsman  {@code true} if the defector is a bondsman (Clan-specific role),
     *                    {@code false} for a standard defector.
     */
    public DefectionOffer(Campaign campaign, Person defector, boolean isBondsman) {
        super(campaign, campaign.getSeniorAdminPerson(HR), null, createInCharacterMessage(campaign,
                defector, isBondsman), createButtons(), createOutOfCharacterMessage(isBondsman),
            null, false, null, true);
    }

    /**
     * Generates the list of buttons to display in the dialog.
     *
     * <p>The dialog includes a single button to acknowledge the defection offer, labeled "Understood".</p>
     *
     * @return A list containing a single button with an appropriate label.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnDefectorUnderstood = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, "understood.button"), null);

        return List.of(btnDefectorUnderstood);
    }

    /**
     * Generates the in-character message for the dialog based on the defection offer.
     *
     * <p>This message customizes its narrative based on the type of defector
     * (standard or bondsman). It provides details about the prisoner, their
     * origin faction, and their offer to defect, addressing the player by their
     * in-game title.</p>
     *
     * @param campaign   The current campaign instance, which provides context data for the message.
     * @param defector   The prisoner making the defection offer.
     * @param isBondsman {@code true} if the defector is a bondsman, {@code false} otherwise.
     * @return A formatted string containing the immersive in-character message for the player.
     */
    private static String createInCharacterMessage(Campaign campaign, Person defector, boolean isBondsman) {
        String typeKey = isBondsman ? "bondsman" : "defector";
        String commanderAddress = campaign.getCommanderAddress(false);

        if (isBondsman) {
            String originFaction = defector.getOriginFaction().getFullName(campaign.getGameYear());

            if (!originFaction.contains("Clan")) {
                originFaction = "The " + originFaction;
            }
            return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".message",
                commanderAddress, defector.getFullName(), originFaction, defector.getFirstName());
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".message",
            commanderAddress, defector.getFullName(),
            defector.getOriginFaction().getFullName(campaign.getGameYear()));
    }

    /**
     * Generates the out-of-character (OOC) message for the defection offer.
     *
     * <p>The OOC message explains additional gameplay or narrative context about the defection,
     * depending on whether the prisoner is a standard defector or a bondsman.</p>
     *
     * @param isBondsman {@code true} if the defection involves a bondsman, {@code false} otherwise.
     * @return A formatted string containing the out-of-character message for the player.
     */
    private static String createOutOfCharacterMessage(boolean isBondsman) {
        String typeKey = isBondsman ? "bondsman" : "defector";
        return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".ooc");
    }
}
