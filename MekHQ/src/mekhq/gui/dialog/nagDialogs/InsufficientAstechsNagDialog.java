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

import static mekhq.MHQConstants.NAG_ADMIN_STRAIN;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechsNagLogic.hasAsTechsNeeded;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog used to notify the user that their campaign has insufficient astechs. Not to be confused with
 * {@link InsufficientAstechTimeNagDialog}.
 *
 * <p>
 * This nag dialog is triggered when the campaign does not have enough astechs to handle the current maintenance and
 * repair workload. Users are notified via a localized message that provides relevant details about the issue.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Notifies users about the shortage of astechs in the current campaign.</li>
 *   <li>Allows users to address the issue or dismiss the dialog while optionally ignoring future warnings.</li>
 * </ul>
 */
public class InsufficientAstechsNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private final Campaign campaign;
    private boolean cancelAdvanceDay;

    /**
     * Constructs an {@code InsufficientAstechsNagDialog} for the given campaign.
     *
     * <p>
     * This dialog uses a localized message identified by the key {@code "InsufficientAstechsNagDialog.text"} to inform
     * the user of the insufficient astechs in their campaign.
     * </p>
     *
     * @param campaign The {@link Campaign} associated with this nag dialog.
     */
    public InsufficientAstechsNagDialog(final Campaign campaign) {
        this.campaign = campaign;

        int asTechsNeeded = campaign.getAstechNeed();

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "InsufficientAstechsNagDialog.ic",
                    campaign.getCommanderAddress(false),
                    asTechsNeeded),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "InsufficientAstechsNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_ADMIN_STRAIN, true);
                cancelAdvanceDay = false;
            }
            default ->
                  throw new IllegalStateException("Unexpected value in InsufficientAstechsNagDialog: " + choiceIndex);
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
     * Determines whether a nag dialog should be displayed for insufficient AsTechs in the campaign.
     *
     * <p>This method evaluates the following conditions to decide if the nag dialog should appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for insufficient AsTechs in their options.</li>
     *     <li>The campaign requires additional AsTechs ({@code asTechsNeeded} is greater than zero).</li>
     * </ul>
     *
     * @param asTechsNeeded The number of additional AsTechs required to meet the campaign's needs.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient AsTechs, {@code false} otherwise.
     */
    public static boolean checkNag(int asTechsNeeded) {
        final String NAG_KEY = MHQConstants.NAG_INSUFFICIENT_ASTECHS;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && hasAsTechsNeeded(asTechsNeeded);
    }
}
