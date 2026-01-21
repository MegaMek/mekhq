/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_PRISONERS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.force.FormationType.SECURITY;
import static mekhq.gui.dialog.nagDialogs.nagLogic.PrisonersNagLogic.hasPrisoners;

import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

public class PrisonersNagDialog extends ImmersiveDialogNag {
    public PrisonersNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_PRISONERS, "PrisonersNagDialog");
    }

    /**
     * Determines the most suitable speaker for a campaign dialog based on personnel specialization and rank.
     *
     * <p>This method evaluates all active forces within the campaign to identify an appropriate speaker. It
     * prioritizes selecting force commanders belonging to "SECURITY" forces, using rank and skills to break ties
     * between candidates. If no qualified force commander is found, it falls back to a default speaker mechanism.</p>
     *
     * @param campaign       The {@link Campaign} instance providing access to force and personnel data.
     * @param specialization The {@link AdministratorSpecialization} used as an optional criterion for selecting the
     *                       speaker (maybe {@code null}).
     *
     * @return The {@link Person} designated as the speaker, favoring commanders from "SECURITY" forces, or a fallback
     *       speaker if no suitable individual is found. Returns {@code null} only if the fallback mechanism cannot
     *       resolve a speaker.
     */
    @Override
    protected @Nullable Person getSpeaker(Campaign campaign, @Nullable AdministratorSpecialization specialization) {
        List<Formation> formations = campaign.getAllForces();


        Person speaker = null;
        for (Formation formation : formations) {
            if (formation.isForceType(SECURITY)) {
                UUID commanderId = formation.getForceCommanderID();
                Person commander = campaign.getPerson(commanderId);
                if (commander == null) {
                    continue;
                }

                if (speaker == null) {
                    speaker = commander;
                    continue;
                }

                if (commander.outRanksUsingSkillTiebreaker(campaign, speaker)) {
                    speaker = commander;
                }
            }
        }

        if (speaker == null) {
            return getFallbackSpeaker(campaign);
        } else {
            return speaker;
        }
    }

    /**
     * Retrieves a fallback speaker based on senior administrators within the campaign.
     *
     * <p>This method attempts to retrieve a senior administrator with the "TRANSPORT" specialization first.
     * If no such administrator is available, it falls back to one with the "COMMAND" specialization.</p>
     *
     * @param campaign The {@link Campaign} instance providing access to administrator data.
     *
     * @return The {@link Person} designated as the fallback speaker. Returns {@code null} if no suitable administrator
     *       is available.
     */
    private @Nullable Person getFallbackSpeaker(Campaign campaign) {
        Person speaker = campaign.getSeniorAdminPerson(TRANSPORT);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

    /**
     * Determines whether a nag dialog should be displayed for prisoners in the given campaign.
     *
     * <p>This method evaluates two conditions to decide if the nag dialog for prisoners should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for prisoners in their options.</li>
     *     <li>The campaign has prisoners, as determined by {@code #hasPrisoners}.</li>
     * </ul>
     *
     * @param hasActiveContract A flag indicating whether the campaign has an active contract.
     * @param hasPrisoners      A flag indicating whether there are prisoners in the campaign.
     *
     * @return {@code true} if the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean hasActiveContract, boolean hasPrisoners) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_PRISONERS) &&
                     hasPrisoners(hasActiveContract, hasPrisoners);
    }
}
