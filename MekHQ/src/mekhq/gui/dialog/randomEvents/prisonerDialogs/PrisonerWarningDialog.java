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
public class PrisonerWarningDialog extends ImmersiveDialogCore {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /**
     * Creates a dialog to warn the player about low prisoner capacity and provide options to resolve it.
     *
     * @param campaign     The current campaign instance, which provides relevant context for the dialog.
     * @param speaker      The in-universe speaker addressing the player, or {@code null} if no speaker is present.
     * @param executeCount The number of prisoners that would be executed if the player chooses to execute prisoners.
     * @param freeCount    The number of prisoners that would be freed if the player chooses to free prisoners.
     */
    public PrisonerWarningDialog(Campaign campaign, @Nullable Person speaker, int executeCount, int freeCount) {
        super(campaign,
              speaker,
              null,
              createInCharacterMessage(campaign),
              createButtons(executeCount, freeCount),
              createOutOfCharacterMessage(),
              null,
              true,
              null,
              null,
              true);
    }

    /**
     * Generates the list of action buttons to display in the dialog.
     *
     * <p>This dialog offers three actions:</p>
     * <ul>
     *    <li><b>Do Nothing</b>: Ignore the low prisoner capacity warning and take no action.</li>
     *    <li><b>Free Prisoners</b>: Free a specified number of prisoners to reduce capacity usage.</li>
     *    <li><b>Execute Prisoners</b>: Execute a specified number of prisoners to free up space.</li>
     * </ul>
     * Each button includes a label and tooltip for clarity.
     *
     * @param executeCount The number of prisoners affected if choosing execution, or {@code null} if not applicable.
     * @param freeCount    The number of prisoners affected if choosing to free prisoners, or {@code null} if not
     *                     applicable.
     *
     * @return A list of action buttons with labels and tooltips.
     */
    private static List<ButtonLabelTooltipPair> createButtons(int executeCount, int freeCount) {
        ButtonLabelTooltipPair btnDoNothing = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "btnDoNothing.button"), null);
        ButtonLabelTooltipPair btnFree = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "free.button",
              freeCount), null);
        ButtonLabelTooltipPair btnExecute = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "execute.button",
              executeCount), null);

        return List.of(btnDoNothing, btnFree, btnExecute);
    }

    /**
     * Generates the in-character message to display in the dialog.
     *
     * <p>The message is tailored to the current situation, warning the player (commander)
     * about dangerously low prisoner capacity and urging immediate resolution. It uses the player's in-game title for
     * immersion.</p>
     *
     * @param campaign The current campaign context, from which player-specific information is derived.
     *
     * @return A formatted string containing the narrative in-character message for the dialog.
     */
    private static String createInCharacterMessage(Campaign campaign) {
        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, "warning.message", commanderAddress);
    }

    /**
     * Generates an out-of-character (OOC) message for the dialog.
     *
     * <p>The OOC message explains the mechanics of the situation and provides additional
     * guidance to help the player make an informed decision about how to handle the prisoner capacity issue.</p>
     *
     * @return A formatted string containing the out-of-character message.
     */
    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "warning.ooc");
    }
}
