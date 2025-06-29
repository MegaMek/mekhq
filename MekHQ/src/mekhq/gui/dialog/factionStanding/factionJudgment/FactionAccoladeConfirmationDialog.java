/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.factionStanding.factionJudgment;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.factionStanding.FactionAccoladeLevel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Displays a confirmation dialog for faction accolades.
 *
 * <p>This dialog asks the user to confirm or cancel an accolade event affecting the campaign. It presents both
 * in-character and out-of-character explanatory text (where appropriate) and returns whether the user confirmed the
 * action.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionAccoladeConfirmationDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionAccoladeDialog";

    private static final String IN_CHARACTER_KEY = "FactionAccoladeDialog.confirmation.inCharacter";
    private static final String OUT_OF_CHARACTER_KEY = "FactionAccoladeDialog.confirmation.outOfCharacter";
    private static final String BUTTON_CONFIRM = "FactionAccoladeDialog.confirmation.button.confirm";
    private static final String BUTTON_CANCEL = "FactionAccoladeDialog.confirmation.button.cancel";

    private static final int CONFIRMED_DIALOG_INDEX = 1;

    private final Campaign campaign;

    private final boolean wasConfirmed;

    /**
     * Returns whether the user confirmed the accolade action.
     *
     * @return {@code true} if the user confirmed; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    /**
     * Constructs a new {@link FactionAccoladeConfirmationDialog}, showing the dialog to the user.
     *
     * <p>The dialog content is dynamically generated based on the specified campaign and its commander.</p>
     *
     * @param campaign      the campaign context in which accolade is being performed
     * @param accoladeLevel the recognition level of the accolade event
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionAccoladeConfirmationDialog(Campaign campaign, FactionAccoladeLevel accoladeLevel) {
        this.campaign = campaign;

        Person speaker = campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
              campaign,
              speaker,
              null,
              getInCharacterText(),
              getDialogOptions(),
              accoladeLevel.is(FactionAccoladeLevel.ADOPTION_OR_MEKS) ? getOutOfCharacterText() : null,
              null,
              false,
              true);

        wasConfirmed = dialog.getDialogChoice() == CONFIRMED_DIALOG_INDEX;
    }

    /**
     * Returns the list of dialog option labels presented to the user.
     *
     * @return a list of option labels (such as "Cancel" and "Confirm")
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> getDialogOptions() {
        return List.of(getTextAt(RESOURCE_BUNDLE, BUTTON_CANCEL),
              getTextAt(RESOURCE_BUNDLE, BUTTON_CONFIRM));
    }

    /**
     * Returns the in-character narrative text shown in the dialog.
     *
     * @return the formatted in-character dialog string
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getInCharacterText() {
        return getFormattedTextAt(RESOURCE_BUNDLE, IN_CHARACTER_KEY,
              campaign.getCommanderAddress(false));
    }

    /**
     * Returns the out-of-character explanatory text shown in the dialog.
     *
     * @return the formatted out-of-character dialog string with warning highlight
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getOutOfCharacterText() {
        return getFormattedTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY);
    }
}
