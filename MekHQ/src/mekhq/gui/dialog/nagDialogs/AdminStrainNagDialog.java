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
 */
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_ADMIN_STRAIN;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.gui.dialog.nagDialogs.nagLogic.AdminStrainNagLogic.hasAdminStrain;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Represents a nag dialog that warns the user about administrative strain in a campaign.
 *
 * <p>This dialog is triggered when the campaign has a positive administrative strain. The purpose
 * of the dialog is to notify the user about the issue, allowing them to take any corrective action as necessary.</p>
 */
public class AdminStrainNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private final Campaign campaign;
    private boolean cancelAdvanceDay;

    /**
     * Constructs the administrative strain nag dialog for the given campaign.
     *
     * <p>This dialog displays a detailed message describing the administrative strain
     * issue in the campaign.</p>
     *
     * @param campaign The {@link Campaign} for which the administrative strain nag dialog is to be displayed.
     */
    public AdminStrainNagDialog(final Campaign campaign) {
        this.campaign = campaign;

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, "AdminStrainNagDialog.ic", campaign.getCommanderAddress(false)),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "AdminStrainNagDialog.ooc"),
              true);

        int choiceIndex = dialog.getDialogChoice();

        switch (choiceIndex) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(NAG_ADMIN_STRAIN, true);
                cancelAdvanceDay = false;
            }
            default -> throw new IllegalStateException("Unexpected value in AdminStrainNagDialog: " + choiceIndex);
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
     * Retrieves the speaker based on the given administrator specialization.
     *
     * <p>This method first attempts to fetch the senior administrator person for the specified specialization. If no
     * person is found, it defaults to retrieving the senior administrator person with the "COMMAND"
     * specialization.</p>
     *
     * @return the {@link Person} assigned as the speaker, either the one matching the given specialization or the one
     *       with the "COMMAND" specialization if no specialized person is available.
     */
    private Person getSpeaker() {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
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
     * Determines if the administrative strain nag dialog should be displayed.
     *
     * <p>This method evaluates whether a warning about administrative strain should
     * be shown to the user based on the following conditions:</p>
     * <ul>
     *     <li>Turnover checks are enabled.</li>
     *     <li>Administrative strain checks are enabled.</li>
     *     <li>The nag dialog for administrative strain has not been ignored in the user options.</li>
     *     <li>The campaign's administrative strain level is above 0.</li>
     * </ul>
     *
     * @param isUseTurnover    {@code true} if turnover-based checks are enabled, {@code false} otherwise.
     * @param isUseAdminStrain {@code true} if administrative strain checks are enabled, {@code false} otherwise.
     * @param adminStrainLevel The current level of administrative strain in the campaign.
     *
     * @return {@code true} if the administrative strain nag dialog should be displayed; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseTurnover, boolean isUseAdminStrain, int adminStrainLevel) {
        final String NAG_KEY = NAG_ADMIN_STRAIN;

        return isUseTurnover &&
                     isUseAdminStrain &&
                     !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) &&
                     hasAdminStrain(adminStrainLevel);
    }
}
