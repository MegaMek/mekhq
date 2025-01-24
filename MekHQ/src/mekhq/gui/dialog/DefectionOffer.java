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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.MHQDialogImmersive;

import java.util.List;
import java.util.ResourceBundle;

import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;

/**
 * Dialog to inform and handle campaign-loading problems within MekHQ.
 *
 * <p>This dialog prompts the user with both in-character and out-of-character messages,
 * providing actionable options if any issues arise during campaign load, such as version
 * incompatibility or missing contracts.</p>
 *
 * <p>The dialog presents two options: "Cancel" to abort loading the campaign or
 * "Continue Regardless" to proceed despite the detected issues. It dynamically generates
 * text based on the problem type and campaign information.</p>
 */
public class DefectionOffer extends MHQDialogImmersive {
    private static final String BUNDLE_KEY = "mekhq.resources.DefectionOffer";
    private static final ResourceBundle resources = ResourceBundle.getBundle(
        BUNDLE_KEY, MekHQ.getMHQOptions().getLocale());

    public DefectionOffer(Campaign campaign, Person defector, boolean isBondsman) {
        super(campaign, getSpeaker(campaign), null, createInCharacterMessage(campaign,
                defector, isBondsman), createButtons(), createOutOfCharacterMessage(isBondsman),
            0, null, null, null);
    }

    private static List<ButtonLabelTooltipPair> createButtons() {
        ButtonLabelTooltipPair btnDefectorUnderstood = new ButtonLabelTooltipPair(
            resources.getString("understood.button"), null);

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
            return String.format(resources.getString(typeKey + ".message"),
                commanderAddress, defector.getFullName(), originFaction, defector.getFirstName());
        }

        return String.format(resources.getString(typeKey + ".message"),
            commanderAddress, defector.getFullName(),
            defector.getOriginFaction().getFullName(campaign.getGameYear()));
    }

    private static String createOutOfCharacterMessage(boolean isBondsman) {
        String typeKey = isBondsman ? "bondsman" : "defector";
        return resources.getString(typeKey + ".ooc");
    }
}
