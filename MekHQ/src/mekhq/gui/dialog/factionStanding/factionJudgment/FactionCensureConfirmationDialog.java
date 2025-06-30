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
import mekhq.campaign.universe.factionStanding.FactionCensureAction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Displays a confirmation dialog for faction censure actions within a campaign.
 *
 * <p>This dialog asks the user to confirm or cancel a censure event affecting the campaign. It presents both
 * in-character and out-of-character explanatory text and returns whether the user confirmed the action.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionCensureConfirmationDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionCensureConfirmationDialog";

    private static final String IN_CHARACTER_KEY = "FactionCensureConfirmationDialog.inCharacter";
    private static final String OUT_OF_CHARACTER_KEY = "FactionCensureConfirmationDialog.outOfCharacter.";
    private static final String OUT_OF_CHARACTER_KEY_AFFIX_GOING_ROGUE = "affix.goRogue";
    private static final String OUT_OF_CHARACTER_KEY_AFFIX_WARNING = "affix.warning";
    private static final String OUT_OF_CHARACTER_KEY_AFFIX_SEPPUKU = "affix.seppuku";
    private static final int CONFIRMED_DIALOG_INDEX = 1;

    private final boolean wasConfirmed;

    /**
     * Returns whether the user confirmed the censure action.
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
     * Constructs a new {@link FactionCensureConfirmationDialog}, showing the dialog to the user.
     *
     * <p>The dialog content is dynamically generated based on the specified campaign and its commander.</p>
     *
     * @param campaign     the campaign context in which censure is being performed
     * @param censureAction the severity of the censure event
     * @param commander the target of the censure event
     * @param isSeppuku {@code true} if the character is considering Seppuku, otherwise {@code false}
     * @param isGoingRogue {@code true} if the campaign is considering going rogue, otherwise {@code false}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureConfirmationDialog(Campaign campaign, FactionCensureAction censureAction, Person commander,
            boolean isSeppuku, boolean isGoingRogue) {
        Person speaker = campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
              campaign,
              speaker,
              null,
              getInCharacterText(campaign),
              getDialogOptions(),
                getOutOfCharacterText(censureAction, isSeppuku, isGoingRogue, commander),
              null,
              false);

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
        return List.of(getTextAt(RESOURCE_BUNDLE, "FactionCensureConfirmationDialog.button.cancel"),
              getTextAt(RESOURCE_BUNDLE, "FactionCensureConfirmationDialog.button.confirm"));
    }

    /**
     * Returns the in-character narrative text shown in the dialog.
     *
     * @param campaign the campaign context used to personalize the dialog text
     *
     * @return the formatted in-character dialog string
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getInCharacterText(final Campaign campaign) {
        return getFormattedTextAt(RESOURCE_BUNDLE, IN_CHARACTER_KEY,
              campaign.getCommanderAddress(false));
    }

    /**
     * Returns the out-of-character explanatory text shown in the dialog.
     *
     * @param censureAction        The severity of the current ongoing censure
     * @param isSeppuku           Whether the character is considering seppuku
     * @param isGoingRogue        Whether the campaign is considering going rogue
     * @param mostSeniorCharacter the target of the censure (if applicable)
     *
     * @return the formatted out-of-character dialog string with warning highlight
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getOutOfCharacterText(FactionCensureAction censureAction, boolean isSeppuku, boolean isGoingRogue,
          Person mostSeniorCharacter) {
        String dialog;
        if (isGoingRogue) {
            dialog = getFormattedTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY + OUT_OF_CHARACTER_KEY_AFFIX_GOING_ROGUE,
                  mostSeniorCharacter.getHyperlinkedFullTitle());

        } else {
            dialog = getFormattedTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY + censureAction.getLookupName(),
                  mostSeniorCharacter.getHyperlinkedFullTitle());

            if (isSeppuku) {
                dialog += getTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY + OUT_OF_CHARACTER_KEY_AFFIX_SEPPUKU);
            }
        }

        dialog += getFormattedTextAt(RESOURCE_BUNDLE, OUT_OF_CHARACTER_KEY + OUT_OF_CHARACTER_KEY_AFFIX_WARNING,
              spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);

        return dialog;
    }
}
