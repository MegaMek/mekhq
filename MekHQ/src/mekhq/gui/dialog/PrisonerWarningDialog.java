/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Presents the prisoner overflow warning to the player and the follow-up narrative dialog that resolves it.
 *
 * <p>The warning dialog offers an in-character message with three options: do nothing, release a number of prisoners,
 * or execute a number of prisoners. The chosen option index is exposed via {@link #getDialogChoice()} so the caller can
 * apply the corresponding campaign effects. Once those effects have been applied,
 * {@link #showResolutionDialog(boolean)} presents a randomized in-character reaction to the outcome.</p>
 *
 * <p>The number of prisoners to release or execute is supplied by the caller and inserted into the respective button
 * labels.</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PrisonerWarningDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    /** Number of randomized reaction messages available for each resolution type in the resource bundle. */
    private static final int REACTION_MESSAGE_COUNT = 50;

    private final Campaign campaign;
    private final Person speaker;
    private final int choiceIndex;

    /**
     * Displays the prisoner overflow warning dialog and records the player's choice.
     *
     * @param campaign   the current campaign instance
     * @param speaker    the {@link Person} presenting the warning, or {@code null} if no speaker is displayed
     * @param setFree    the number of prisoners to release if that option is selected
     * @param executions the number of prisoners to execute if that option is selected
     *
     * @author Illiani
     * @since 0.51.01
     */
    public PrisonerWarningDialog(Campaign campaign, @Nullable Person speaker, int setFree, int executions) {
        this.campaign = campaign;
        this.speaker = speaker;

        String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "warning.message", commanderAddress);

        List<String> options = List.of(getFormattedTextAt(RESOURCE_BUNDLE, "btnDoNothing.button"),
              getFormattedTextAt(RESOURCE_BUNDLE, "free.button", setFree),
              getFormattedTextAt(RESOURCE_BUNDLE, "execute.button", executions));

        ImmersiveDialogSimple warningDialog = new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              options,
              getFormattedTextAt(RESOURCE_BUNDLE, "warning.ooc"),
              null,
              true);

        this.choiceIndex = warningDialog.getDialogChoice();
    }

    /**
     * Returns the index of the option chosen by the player.
     *
     * @return the selected option index (e.g., 0 for do nothing, 1 for release, 2 for execute)
     */
    public int getDialogChoice() {
        return choiceIndex;
    }

    /**
     * Displays the follow-up dialog reacting to the player's chosen resolution.
     *
     * <p>A randomized in-character message is selected based on whether prisoners were executed or released.</p>
     *
     * @param wasExecution {@code true} if prisoners were executed, {@code false} if they were released
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void showResolutionDialog(boolean wasExecution) {
        String commanderAddress = campaign.getCommanderAddress();
        String prefix = wasExecution ? "executeEvent" : "freeEvent";
        String resourceKey = prefix + Compute.randomInt(REACTION_MESSAGE_COUNT) + ".message";

        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");

        new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              null,
              outOfCharacterMessage,
              null,
              false);
    }
}
