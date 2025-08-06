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

import static mekhq.MHQConstants.NAG_UNMAINTAINED_UNITS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.gui.dialog.nagDialogs.nagLogic.UnmaintainedUnitsNagLogic.campaignHasUnmaintainedUnits;

import java.util.Collection;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about unmaintained units within the campaign.
 *
 * <p>The {@code UnmaintainedUnitsNagDialog} extends {@link ImmersiveDialogNag} and is specifically designed
 * to alert players when there are units in the campaign that have not received necessary maintenance. It uses
 * predefined constants, such as {@code NAG_UNMAINTAINED_UNITS}, to configure the dialog's behavior and content.</p>
 */
public class UnmaintainedUnitsNagDialog extends ImmersiveDialogNag {
    /**
     * Constructs a new {@code UnmaintainedUnitsNagDialog} to display a warning about unmaintained units.
     *
     * <p>This constructor initializes the dialog with preconfigured values, including the
     * {@code NAG_UNMAINTAINED_UNITS} constant for managing dialog suppression and the
     * {@code "UnmaintainedUnitsNagDialog"} localization key for retrieving dialog content. This dialog does not
     * associate a specific speaker.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data
     *                 required for constructing the nag dialog.
     */
    public UnmaintainedUnitsNagDialog(final Campaign campaign) {
        super(campaign, null, NAG_UNMAINTAINED_UNITS, "UnmaintainedUnitsNagDialog");
    }

    /**
     * Retrieves the appropriate speaker for a campaign dialog based on personnel specialization and rank.
     *
     * <p>This method evaluates the active personnel within the campaign to determine the most suitable speaker.
     * It prioritizes personnel with technical specialization, using rank and skills to select the optimal candidate. If
     * no technical specialist is available, the method falls back to senior administrators with the "HR" or "COMMAND"
     * specialization, ensuring a valid speaker is selected whenever possible.</p>
     *
     * <p>If the campaign instance is {@code null} or there are no active personnel available, a fallback mechanism is
     * employed to determine the speaker based on senior administrators.</p>
     *
     * @param campaign       The {@link Campaign} instance providing access to personnel and administrator data.
     * @param specialization The {@link AdministratorSpecialization} used as a criterion for selecting the speaker.
     *
     * @return The {@link Person} designated as the speaker, prioritizing technical specialists, then senior
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
            if (!person.isTech()) {
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
     * <p>This method attempts to retrieve a senior administrator with the "LOGISTICS" specialization first.
     * If no such administrator is available, it falls back to one with the "COMMAND" specialization.</p>
     *
     * @param campaign The {@link Campaign} instance providing access to administrator data.
     *
     * @return The {@link Person} designated as the fallback speaker. Returns {@code null} if no suitable administrator
     *       is available.
     */
    private @Nullable Person getFallbackSpeaker(Campaign campaign) {
        Person speaker = campaign.getSeniorAdminPerson(LOGISTICS);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

    /**
     * Determines whether a nag dialog should be displayed for unmaintained units in the campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The check for unmaintained units is enabled ({@code isCheckMaintenance} is {@code true}).</li>
     *     <li>The user has not ignored the nag dialog for unmaintained units in the options.</li>
     *     <li>The campaign's hangar contains unmaintained units that are not salvage.</li>
     * </ul>
     *
     * @param units              A {@link Collection} of {@link Unit} objects representing the campaign's hangar units.
     * @param isCheckMaintenance A flag indicating whether to check for unmaintained units.
     *
     * @return {@code true} if the nag dialog should be displayed due to unmaintained units, {@code false} otherwise.
     */
    public static boolean checkNag(Collection<Unit> units, boolean isCheckMaintenance) {
        final String NAG_KEY = NAG_UNMAINTAINED_UNITS;

        return isCheckMaintenance &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     campaignHasUnmaintainedUnits(units);
    }
}
