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

import static mekhq.MHQConstants.NAG_INSUFFICIENT_MEDICS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientMedicsNagLogic.hasMedicsNeeded;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about an insufficient number of medics in their campaign.
 *
 * <p>The {@code InsufficientMedicsNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized dialog
 * designed to alert players when there is a shortage of medics required for effective campaign operations. It uses
 * predefined values, including the {@code NAG_INSUFFICIENT_MEDICS} constant, and does not provide a specific speaker
 * specialization, relying instead on a default fallback mechanism.</p>
 */
public class InsufficientMedicsNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code InsufficientMedicsNagDialog} instance to display the insufficient medics nag dialog.
     *
     * <p>This constructor initializes the dialog with preconfigured parameters, such as the
     * {@code NAG_INSUFFICIENT_MEDICS} constant for managing dialog suppression and the
     * {@code "InsufficientMedicsNagDialog"} message key for retrieving localized dialog content. No specific speaker is
     * provided, triggering fallback logic to determine the appropriate speaker for the dialog.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for constructing the dialog.
     */
    public InsufficientMedicsNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_INSUFFICIENT_MEDICS, "InsufficientMedicsNagDialog");
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
          @Nullable AdministratorSpecialization specialization) {
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

    @Override
    protected String getInCharacterMessage(@Nullable Campaign campaign, String key, String commanderAddress) {
        final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

        int count = 0;

        if (campaign != null) {
            count = campaign.getMedicsNeed();
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ic", commanderAddress, count);
    }

    /**
     * Determines whether a nag dialog should be displayed for insufficient medics in the campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for insufficient medics in their options.</li>
     *     <li>The campaign requires additional medics ({@code medicsRequired} is greater than zero).</li>
     * </ul>
     *
     * @param medicsRequired The number of additional medics required to meet the campaign's needs.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient medics, {@code false} otherwise.
     */
    public static boolean checkNag(int medicsRequired) {
        final String NAG_KEY = NAG_INSUFFICIENT_MEDICS;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && hasMedicsNeeded(medicsRequired);
    }
}
