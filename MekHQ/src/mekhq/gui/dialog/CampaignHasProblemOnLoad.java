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
package mekhq.gui.dialog;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.CampaignFactory.CampaignProblemType.CANT_LOAD_FROM_NEWER_VERSION;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory.CampaignProblemType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * A dialog class for managing and addressing campaign load problems in MekHQ.
 *
 * <p>This class is responsible for creating a user-interactive dialog whenever issues arise while loading a campaign.
 * It provides both informative messages (in-character and out-of-character) and actionable buttons tailored to the
 * specific type of problem detected.</p>
 *
 * @since 0.50.04
 */
public class CampaignHasProblemOnLoad {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignHasProblemOnLoad";

    private final int DIALOG_CANCEL_OPTION = 0;

    private final Campaign campaign;

    private boolean wasCanceled = true;

    /**
     * Constructs a dialog to handle problems encountered when loading a campaign.
     *
     * <p>This dialog provides messages and options to notify the user about the nature of the issue and allow
     * interaction based on the specific problem type.</p>
     *
     * <p>The dialog also determines whether the operation was canceled, based on the user's choice
     * and the problem type.</p>
     *
     * @param campaign    the {@link Campaign} associated with the load problem
     * @param problemType the {@link CampaignProblemType} describing the nature of the problem affecting the campaign
     *                    load
     */
    public CampaignHasProblemOnLoad(Campaign campaign, CampaignProblemType problemType) {
        this.campaign = campaign;

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              createInCharacterMessage(problemType),
              createButtons(problemType),
              createOutOfCharacterMessage(problemType),
              null,
              true);

        wasCanceled = (dialog.getDialogChoice() == DIALOG_CANCEL_OPTION);
    }

    /**
     * Indicates whether the operation or process was canceled by the user or system.
     *
     * @return {@code true} if the operation was explicitly interrupted or canceled. Otherwise, it returns
     *       {@code false}, meaning the operation was allowed to continue or complete as normal.
     */
    public boolean wasCanceled() {
        return wasCanceled;
    }

    /**
     * Generates a list of localized button labels for the dialog based on the specified problem type.
     *
     * <p>The buttons include options to either cancel or continue with loading the campaign, with the
     * available buttons determined by the nature of the problem:</p>
     *
     * <ul>
     *   <li><b>"Cancel":</b> Stops the campaign loading process in all scenarios.</li>
     *   <li><b>"Continue":</b> Allows the user to proceed with loading the campaign (if permitted by the problem type).</li>
     *   <li><b>"Continue with New Version":</b> Shown when handling issues with old campaign data usable in a newer version.</li>
     * </ul>
     *
     * <p>The button decisions are determined by the {@link CampaignProblemType} parameter:</p>
     * <ul>
     *   <li><b>{@code CANT_LOAD_FROM_NEWER_VERSION}</b>: Only the "Cancel" button is returned, as continuing is not allowed.</li>
     *   <li><b>{@code NEW_VERSION_WITH_OLD_DATA}</b>: Only the "Continue with New Version" button is returned.</li>
     *   <li><b>Other problem types:</b> Both "Cancel" and "Continue" buttons are included.</li>
     * </ul>
     *
     * @param problemType the {@link CampaignProblemType} specifying the nature of the issue, which determines the
     *                    buttons that should be displayed
     *
     * @return a {@link List} of {@link String} objects representing the localized labels of the dialog buttons
     */
    private List<String> createButtons(CampaignProblemType problemType) {
        String btnCancel = getFormattedTextAt(RESOURCE_BUNDLE, "cancel.button");
        String btnContinue = getFormattedTextAt(RESOURCE_BUNDLE, "continue.button");

        if (problemType == CANT_LOAD_FROM_NEWER_VERSION) {
            return List.of(btnCancel);
        } else {
            return List.of(btnCancel, btnContinue);
        }
    }

    /**
     * Retrieves the speaker for in-character dialog.
     *
     * <p>The speaker is determined as the senior administrator for the campaign
     * with the "Command" specialization. If no such administrator is found, {@code null} is returned.</p>
     *
     * @return a {@link Person} representing the senior administrator, or {@code null} if none exists
     */
    private @Nullable Person getSpeaker() {
        return campaign.getSeniorAdminPerson(COMMAND);
    }

    /**
     * Creates the in-character message dynamically based on the problem type.
     *
     * <p>This message is localized and assembled using resource bundles, with campaign-specific
     * information such as the commander's address.</p>
     *
     * @param problemType the {@link CampaignProblemType} specifying the nature of the load problem
     *
     * @return a localized {@link String} containing the in-character message
     */
    private String createInCharacterMessage(CampaignProblemType problemType) {
        String typeKey = problemType.toString();
        String commanderAddress = campaign.getCommanderAddress();

        return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".message", commanderAddress);
    }

    /**
     * Creates the out-of-character message dynamically based on the problem type.
     *
     * <p>This message is localized and is more technical or process-oriented,
     * explaining the detected issues in plain terms.</p>
     *
     * @param problemType the {@link CampaignProblemType} specifying the nature of the load problem
     *
     * @return a localized {@link String} containing the out-of-character message
     */
    private String createOutOfCharacterMessage(CampaignProblemType problemType) {
        String typeKey = problemType.toString();
        return getFormattedTextAt(RESOURCE_BUNDLE, typeKey + ".ooc");
    }
}
