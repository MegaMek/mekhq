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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.randomEventSystem.RandomEventType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import org.jspecify.annotations.NonNull;

public class RandomEventDialog {
    private final static String RESPONSE_0_PREFIX = "response.0";
    private final static String RESPONSE_1_PREFIX = "response.1";
    private final static String RESPONSE_2_PREFIX = "response.2";
    private final static String BUTTON_SUFFIX = ".button";

    private final static String RESULT_OOC = "result.ooc";

    private final static String EVENT_PREFIX = "event.";
    private final static String MESSAGE_SUFFIX = ".message";

    private final int choiceIndex;

    public int getDialogChoice() {
        return choiceIndex;
    }

    public RandomEventDialog(Campaign campaign, Person speaker, RandomEventType event, String RESOURCE_BUNDLE) {
        String commanderAddress = getCommanderAddress(campaign);
        String inCharacterMessage = getInCharacterMessage(event, RESOURCE_BUNDLE, commanderAddress);
        List<String> options = getOptions(event, RESOURCE_BUNDLE);

        ImmersiveDialogSimple eventDialog = new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              options,
              getFormattedTextAt(RESOURCE_BUNDLE, RESULT_OOC),
              null,
              true);

        choiceIndex = eventDialog.getDialogChoice();
    }

    private static @NonNull List<String> getOptions(RandomEventType event, String RESOURCE_BUNDLE) {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, RESPONSE_0_PREFIX + event.name() + BUTTON_SUFFIX),
              getFormattedTextAt(RESOURCE_BUNDLE, RESPONSE_1_PREFIX + event.name() + BUTTON_SUFFIX),
              getFormattedTextAt(RESOURCE_BUNDLE, RESPONSE_2_PREFIX + event.name() + BUTTON_SUFFIX));
    }

    private static @NonNull String getInCharacterMessage(RandomEventType event, String RESOURCE_BUNDLE,
          String commanderAddress) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              EVENT_PREFIX + event.name() + MESSAGE_SUFFIX,
              commanderAddress);
    }

    private static String getCommanderAddress(Campaign campaign) {
        return campaign.getCommanderAddress();
    }
}
