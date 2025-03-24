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

import static mekhq.MHQConstants.NAG_CONTRACT_ENDED;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.gui.dialog.nagDialogs.nagLogic.EndContractNagLogic.isContractEnded;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.gui.baseComponents.AbstractMHQNagDialog;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog used to notify the user about the end date of a contract in the campaign.
 *
 * <p>
 * This nag dialog is triggered when a contract in the campaign is flagged as ending on the current date and the user
 * has not opted to ignore such notifications. It shows relevant details about the situation and allows the user to take
 * action or dismiss the dialog.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Handles notifications for contract end dates in the campaign.</li>
 *   <li>Uses a localized message with context-specific details from the campaign.</li>
 *   <li>Extends the {@link AbstractMHQNagDialog} to reuse base nag dialog functionality.</li>
 * </ul>
 */
public class EndContractNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private boolean cancelAdvanceDay;

    /**
     * Constructs an {@code EndContractNagDialog} for the given campaign.
     *
     * <p>
     * This dialog uses the localization key {@code "EndContractNagDialog.text"} to provide a message that includes
     * additional information, such as the commander's address. It is specifically tailored to show when a contract is
     * reaching its end date.
     * </p>
     *
     * @param campaign The {@link Campaign} that the nag dialog is tied to.
     */
    public EndContractNagDialog(final Campaign campaign) {
        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "EndContractNagDialog.ic", campaign.getCommanderAddress(false)),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "EndContractNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_CONTRACT_ENDED, true);
                cancelAdvanceDay = false;
            }
            default -> throw new IllegalStateException("Unexpected value in " +
                                                             getClass().getSimpleName() +
                                                             ": " +
                                                             choiceIndex);
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
     * Determines whether a nag dialog should be displayed for an ended contract in the given campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for ended contracts in their options.</li>
     *     <li>A contract in the campaign has ended, as determined by {@code #isContractEnded}.</li>
     * </ul>
     *
     * @param today           The current local date used to check against the contracts' ending dates.
     * @param activeContracts A list of {@link AtBContract} objects representing the campaign's active contracts.
     *
     * @return {@code true} if the nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(LocalDate today, List<AtBContract> activeContracts) {
        final String NAG_KEY = NAG_CONTRACT_ENDED;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && isContractEnded(today, activeContracts);
    }
}
