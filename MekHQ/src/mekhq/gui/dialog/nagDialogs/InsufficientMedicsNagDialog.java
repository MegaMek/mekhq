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

import static mekhq.MHQConstants.NAG_INSUFFICIENT_MEDICS;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientMedicsNagLogic.hasMedicsNeeded;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog used to notify the user about insufficient medics required to meet the medical needs of the campaign.
 *
 * <p>
 * This nag dialog is triggered when the count of available medics in the campaign falls short of the total number
 * required for handling the current medical workload. It displays a localized message for the user with specifics about
 * the deficit, and optionally allows the user to dismiss or ignore future warnings.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Calculates the number of medics required for a campaign using {@link Campaign#getMedicsNeed()}.</li>
 *   <li>Displays a dialog to warn the user if the required number of medics exceeds the available count.</li>
 * </ul>
 */
public class InsufficientMedicsNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private final Campaign campaign;
    private boolean cancelAdvanceDay;

    /**
     * Constructs an {@code InsufficientMedicsNagDialog} for the given campaign.
     *
     * <p>
     * This dialog calculates the number of medics required and uses a localized message to notify the user about the
     * shortage. The message includes the commander's address, the medic deficit, and a pluralized suffix based on the
     * deficit count.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog. The campaign provides the medical
     *                 requirements for the calculation.
     */
    public InsufficientMedicsNagDialog(final Campaign campaign) {
        this.campaign = campaign;

        int medicsRequired = campaign.getMedicsNeed();

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "InsufficientMedicsNagDialog.ic",
                    campaign.getCommanderAddress(false),
                    medicsRequired),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "InsufficientMedicsNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_INSUFFICIENT_MEDICS, true);
                cancelAdvanceDay = false;
            }
            default ->
                  throw new IllegalStateException("Unexpected value in InsufficientMedicsNagDialog: " + choiceIndex);
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
     * Retrieves the speaker based on the active personnel.
     *
     * <p>This method iterates through the active personnel within the campaign and attempts to identify a speaker who
     * meets the criteria. It prioritizes selecting a person with technical specialization, using a tie-breaking
     * mechanism based on rank and skills. If no suitable speaker is found, it defaults to the senior administrator
     * person with the "COMMAND" specialization.</p>
     *
     * @return the {@link Person} designated as the speaker, either the highest-ranking technical specialist or the
     *       senior administrator with the "COMMAND" specialization if no other suitable speaker is found.
     */
    private Person getSpeaker() {
        List<Person> activePersonnel = campaign.getActivePersonnel(false);

        Person speaker = null;

        for (Person person : activePersonnel) {
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
            speaker = campaign.getSeniorAdminPerson(HR);
        } else {
            return speaker;
        }

        // Second fallback
        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
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
