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

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * @deprecated Unused
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class MissionEndPrisonerDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a MissionEndPrisonerDialog with the specified parameters.
     *
     * @param campaign    The current campaign instance.
     * @param ransom      The ransom amount associated with the prisoners.
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     */
    public MissionEndPrisonerDialog(Campaign campaign, Money ransom, boolean isAllied, boolean isSuccess,
          boolean isGoodEvent) {
        super(campaign,
              getSpeaker(campaign),
              null,
              createInCharacterMessage(campaign, ransom, isAllied, isSuccess, isGoodEvent),
              createButtons(isAllied, isSuccess, isGoodEvent),
              createOutOfCharacterMessage(isAllied, isSuccess, isGoodEvent),
              null,
              false,
              null,
              null,
              true);
    }

    /**
     * Builds the list of buttons to be displayed in the dialog based on the parameters.
     *
     * <p>
     * The available buttons depend on the ownership (allied/enemy), mission outcome, and the type of event. Buttons may
     * include options such as "Accept Ransom", "Decline Ransom", "Release Prisoners", and "Execute Prisoners".
     * </p>
     *
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     *
     * @return A list of button-label and tooltip pairs to be displayed on the dialog.
     */
    private static List<ButtonLabelTooltipPair> createButtons(boolean isAllied, boolean isSuccess,
          boolean isGoodEvent) {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        boolean isRansom = (!isAllied && isSuccess && isGoodEvent) ||
                                 (!isAllied && !isSuccess && isGoodEvent) ||
                                 (isAllied && !isSuccess && isGoodEvent);

        if (isRansom) {
            if (isAllied) {
                ButtonLabelTooltipPair btnDecline = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                      "decline.button"), null);
                buttons.add(btnDecline);
            }

            ButtonLabelTooltipPair btnAccept = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                  "accept.button"), null);
            buttons.add(btnAccept);
        }

        if (!isAllied) {
            ButtonLabelTooltipPair btnReleaseThem = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                  "releaseThem.button"), null);
            buttons.add(btnReleaseThem);

            ButtonLabelTooltipPair btnExecuteThem = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                  "executeThem.button"), null);
            buttons.add(btnExecuteThem);
        }

        if (isAllied && !isRansom) {
            ButtonLabelTooltipPair btnConfirmation = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
                  "successful.button"), null);
            buttons.add(btnConfirmation);
        }

        return buttons;
    }

    /**
     * Generates the in-character message for the dialog based on mission details.
     *
     * <p>
     * The message reflects the outcome of the mission, the type of event, and whether the prisoners are allied or
     * enemies. It also includes the ransom amount and a commander-specific greeting or address.
     * </p>
     *
     * @param campaign    The current campaign instance.
     * @param ransomSum   The ransom amount.
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     *
     * @return A formatted string containing the in-character dialog message.
     */
    private static String createInCharacterMessage(Campaign campaign, Money ransomSum, boolean isAllied,
          boolean isSuccess, boolean isGoodEvent) {
        String key = "prisoners." +
                           (isAllied ? "player" : "enemy") +
                           '.' +
                           (isSuccess ? "victory" : "defeat") +
                           '.' +
                           (isGoodEvent ? "good" : "bad") +
                           '.' +
                           randomInt(50);

        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, key, commanderAddress, ransomSum.toAmountAndSymbolString());
    }

    /**
     * Retrieves the speaker for the dialog, typically the senior commanding officer.
     *
     * @param campaign The current campaign instance.
     *
     * @return The person to be displayed as the speaker in the dialog.
     */
    private static Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    /**
     * Creates an optional out-of-character (OOC) message based on mission details.
     *
     * <p>
     * This message provides additional context or instructions for the user regarding the prisoners. The message is
     * only displayed in specific scenarios.
     * </p>
     *
     * @param isAllied    Indicates whether the prisoners are allied.
     * @param isSuccess   Indicates whether the mission was successful.
     * @param isGoodEvent Indicates whether the event is positive.
     *
     * @return A formatted string containing the OOC dialog message, or {@code null} if no message is required.
     */
    private static @Nullable String createOutOfCharacterMessage(boolean isAllied, boolean isSuccess,
          boolean isGoodEvent) {
        boolean showMessage = (isAllied && !isSuccess && isGoodEvent);

        if (showMessage) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "prisoners.ransom.ooc");
        } else {
            return null;
        }
    }
}
