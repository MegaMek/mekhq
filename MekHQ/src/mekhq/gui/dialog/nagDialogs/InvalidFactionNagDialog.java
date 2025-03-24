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
 */
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_INVALID_FACTION;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InvalidFactionNagLogic.isFactionInvalid;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog used to notify the user about an invalid faction in the current campaign.
 *
 * <p>
 * This nag dialog is triggered when the campaign's selected faction is determined to be invalid for the current
 * campaign date. It evaluates the validity of the faction based on the campaign date and displays a localized message
 * warning the user about the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Checks whether the campaign's faction is valid based on the current in-game date.</li>
 *   <li>Displays a warning dialog to alert the user when an invalid faction is detected.</li>
 * </ul>
 */
public class InvalidFactionNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private boolean cancelAdvanceDay;

    /**
     * Constructs an {@code InvalidFactionNagDialog} for the given campaign.
     *
     * <p>
     * This dialog initializes with the campaign information and sets a localized message to notify the user about the
     * potential issue involving an invalid faction. The message includes the commander's address for better clarity.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog. The campaign provides the faction and other
     *                 details for evaluation.
     */
    public InvalidFactionNagDialog(final Campaign campaign) {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "InvalidFactionNagDialog.ic", campaign.getCommanderAddress(false)),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "InvalidFactionNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_INVALID_FACTION, true);
                cancelAdvanceDay = false;
            }
            default -> throw new IllegalStateException("Unexpected value in InvalidFactionNagDialog: " + choiceIndex);
        }
    }

    /**
     * Retrieves a list of button labels from the resource bundle.
     *
     * <p>The method collects and returns button labels such as "Cancel", "Continue", and "Suppress" after
     * formatting them using the provided resource bundle.</p>
     *
     * @return a {@link List} of formatted button labels as {@link String}.
     */
    private List<String> getButtonLabels() {
        List<String> buttonLabels = new ArrayList<>();

        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.cancel"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.continue"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.suppress"));

        return buttonLabels;
    }

    /**
     * Determines whether the advance day operation should be canceled.
     *
     * @return {@code true} if advancing the day should be canceled, {@code false} otherwise.
     */
    public boolean shouldCancelAdvanceDay() {
        return cancelAdvanceDay;
    }

    /**
     * Determines whether a nag dialog should be displayed for an invalid faction in the campaign.
     *
     * <p>This method checks two conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for invalid factions in their options.</li>
     *     <li>The faction associated with the campaign is invalid for the specified date.</li>
     * </ul>
     *
     * @param campaignFaction The {@link Faction} associated with the campaign to be checked.
     * @param today           The {@link LocalDate} representing the current in-game date.
     *
     * @return {@code true} if the nag dialog should be displayed, {@code false} otherwise.
     */
    public static boolean checkNag(Faction campaignFaction, LocalDate today) {
        final String NAG_KEY = NAG_INVALID_FACTION;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && (isFactionInvalid(campaignFaction, today));
    }
}
