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
package mekhq.gui.dialog;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;

import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public class DefectionOffer extends MHQDialogImmersive {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    public DefectionOffer(Campaign campaign, Person defector, boolean isBondsman) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign,
                defector, isBondsman), createButtons(), createOutOfCharacterMessage(isBondsman),
            null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnDefectorUnderstood = new ButtonLabelTooltipPair(
            getFormattedTextAt(RESOURCE_BUNDLE, "understood.button"), null);

        return List.of(btnDefectorUnderstood);
    }

    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(HR);
    }

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

    private static String createOutOfCharacterMessage(boolean isBondsman) {
        String typeKey = isBondsman ? "bondsman" : "defector";
        return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".ooc");
    }
}
