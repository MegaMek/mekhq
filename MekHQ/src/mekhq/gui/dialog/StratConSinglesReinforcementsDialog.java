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
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * Displays a confirmation dialog for deploying reinforcements in StratCon Single Drop scenarios.
 *
 * <p>This dialog presents the commander’s message, offers Cancel and Confirm options, and returns a
 * {@link StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType} based on the player’s choice. It is
 * used when the GM needs to instantly approve reinforcements during a Single Drop scenario, where all reinforcements
 * are free and automatic.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class StratConSinglesReinforcementsDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConSinglesReinforcementsDialog";

    private final StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType reinforcementDialogResponseType;

    /**
     * Returns the response selected by the user.
     *
     * <p>The response corresponds directly to the button clicked in the dialog:</p>
     * <ul>
     *   <li>{@code CANCEL} – user chose to abort</li>
     *   <li>{@code REINFORCE_GM_INSTANTLY} – user confirmed deployment</li>
     * </ul>
     * </p>
     *
     * @return the chosen {@link StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType}
     *
     * @author Illiani
     * @since 0.50.10
     */
    public StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType getResponseType() {
        return reinforcementDialogResponseType;
    }

    public StratConSinglesReinforcementsDialog(Campaign campaign) {
        final String commanderAddress = campaign.getCommanderAddress();

        ImmersiveDialogCore dialog = new ImmersiveDialogCore(campaign,
              campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "StratConSinglesReinforcementsDialog.centerMessage",
                    commanderAddress),
              getButtons(),
              getTextAt(RESOURCE_BUNDLE, "StratConSinglesReinforcementsDialog.bottomMessage"),
              null,
              false,
              null,
              null,
              true);

        reinforcementDialogResponseType = switch (dialog.getDialogChoice()) {
            case 0 -> StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType.CANCEL;
            case 1 -> StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType.REINFORCE_GM_INSTANTLY;
            default -> throw new IllegalStateException("Unexpected dialog choice value: "
                                                             +
                                                             dialog.getDialogChoice()
                                                             +
                                                             ". Valid choices are 0-1. This may occur if an invalid " +
                                                             "dialog choice is returned.");
        };
    }

    private List<ImmersiveDialogCore.ButtonLabelTooltipPair> getButtons() {
        List<ImmersiveDialogCore.ButtonLabelTooltipPair> buttons = new ArrayList<>();

        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Cancel.text"), null));
        buttons.add(new ImmersiveDialogCore.ButtonLabelTooltipPair(getText("Confirm.text"), null));

        return buttons;
    }
}
