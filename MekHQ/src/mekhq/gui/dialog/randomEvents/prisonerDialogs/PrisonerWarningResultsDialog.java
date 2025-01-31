/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.randomEvents.prisonerDialogs;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static megamek.common.Compute.randomInt;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class PrisonerWarningResultsDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public PrisonerWarningResultsDialog(Campaign campaign, @Nullable Person speaker, boolean isExecute) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, isExecute),
            createButtons(), createOutOfCharacterMessage(), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnUnderstood = new ButtonLabelTooltipPair(
                getFormattedTextAt(RESOURCE_BUNDLE, "btnUnderstood.button"), null);

        return List.of(btnUnderstood);
    }

    private static String createInCharacterMessage(Campaign campaign, boolean isExecute) {
        String executeKey = isExecute ? "execute" : "free";
        int eventRoll = randomInt(50);

        String resourceKey = executeKey + "Event" + eventRoll + ".message";

        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
    }

    private static @Nullable String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
