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
package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;

/**
 * @deprecated Unused
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class PrisonerWarningResultsDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to display the results of resolving high Prisoner Capacity.
     *
     * @param campaign  The current campaign instance, providing context for the dialog.
     * @param speaker   The person acting as the in-universe speaker for the event, or {@code null} if no speaker is
     *                  present.
     * @param isExecute {@code true} if the player chose to execute prisoners, {@code false} if the player opted to
     *                  release them.
     */
    public PrisonerWarningResultsDialog(Campaign campaign, @Nullable Person speaker, boolean isExecute) {
        super(campaign,
              speaker,
              null,
              createInCharacterMessage(campaign, isExecute),
              createButtons(),
              createOutOfCharacterMessage(),
              null,
              false,
              null,
              null,
              true);
    }

    /**
     * Generates a list of buttons to display in the dialog.
     *
     * <p>
     * The dialog includes a single "Understood" button, allowing the player to acknowledge the given information.
     * </p>
     *
     * @return A list containing a single button with the appropriate label and tooltip.
     */
    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnUnderstood = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "btnUnderstood.button"), null);

        return List.of(btnUnderstood);
    }

    /**
     * Generates the immersive in-character message for the dialog.
     *
     * <p>The generated message reflects the player's decision to either release or execute
     * prisoners, incorporating contextual elements such as the player's in-game title and random narrative variation
     * for replay value.</p>
     *
     * @param campaign  The current campaign instance, used for context-specific data such as the commander's address.
     * @param isExecute {@code true} if the player chose to execute prisoners, {@code false} if the player chose to
     *                  release them.
     *
     * @return A formatted string containing the narrative in-character message for the dialog.
     */
    private static String createInCharacterMessage(Campaign campaign, boolean isExecute) {
        String executeKey = isExecute ? "execute" : "free";
        int eventRoll = randomInt(50);

        String resourceKey = executeKey + "Event" + eventRoll + ".message";

        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
    }

    /**
     * Generates an out-of-character (OOC) message for the dialog.
     *
     * <p>This optional message provides additional gameplay or narrative context.</p>
     *
     * @return A formatted string containing the out-of-character message.
     */
    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
