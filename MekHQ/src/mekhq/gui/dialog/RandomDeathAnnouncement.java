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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class RandomDeathAnnouncement {
    private final static String RESOURCE_BUNDLE = "mekhq.resources.RandomDeathAnnouncement";

    public RandomDeathAnnouncement(Campaign campaign, Person victim, PersonnelStatus causeOfDeath) {
        new ImmersiveDialogSimple(campaign,
              victim,
              null,
              getInCharacterText(victim, causeOfDeath),
              List.of(getResponseText()),
              getOutOfCharacterText(),
              null,
              false);
    }

    private String getInCharacterText(Person victim, PersonnelStatus causeOfDeath) {
        String causeOfDeathLabel = causeOfDeath.getLogText();

        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.inCharacter",
              victim.getHyperlinkedFullTitle(),
              causeOfDeathLabel);
    }

    private String getResponseText() {
        return getTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.response");
    }

    private String getOutOfCharacterText() {
        return getTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.outOfCharacter");
    }
}
