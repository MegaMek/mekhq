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

import static mekhq.MHQConstants.NAG_UNTREATED_PERSONNEL;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.calculateTotalDoctorCapacity;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UntreatedPersonnelNagLogic.campaignHasUntreatedInjuries;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

public class UntreatedPersonnelNagDialog extends ImmersiveDialogNag {
    public UntreatedPersonnelNagDialog(Campaign campaign) {
        super(campaign, null, NAG_UNTREATED_PERSONNEL, "UntreatedPersonnelNagDialog");
    }

    /**
     * Retrieves the appropriate speaker for a campaign dialog based on personnel specialization and rank.
     *
     * <p>This method evaluates the active personnel within the campaign to determine the most suitable speaker.
     * It prioritizes personnel with doctor roles, using rank and skills to select the optimal candidate. If no medical
     * specialist is available, the method falls back to senior administrators with the "HR" or "COMMAND"
     * specialization, ensuring a valid speaker is selected whenever possible.</p>
     *
     * <p>If the campaign instance is {@code null} or there are no active personnel available, a fallback mechanism is
     * employed to determine the speaker based on senior administrators.</p>
     *
     * @param campaign       The {@link Campaign} instance providing access to personnel and administrator data.
     * @param specialization The {@link AdministratorSpecialization} used as a criterion for selecting the speaker.
     *
     * @return The {@link Person} designated as the speaker, prioritizing medical specialists, then senior
     *       administrators with "HR" or "COMMAND" specializations. Returns {@code null} if no suitable speaker can be
     *       found.
     */
    @Override
    protected @Nullable Person getSpeaker(@Nullable Campaign campaign,
          @Nullable Campaign.AdministratorSpecialization specialization) {
        if (campaign == null) {
            return null;
        }

        List<Person> potentialSpeakers = campaign.getActivePersonnel(false);

        if (potentialSpeakers.isEmpty()) {
            return getFallbackSpeaker(campaign);
        }

        Person speaker = null;

        for (Person person : potentialSpeakers) {
            if (!person.isDoctor()) {
                continue;
            }

            if (speaker == null) {
                speaker = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaign, speaker)) {
                speaker = person;
            }
        }

        // First fallback
        if (speaker == null) {
            return getFallbackSpeaker(campaign);
        } else {
            return speaker;
        }
    }

    /**
     * Retrieves a fallback speaker based on senior administrators within the campaign.
     *
     * <p>This method attempts to retrieve a senior administrator with the "HR" specialization first.
     * If no such administrator is available, it falls back to one with the "COMMAND" specialization.</p>
     *
     * @param campaign The {@link Campaign} instance providing access to administrator data.
     *
     * @return The {@link Person} designated as the fallback speaker. Returns {@code null} if no suitable administrator
     *       is available.
     */
    private @Nullable Person getFallbackSpeaker(Campaign campaign) {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

    /**
     * Determines whether a nag dialog should be displayed for untreated injuries among campaign personnel.
     *
     * @param activePersonnel            A {@link List} of active personnel in the campaign. This includes individuals
     *                                   who may require treatment and doctors available to provide care.
     * @param baseBedCount               The base number of patients each doctor can handle, serving as the foundation
     *                                   for calculating total doctor capacity.
     * @param isDoctorsUseAdministration A flag determining whether the administrative skills of doctors should be
     *                                   factored into their capacity calculation.
     *
     * @return {@code true} if the nag dialog should be displayed due to untreated injuries, {@code false} otherwise.
     */
    public static boolean checkNag(List<Person> activePersonnel, int baseBedCount, boolean isDoctorsUseAdministration) {
        final String NAG_KEY = NAG_UNTREATED_PERSONNEL;

        int totalDoctorCapacity = calculateTotalDoctorCapacity(activePersonnel,
              isDoctorsUseAdministration,
              baseBedCount);

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     campaignHasUntreatedInjuries(activePersonnel, totalDoctorCapacity);
    }
}
