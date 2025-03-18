/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.randomEvents;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.RoninOfferDialog;

import static megamek.common.Compute.randomInt;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.PersonUtility.reRollAdvantages;
import static mekhq.campaign.personnel.PersonUtility.reRollLoyalty;
import static mekhq.campaign.personnel.enums.PersonnelRole.AEROSPACE_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.generateBigPersonality;

/**
 * Represents a Ronin, a special type of recruitable personnel such as a MechWarrior,
 * who can be generated and added to the campaign's personnel roster.
 * Ronin are unique individuals with customized skill levels, traits, and personalities,
 * and their recruitment involves special dialogs and interactions.
 */
public class RoninOffer {

    /**
     * Constructs and generates a Ronin, adds them to the personnel roster, and handles
     * the custom recruitment process through dialogs.
     *
     * <p>The recruitment process involves two dialogs:</p>
     * <ol>
     *     <li>An initial offer dialog is displayed to the user.</li>
     *     <li>A subsequent follow-up dialog is presented if the initial offer is accepted.</li>
     * </ol>
     * If the user declines in any of the dialogs, the Ronin is not added to the campaign.
     *
     * @param campaign the current {@link Campaign} to which the Ronin is added if recruited.
     */
    public RoninOffer(Campaign campaign) {
        int roll = randomInt(5);

        PersonnelRole role = roll == 0 ? AEROSPACE_PILOT : MEKWARRIOR;
        Person ronin = campaign.newPerson(role);

        RandomSkillPreferences randomSkillPreferences = campaign.getRandomSkillPreferences();
        boolean useExtraRandomness = randomSkillPreferences.randomizeSkill();

        // We don't care about admin settings, as we're not going to have an admin here
        overrideSkills(false, false, useExtraRandomness,
              ronin, role, VETERAN);

        generateBigPersonality(ronin);

        SkillLevel skillLevel = ronin.getSkillLevel(campaign, false);
        reRollLoyalty(ronin, skillLevel);
        reRollAdvantages(campaign, ronin, skillLevel);

        ronin.setCallsign(RandomCallsignGenerator.getInstance().generate());

        RoninOfferDialog roninOfferDialogInitialMessage = new RoninOfferDialog(campaign, true, ronin);
        if (roninOfferDialogInitialMessage.getDialogChoice() != 0) {
            return;
        }

        RoninOfferDialog roninOfferDialogFollowUpMessage = new RoninOfferDialog(campaign, false, ronin);
        if (roninOfferDialogFollowUpMessage.getDialogChoice() != 0) {
            return;
        }

        campaign.recruitPerson(ronin, true);
    }
}
