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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Dialog class providing the user with options regarding the resupply convoy.
 *
 * <p>Invokes an immersive dialog to prompt the player for a choice and records the selected response type.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class ResupplyConvoyChoice {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ResupplyConvoyChoice";

    /** Type of response chosen in the dialog. */
    private final ConvoyResponseType responseType;

    /**
     * Enum representing the possible responses to the resupply convoy dialog.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public enum ConvoyResponseType {
        /** Cancel the operation. */
        CANCEL,
        /** Utilize an NPC convoy. */
        NPC,
        /** Utilize a player convoy. */
        PLAYER
    }

    /**
     * Gets the response type selected by the user in the dialog.
     *
     * @return {@link ConvoyResponseType} representing the selected option.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ConvoyResponseType getResponseType() {
        return responseType;
    }

    /**
     * Constructs a {@link ResupplyConvoyChoice}, displaying the dialog and storing the user's selection.
     *
     * @param campaign             the campaign context in which this occurs
     * @param isForcedPlayerConvoy {@code true} if the convoy must be player-owned
     * @param enhancedTonnage      enhanced convoy tonnage value (return value of
     *                             {@link Resupply#getTargetCargoTonnagePlayerConvoy})
     * @param normalTonnage        normal convoy tonnage value (return value of {@link Resupply#getTargetCargoTonnage})
     * @param availableCargoSpace  currently available cargo space in player convoys (return value of
     *                             {@link Resupply#getTotalPlayerCargoCapacity})
     * @param moraleString         string label for the contract's current {@link AtBMoraleLevel}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public ResupplyConvoyChoice(Campaign campaign, boolean isForcedPlayerConvoy, int enhancedTonnage,
          int normalTonnage, double availableCargoSpace, String moraleString) {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.TRANSPORT),
              null,
              getInCharacterMessage(isForcedPlayerConvoy, campaign.getCommanderAddress(), enhancedTonnage,
                    normalTonnage, availableCargoSpace),
              getButtons(isForcedPlayerConvoy),
              getOutOfCharacterMessage(isForcedPlayerConvoy, moraleString),
              null,
              true);

        responseType = switch (dialog.getDialogChoice()) {
            case 0 -> ConvoyResponseType.CANCEL;
            case 1 -> ConvoyResponseType.PLAYER;
            case 2 -> ConvoyResponseType.NPC;
            default -> throw new IllegalStateException("Unexpected value: " + dialog.getDialogChoice());
        };
    }

    /**
     * Returns the in-character message to be displayed in the dialog, formatted with the provided data.
     *
     * @param isForcedPlayerConvoy whether the convoy is forced to be a player convoy
     * @param commanderAddress     the address or name of the commander
     * @param enhancedTonnage      the enhanced tonnage to display
     * @param normalTonnage        the normal tonnage to display
     * @param availableCargoSpace  the available cargo space
     *
     * @return formatted in-character dialog message
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getInCharacterMessage(boolean isForcedPlayerConvoy, String commanderAddress, int enhancedTonnage,
          int normalTonnage, double availableCargoSpace) {
        String key;

        if (isForcedPlayerConvoy) {
            key = "ResupplyConvoyChoice.inCharacter.forced";
        } else {
            key = "ResupplyConvoyChoice.inCharacter.normal";
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress, enhancedTonnage, normalTonnage,
              availableCargoSpace);
    }

    /**
     * Generates a list of button text strings for the convoy choice dialog, depending on campaign context.
     *
     * @param isForcedPlayerConvoy whether a player convoy is required
     *
     * @return a list of button label strings for the dialog
     *
     * @author Illiani
     * @since 0.50.07
     */
    private List<String> getButtons(boolean isForcedPlayerConvoy) {
        List<String> buttons = new ArrayList<>();

        String cancel = getTextAt(RESOURCE_BUNDLE, "ResupplyConvoyChoice.button.cancel");
        buttons.add(cancel);

        String player = getTextAt(RESOURCE_BUNDLE, "ResupplyConvoyChoice.button.player");
        buttons.add(player);

        if (!isForcedPlayerConvoy) {
            String npc = getTextAt(RESOURCE_BUNDLE, "ResupplyConvoyChoice.button.npc");
            buttons.add(npc);
        }

        return buttons;
    }

    /**
     * Gets the out-of-character message for the dialog, using the provided morale string.
     *
     * @param isForcedPlayerConvoy whether a player convoy is required
     * @param moraleString         the morale string to be included in the message
     *
     * @return formatted out-of-character message
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getOutOfCharacterMessage(boolean isForcedPlayerConvoy, String moraleString) {
        String key;

        if (isForcedPlayerConvoy) {
            key = "ResupplyConvoyChoice.outOfCharacter.forced";
        } else {
            key = "ResupplyConvoyChoice.outOfCharacter.normal";
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, key, moraleString);
    }
}
