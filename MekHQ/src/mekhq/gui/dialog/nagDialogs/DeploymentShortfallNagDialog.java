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
import static mekhq.gui.dialog.nagDialogs.nagLogic.DeploymentShortfallNagLogic.hasDeploymentShortfall;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A nag dialog that alerts the user if short deployments are detected in the campaign's active contracts.
 *
 * <p>
 * This dialog checks whether any active AtB (Against the Bot) contracts have a deployment deficit and alerts the player
 * to address the issue. The check is performed weekly (on Sundays) and only when the campaign is currently located on a
 * planet. If deployment requirements are not met, the dialog is displayed to prompt the user to correct the situation.
 * </p>
 */
public class DeploymentShortfallNagDialog {
    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private final int CHOICE_CANCEL = 0;
    private final int CHOICE_CONTINUE = 1;
    private final int CHOICE_SUPPRESS = 2;

    private final Campaign campaign;
    private boolean cancelAdvanceDay;

    /**
     * Constructs the shortfall deployment nag dialog for a given campaign.
     *
     * <p>
     * This constructor initializes the dialog with the specified campaign and formats the resource message to display
     * information about deployment shortfalls.
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     */
    public DeploymentShortfallNagDialog(final Campaign campaign) {
        this.campaign = campaign;

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(COMMAND),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "DeploymentShortfallNagDialog.ic",
                    campaign.getCommanderAddress(false)),
              getButtonLabels(),
              getFormattedTextAt(RESOURCE_BUNDLE, "DeploymentShortfallNagDialog.ooc"),
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
     * Determines whether the advance day operation should be canceled.
     *
     * @return {@code true} if advancing the day should be canceled, {@code false} otherwise.
     */
    public boolean shouldCancelAdvanceDay() {
        return cancelAdvanceDay;
    }

    /**
     * Determines whether a nag dialog should be displayed for deployment shortfalls in the specified campaign.
     *
     * <p>This method evaluates several conditions to decide if the nag dialog should be shown:</p>
     * <ul>
     *     <li>The campaign uses AtB (Against the Bot) rules.</li>
     *     <li>The user has not ignored the nag dialog for deployment shortfalls in their options.</li>
     *     <li>The campaign has a deployment shortfall, as determined by {@code #hasDeploymentShortfall}.</li>
     * </ul>
     *
     * @param isUseAtB A flag indicating whether the campaign is using AtB rules.
     * @param campaign The {@link Campaign} object used to check for deployment shortfalls.
     *
     * @return {@code true} if the nag dialog should be displayed due to deployment shortfalls; {@code false} otherwise.
     */
    public static boolean checkNag(boolean isUseAtB, Campaign campaign) {
        final String NAG_KEY = MHQConstants.NAG_SHORT_DEPLOYMENT;

        return isUseAtB && !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_KEY) && hasDeploymentShortfall(campaign);
    }
}
