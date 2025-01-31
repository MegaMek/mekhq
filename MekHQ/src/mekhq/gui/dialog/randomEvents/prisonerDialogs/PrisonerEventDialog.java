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
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerEvent;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class PrisonerEventDialog extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    static final String FORWARD_EVENT = "event.";
    static final String SUFFIX_MESSAGE = ".message";

    static final String FORWARD_RESPONSE = "response.";
    static final String OPTION_INDEX_0 = "0.";
    static final String OPTION_INDEX_1 = "1.";
    static final String OPTION_INDEX_2 = "2.";
    static final String SUFFIX_BUTTON = ".button";
    static final String SUFFIX_TOOLTIP = ".tooltip";

    public PrisonerEventDialog(Campaign campaign, @Nullable Person speaker, PrisonerEvent event) {
        super(campaign, speaker, null, createInCharacterMessage(campaign, event),
            createButtons(event), createOutOfCharacterMessage(), null);
    }

    private static List<ButtonLabelTooltipPair> createButtons(PrisonerEvent event) {
        ButtonLabelTooltipPair btnResponseA = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_0 + event.name() + SUFFIX_BUTTON),
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_0 + event.name() + SUFFIX_TOOLTIP));
        ButtonLabelTooltipPair btnResponseB = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_1 + event.name() + SUFFIX_BUTTON),
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_1 + event.name() + SUFFIX_TOOLTIP));
        ButtonLabelTooltipPair btnResponseC = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_2 + event.name() + SUFFIX_BUTTON),
            getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_RESPONSE + OPTION_INDEX_2 + event.name() + SUFFIX_TOOLTIP));

        return List.of(btnResponseA, btnResponseB, btnResponseC);
    }

    private static String createInCharacterMessage(Campaign campaign, PrisonerEvent event) {
        String commanderAddress = campaign.getCommanderAddress(false);
        return getFormattedTextAt(RESOURCE_BUNDLE, FORWARD_EVENT + event.name() + SUFFIX_MESSAGE,
            commanderAddress);
    }

    private static String createOutOfCharacterMessage() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "result.ooc");
    }
}
