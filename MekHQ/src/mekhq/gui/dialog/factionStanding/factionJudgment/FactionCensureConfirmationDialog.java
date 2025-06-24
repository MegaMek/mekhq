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
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class FactionCensureConfirmationDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureDialog";

    private static final String IN_CHARACTER_KEY = "FactionCensureConfirmationDialog.inCharacter";
    private static final String OUT_OF_CHARACTER_KEY = "FactionCensureConfirmationDialog.outOfCharacter";
    private static final int CONFIRMED_DIALOG_INDEX = 1;

    private final boolean wasConfirmed;

    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    public FactionCensureConfirmationDialog(Campaign campaign, Person commander) {
        Person speaker = campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
              campaign,
              speaker,
              null,
              getInCharacterText(campaign, commander),
              getDialogOptions(),
              getOutOfCharacterText(),
              null,
              false);

        wasConfirmed = dialog.getDialogChoice() == CONFIRMED_DIALOG_INDEX;
    }

    public List<String> getDialogOptions() {
        return List.of(getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.cancel"),
              getTextAt(RESOURCE_BUNDLE, "FactionCensureDialog.button.confirm"));
    }

    public String getInCharacterText(final Campaign campaign, Person commander) {
        return getFormattedTextAt(RESOURCE_BUNDLE, IN_CHARACTER_KEY,
              campaign.getCommanderAddress(false));
    }

    public String getOutOfCharacterText() {
        return getFormattedTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY,
              spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);
    }
}
