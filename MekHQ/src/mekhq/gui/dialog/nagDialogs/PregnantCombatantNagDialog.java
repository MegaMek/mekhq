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

import static mekhq.MHQConstants.NAG_PREGNANT_COMBATANT;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.PregnantCombatantNagLogic.hasActivePregnantCombatant;

import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * A dialog class used to notify players about a pregnant combatant in their campaign.
 *
 * <p>The {@code PregnantCombatantNagDialog} extends {@link ImmersiveDialogNag} and provides a specialized dialog
 * designed to alert players when a pregnant combatant is detected within the campaign. It uses predefined values,
 * including the {@code HR} speaker and the {@code NAG_PREGNANT_COMBATANT} constant, to configure the dialog's behavior
 * and content.</p>
 */
public class PregnantCombatantNagDialog extends ImmersiveDialogNag {

    /**
     * Constructs a new {@code PregnantCombatantNagDialog} instance to display the pregnant combatant nag dialog.
     *
     * <p>This constructor initializes the dialog with preconfigured parameters, such as the
     * {@code NAG_PREGNANT_COMBATANT} constant for managing dialog suppression, the {@code "PregnantCombatantNagDialog"}
     * message key for localization, and the {@code HR} speaker for delivering the dialog message.</p>
     *
     * @param campaign The {@link Campaign} instance associated with this dialog. Provides access to campaign data and
     *                 settings required for constructing the dialog.
     */
    public PregnantCombatantNagDialog(final Campaign campaign) {
        super(campaign, HR, NAG_PREGNANT_COMBATANT, "PregnantCombatantNagDialog");
    }

    /**
     * Determines whether a nag dialog should be displayed for active pregnant combatants in the given campaign.
     *
     * <p>This method evaluates two main conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for active pregnant combatants in their options.</li>
     *     <li>There are active pregnant combatants in the campaign, as determined by
     *         {@code #hasActivePregnantCombatant}.</li>
     * </ul>
     *
     * @param hasActiveContract A flag indicating whether the campaign currently has an active contract.
     * @param activePersonnel   A list of {@link Person} objects representing the active personnel in the campaign.
     *
     * @return {@code true} if the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean hasActiveContract, List<Person> activePersonnel) {

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_PREGNANT_COMBATANT) &&
                     (hasActivePregnantCombatant(hasActiveContract, activePersonnel));
    }
}
