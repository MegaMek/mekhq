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

import static mekhq.MHQConstants.NAG_INSUFFICIENT_ASTECH_TIME;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.getAsTechTimeDeficit;
import static mekhq.gui.dialog.nagDialogs.nagLogic.InsufficientAstechTimeNagLogic.hasAsTechTimeDeficit;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog used to notify the user about insufficient available time for astechs to complete the required maintenance
 * tasks. Not to be confused with {@link InsufficientAstechsNagDialog}.
 *
 * <p>
 * This nag dialog is triggered when the available work time for the astech pool is inadequate to meet the maintenance
 * time requirements for the current campaign's hangar units. It provides a localized message detailing the time deficit
 * and allows the user to take necessary action or dismiss the dialog.
 * </p>
 *
 * <strong>Features:</strong>
 * <ul>
 *   <li>Calculates the time deficit for the astech pool based on hangar unit maintenance requirements.</li>
 *   <li>Notifies the user when there is inadequate time available to maintain all units.</li>
 * </ul>
 */
public class InsufficientAstechTimeNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private final Campaign campaign;
    private boolean cancelAdvanceDay;

    /**
     * Constructs an {@code InsufficientAstechTimeNagDialog} for the given campaign.
     *
     * <p>
     * This dialog calculates the astech time deficit and uses a localized message to notify the user about the shortage
     * of available time. The message provides the commander's address, the time deficit, and a pluralized suffix for
     * correctness.
     * </p>
     *
     * @param campaign                   The {@link Campaign} tied to this nag dialog. The campaign provides data about
     *                                   hangar units and astech availability.
     * @param units                      A collection of {@link Unit} objects to evaluate for maintenance needs.
     * @param possibleAstechPoolMinutes  The total available AsTech work minutes without considering overtime.
     * @param isOvertimeAllowed          A flag indicating whether overtime is allowed, which adds to the available
     *                                   AsTech work time.
     * @param possibleAstechPoolOvertime The additional AsTech work minutes available if overtime is allowed.
     */
    public InsufficientAstechTimeNagDialog(final Campaign campaign, Collection<Unit> units, int possibleAstechPoolMinutes, boolean isOvertimeAllowed, int possibleAstechPoolOvertime) {
        this.campaign = campaign;

        int asTechsTimeDeficit = getAsTechTimeDeficit(units,
              possibleAstechPoolMinutes,
              isOvertimeAllowed,
              possibleAstechPoolOvertime);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "InsufficientAstechTimeNagDialog.ic",
                    campaign.getCommanderAddress(false),
                    asTechsTimeDeficit),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "InsufficientAstechTimeNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_INSUFFICIENT_ASTECH_TIME, true);
                cancelAdvanceDay = false;
            }
            default ->
                  throw new IllegalStateException("Unexpected value in InsufficientAstechTimeNagDialog" + choiceIndex);
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
     * Determines whether a nag dialog should be displayed due to insufficient AsTech time in the campaign.
     *
     * <p>This method evaluates the following conditions to determine whether the nag dialog needs to appear:</p>
     * <ul>
     *     <li>The user has not ignored the nag dialog for insufficient AsTech time in their options.</li>
     *     <li>There is a positive deficit in the available AsTech time for maintaining the campaign's units.</li>
     * </ul>
     *
     * @param units                      A collection of {@link Unit} objects to evaluate for maintenance needs.
     * @param possibleAstechPoolMinutes  The total available AsTech work minutes without considering overtime.
     * @param isOvertimeAllowed          A flag indicating whether overtime is allowed, which adds to the available
     *                                   AsTech work time.
     * @param possibleAstechPoolOvertime The additional AsTech work minutes available if overtime is allowed.
     *
     * @return {@code true} if the nag dialog should be displayed due to insufficient AsTech time, {@code false}
     *       otherwise.
     */
    public static boolean checkNag(Collection<Unit> units, int possibleAstechPoolMinutes, boolean isOvertimeAllowed, int possibleAstechPoolOvertime) {
        final String NAG_KEY = NAG_INSUFFICIENT_ASTECH_TIME;

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     hasAsTechTimeDeficit(units,
                           possibleAstechPoolMinutes,
                           isOvertimeAllowed,
                           possibleAstechPoolOvertime);
    }
}
